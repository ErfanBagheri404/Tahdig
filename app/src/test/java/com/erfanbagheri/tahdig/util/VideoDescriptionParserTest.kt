package com.erfanbagheri.tahdig.util

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #77 fixtures: a watch-page HTML with the player response embedded the way
 * YouTube actually ships it (JS-escaped), plus timestamp-shape coverage.
 */
class VideoDescriptionParserTest {

    private val oembed = """
        {"title":"قورمه سبزی مثل مادرها","author_name":"آشپز خونه",
         "thumbnail_url":"https://i.ytimg.com/vi/abc123/hqdefault.jpg"}
    """.trimIndent()

    // Same shape as production: \n inside shortDescription, \uXXXX escapes.
    private val pageHtml = """
        <html><head><title>قورمه سبزی مثل مادرها - YouTube</title></head><body>
        <script>var ytInitialPlayerResponse = {"videoDetails":{"title":"قورمه سبزی مثل مادرها",
        "shortDescription":"مواد لازم\n2 عدد پیاز\n۱ پیمانه برنج\nطرز تهیه\n۰:۴۵ پیاز را تفت دهید\n1:30 اضافه کردن سبزی\n2 ساعت بپزید",
        "thumbnail":{"thumbnails":[{"url":"https://i.ytimg.com/vi/abc123/default.jpg"},
        {"url":"https://i.ytimg.com/vi/abc123/maxresdefault.jpg"}]}}};</script>
        </body></html>
    """.trimIndent()

    // ── Pure parse (fixture path — no network) ───────────────────────

    @Test
    fun `player response json extracts from page`() {
        val player = VideoDescriptionParser.extractPlayerResponse(pageHtml)!!
        assertEquals("قورمه سبزی مثل مادرها", player["videoDetails"]
            ?.let { runCatching { it.jsonObject["title"]?.jsonPrimitive?.content }.getOrNull() })
    }

    @Test
    fun `parsePageHtml fills title photo and both columns`() {
        val draft = VideoDescriptionParser.parsePageHtml(pageHtml, oembed, "https://youtu.be/abc123")
        assertEquals("قورمه سبزی مثل مادرها", draft.title)
        assertEquals("https://i.ytimg.com/vi/abc123/hqdefault.jpg", draft.photoUrl)
        assertEquals(listOf("2 عدد پیاز", "۱ پیمانه برنج"), draft.ingredients)
        assertEquals(3, draft.steps.size)
        assertEquals("https://youtu.be/abc123", draft.sourceUrl)
    }

    @Test
    fun `thumbnail falls back to player response when oembed has none`() {
        val noThumb = """{"title":"عنوان"}"""
        val draft = VideoDescriptionParser.parsePageHtml(pageHtml, noThumb)
        assertEquals("https://i.ytimg.com/vi/abc123/maxresdefault.jpg", draft.photoUrl)
    }

    @Test(expected = VideoImportError::class)
    fun `page without player response throws, not crashes`() {
        VideoDescriptionParser.parsePageHtml("<html><body>404 Not Found</body></html>", oembed)
    }

    @Test
    fun `missing player response reports PRIVATE_OR_REMOVED`() {
        val e = runCatching {
            VideoDescriptionParser.parsePageHtml("<html></html>", oembed)
        }.exceptionOrNull() as VideoImportError
        assertEquals(VideoError.MISSING_RESPONSE, e.kind)
    }

    @Test
    fun `description-less video reports NO_DESCRIPTION`() {
        val html = """<script>ytInitialPlayerResponse={"videoDetails":{"title":"x"}};</script>"""
        val e = runCatching {
            VideoDescriptionParser.parsePageHtml(html, oembed)
        }.exceptionOrNull() as VideoImportError
        assertEquals(VideoError.NO_DESCRIPTION, e.kind)
    }

    // ── Timestamps ride as chips, never timers ───────────────────────

    @Test
    fun `stamp line keeps its minutes and joins its label`() {
        val stamped = VideoDescriptionParser.stampChapterLines("۲:۳۰ — تفت دادن")
        assertEquals("• ۲:۳۰ — تفت دادن", stamped)
    }

    @Test
    fun `bare stamp borrows the line under it`() {
        val stamped = VideoDescriptionParser.stampChapterLines("2:30\nتفت دادن")
        assertEquals("• 2:30 — تفت دادن", stamped)
    }

    @Test
    fun `chapter stamps stay as steps with the stamp intact`() {
        val draft = RecipeTextParser.parse(
            VideoDescriptionParser.stampChapterLines(
                "طرز تهیه\n۰:۴۵ پیاز را تفت دهید\n1:30 اضافه کردن سبزی"
            )
        )
        assertTrue(draft.steps.isNotEmpty())
        assertTrue(draft.ingredients.isEmpty())
        assertEquals("۰:۴۵ — پیاز را تفت دهید", draft.steps.first())
    }

    @Test
    fun `stampOf reads the chip back out of a step`() {
        assertEquals("۲:۳۰", VideoDescriptionParser.stampOf("• ۲:۳۰ — تفت دادن"))
        assertNull(VideoDescriptionParser.stampOf("پیاز را خرد کنید"))
    }

    @Test
    fun `a chapter stamp never parses as a timer duration`() {
        // The chip must not become a countdown: DurationParser must stay blind
        // to mm:ss, and a step that ALSO states «۲ ساعت» keeps its real timer.
        assertNull(DurationParser.first("• ۲:۳۰ — تفت دادن"))
        assertEquals(7200L, DurationParser.first("• ۰:۴۵ — ۲ ساعت بپزید")!!.seconds)
    }

    // ── URL shapes ───────────────────────────────────────────────────

    @Test
    fun `video id from every youtube url shape`() {
        listOf(
            "https://www.youtube.com/watch?v=abc123XYZ_-&t=45",
            "https://youtu.be/abc123XYZ_-",
            "https://www.youtube.com/shorts/abc123XYZ_-",
            "https://www.youtube.com/embed/abc123XYZ_-",
        ).forEach { assertEquals("abc123XYZ_-", VideoDescriptionParser.extractVideoId(it)) }
        assertNull(VideoDescriptionParser.extractVideoId("https://example.com/foo"))
    }

    @Test
    fun `oembed parses title and thumbnail`() {
        val (title, thumb) = VideoDescriptionParser.parseOembed(oembed)
        assertEquals("قورمه سبزی مثل مادرها", title)
        assertEquals("https://i.ytimg.com/vi/abc123/hqdefault.jpg", thumb)
    }

    @Test
    fun `garbage oembed parses to nulls without throwing`() {
        assertEquals(null to null, VideoDescriptionParser.parseOembed("<html>nope"))
    }
}
