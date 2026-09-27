package com.erfanbagheri.tahdig.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetch + parse a YouTube watch page into a [RecipeDraft] (#77).
 *
 * Two key-less calls: oembed for title/thumbnail, then the watch page for the
 * `shortDescription` embedded in the `ytInitialPlayerResponse` JSON blob. The
 * description is split by the shared [RecipeTextParser]; a leading `mm:ss`
 * chapter stamp stays in the step text («۲:۳۰ — تفت دادن») so step mode shows
 * it as a chip — [DurationParser] reads `۴۵ دقیقه`, never `2:30`, so a video
 * index can never turn into a countdown.
 *
 * ponytail: YouTube only. oembed answers 404 for every other host, which is
 * the same clean error a private video produces. Add an Instagram/TikTok
 * fetch when those URLs can actually be resolved key-lessly.
 */
object VideoDescriptionParser {

    /** Fetch + parse a video URL off the main thread. Throws [VideoImportError] on failure. */
    suspend fun fetchParse(url: String): RecipeDraft = withContext(Dispatchers.IO) {
        fetchParseBlocking(url)
    }

    /**
     * Pure core: watch-page HTML + its oembed JSON -> draft. No network, no
     * Android, so the whole split is unit-testable on a fixture.
     */
    fun parsePageHtml(html: String, oembed: String, sourceUrl: String? = null): RecipeDraft {
        val player = extractPlayerResponse(html)
            ?: throw VideoImportError(VideoError.MISSING_RESPONSE)
        // title / shortDescription / thumbnails all live under videoDetails.
        val details = runCatching { player["videoDetails"]!!.jsonObject }.getOrNull() ?: player
        val title = oembedTitle(oembed) ?: str(details, "title")
            ?: throw VideoImportError(VideoError.NO_TITLE)
        val description = str(details, "shortDescription")
            ?: throw VideoImportError(VideoError.NO_DESCRIPTION)
        val draft = RecipeTextParser.parse(stampChapterLines(description))
        return draft.copy(
            title = title.take(120),
            sourceUrl = sourceUrl,
            photoUrl = oembedThumbnail(oembed) ?: extractThumbnail(details),
        )
    }

    /** Pure parse of oembed JSON: {title, thumbnail_url, ...}. Blank fields -> null. */
    fun parseOembed(json: String): Pair<String?, String?> {
        val obj = parseJson(json) ?: return null to null
        val title = str(obj, "title")?.trim()?.ifBlank { null }
        val thumb = str(obj, "thumbnail_url")?.trim()?.ifBlank { null }
        return title to thumb
    }

    fun oembedTitle(json: String): String? = parseOembed(json)?.first
    fun oembedThumbnail(json: String): String? = parseOembed(json)?.second

    // ── Timestamps ───────────────────────────────────────────────────
    // «۰۰:۰۰ مقدمه» / «2:30 - تفت دادن» / «12:04» alone on a line.
    private val STAMP = Regex("""^\s*([0-9۰-۹]{1,3}:[0-9۰-۹]{2})\s*[-–—:،.]?\s*(.*)$""")
    private const val BULLET = "• "

    /**
     * Rewrite chapter stamps so the shared splitter keeps them with the step.
     *
     * `۲:۳۰` alone would be eaten as a numbered-list marker and split at the
     * colon («۳۰» + lost minutes), and a stamp glued to its step («۲:۳۰ — تفت
     * دادن») has no marker at all, so it would land in the ingredients column.
     * A bullet in front of the stamp fixes both: the splitter strips the
     * marker, keeps the stamp, and files the line under steps. The stamp stays
     * plain text — no timer field, nothing new in the model.
     */
    fun stampChapterLines(description: String): String {
        val lines = description.lines()
        val out = ArrayList<String>(lines.size)
        var i = 0
        while (i < lines.size) {
            val m = STAMP.find(lines[i])
            if (m == null) {
                out.add(lines[i]); i++
            } else {
                val stamp = m.groupValues[1]
                val rest = m.groupValues[2].trim()
                when {
                    rest.isNotEmpty() -> { out.add("$BULLET$stamp — $rest"); i++ }
                    i + 1 < lines.size -> {
                        // Bare chapter heading: the stamp belongs to the line under it.
                        out.add("$BULLET$stamp — ${lines[i + 1].trim()}"); i += 2
                    }
                    else -> { out.add("$BULLET$stamp"); i++ }
                }
            }
        }
        return out.joinToString("\n")
    }

    /**
     * Chapter stamp of a step, or null when the step states none.
     * The bullet [stampChapterLines] adds is skipped so a saved step reads back.
     */
    fun stampOf(step: String): String? =
        STAMP.find(step.removePrefix(BULLET).trimStart())?.groupValues?.get(1)

    // ── Watch page → player response ─────────────────────────────────
    // YouTube embeds the player config as `ytInitialPlayerResponse = {...};`
    // in an inline <script>. The slice is JS-escaped (\uXXXX, \n, \"), so
    // unescape before handing it to the JSON parser.
    private val PLAYER_RE = Regex("""ytInitialPlayerResponse\s*=\s*(\{.*?\});""", RegexOption.DOT_MATCHES_ALL)

    fun extractPlayerResponse(html: String): JsonObject? {
        val m = PLAYER_RE.find(html) ?: return null
        return parseJson(unescapeJs(m.groupValues[1]))
    }

    /** Last (largest) thumbnail URL from `thumbnail.thumbnails[]`. */
    fun extractThumbnail(player: JsonObject): String? {
        val list = runCatching {
            player["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray
        }.getOrNull() ?: return null
        return list.mapNotNull { el ->
            runCatching { el.jsonObject["url"]?.jsonPrimitive?.content?.trim() }.getOrNull()
        }.lastOrNull()
    }

    // ── HTTP ─────────────────────────────────────────────────────────
    private const val UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun fetchParseBlocking(url: String): RecipeDraft {
        val videoId = extractVideoId(url)
        val watchUrl = "https://www.youtube.com/watch?v=" + (videoId ?: url)

        // oembed first: 404/403 here is the cheapest proof the video is gone
        // or private — YouTube serves it without a key.
        val oembed = runCatching {
            httpGet("https://www.youtube.com/oembed?url=" +
                java.net.URLEncoder.encode(watchUrl, "UTF-8") + "&format=json")
        }.getOrElse { throw VideoImportError(VideoError.NETWORK) }
        if (oembed == null) throw VideoImportError(VideoError.PRIVATE_OR_REMOVED)
        val (oeTitle, oeThumb) = parseOembed(oembed) ?: (null to null)

        val html = runCatching { httpGet(watchUrl) }
            .getOrElse { throw VideoImportError(VideoError.NETWORK) }
        if (html == null || !html.contains("ytInitialPlayerResponse")) {
            throw VideoImportError(VideoError.PRIVATE_OR_REMOVED)
        }

        val player = extractPlayerResponse(html)
            ?: throw VideoImportError(VideoError.MISSING_RESPONSE)
        val title = oeTitle ?: str(player, "title")
            ?: throw VideoImportError(VideoError.NO_TITLE)
        val description = str(player, "shortDescription")
            ?: throw VideoImportError(VideoError.NO_DESCRIPTION)

        val draft = RecipeTextParser.parse(stampChapterLines(description))
        return draft.copy(
            title = title.take(120),
            sourceUrl = watchUrl,
            photoUrl = oeThumb ?: extractThumbnail(player),
        )
    }

    private fun httpGet(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Accept-Language", "fa-IR,fa;q=0.9,en;q=0.5")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) return null
            return conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } finally {
            conn.disconnect()
        }
    }

    private fun str(obj: JsonObject, key: String): String? =
        runCatching { obj[key]?.jsonPrimitive?.content?.trim()?.ifBlank { null } }.getOrNull()

    private fun parseJson(text: String): JsonObject? =
        runCatching { json.parseToJsonElement(text) }.getOrNull()
            ?.let { runCatching { it.jsonObject }.getOrNull() }

    /** Minimal JS/JSON unescape for the player-response slice. */
    private fun unescapeJs(raw: String): String {
        if (!raw.contains('\\')) return raw
        val sb = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c != '\\' || i + 1 >= raw.length) {
                sb.append(c); i++; continue
            }
            when (raw[i + 1]) {
                'u' -> {
                    val code = if (i + 5 < raw.length) raw.substring(i + 2, i + 6).toIntOrNull(16) else null
                    if (code != null) { sb.append(code.toChar()); i += 6 } else { sb.append(c); i++ }
                }
                '"' -> { sb.append('"'); i += 2 }
                '\\' -> { sb.append('\\'); i += 2 }
                '/' -> { sb.append('/'); i += 2 }
                'n' -> { sb.append('\n'); i += 2 }
                't' -> { sb.append('\t'); i += 2 }
                'r' -> { sb.append('\r'); i += 2 }
                else -> { sb.append(c); i++ }
            }
        }
        return sb.toString()
    }

    /** Bare video id from a watch / youtu.be / shorts / embed URL, or null. */
    fun extractVideoId(url: String): String? {
        listOf(
            Regex("""youtu\.be/([\w-]{6,})"""),
            Regex("""[?&]v=([\w-]{6,})"""),
            Regex("""/(?:shorts|embed|live)/([\w-]{6,})"""),
        ).forEach { p -> p.find(url)?.groupValues?.get(1)?.let { return it.take(11) } }
        return null
    }
}

/** Why an import failed, so the hub can show one clean Farsi line. */
enum class VideoError { NETWORK, PRIVATE_OR_REMOVED, MISSING_RESPONSE, NO_TITLE, NO_DESCRIPTION }

class VideoImportError(val kind: VideoError) : Exception(kind.name)

/** Farsi, one line per failure. Kept here (not strings.xml) like every other screen string. */
fun VideoError.farsiMessage(): String = when (this) {
    VideoError.NETWORK -> "نشانی در دسترس نیست؛ دوباره تلاش کن."
    VideoError.PRIVATE_OR_REMOVED, VideoError.MISSING_RESPONSE -> "این ویدیو خصوصی یا حذف شده است."
    VideoError.NO_TITLE -> "عنوان ویدیو پیدا نشد."
    VideoError.NO_DESCRIPTION -> "توضیح این ویدیو خالی است؛ متن دستور را مستقیم بچسبان."
}
