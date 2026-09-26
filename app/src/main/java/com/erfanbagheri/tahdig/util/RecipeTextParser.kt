package com.erfanbagheri.tahdig.util

/**
 * Plain-text recipe splitter (#75).
 *
 * Recipes arrive as a blob from the share sheet, a Telegram message, or a blog
 * copy — no structure, no schema. This is a best-effort split into the two
 * columns [RecipeDraft] has room for, because a review screen that shows the
 * raw blob is not review.
 *
 * Rules, in order (first hit wins):
 * 1. A bare http(s) URL is not a recipe — hand the blob back with the URL so
 *    the caller can run the web parser instead.
 * 2. «مواد لازم» / «طرز تهیه» (and close variants) split the blob: everything
 *    after the heading goes to the matching list.
 * 3. Lines starting with «N.» / «N)» (ASCII or Persian digits) — or a bullet —
 *    are steps. Remaining non-empty lines are ingredients.
 *
 * Nothing here throws: a messy blob just becomes a mostly-empty draft the user
 * edits before saving. #76–#78 extend by adding their own parser next to this,
 * not by editing it.
 */
object RecipeTextParser {

    private val URL = Regex("""\bhttps?://\S+""", RegexOption.IGNORE_CASE)
    private val HEADING = Regex(
        """^[\s\-*•\d\u06F0-\u06F9\u0660-\u0669]{0,8}(مواد\s*لازم|طرز\s*تهیه|طرز\s*پخت|دستور\s*پخت|پخت|دستور|ingredients?|steps?|description)\s*[:.\-]?\s*$""",
        RegexOption.IGNORE_CASE,
    )
    private val NUMBERED = Regex(
        """^\s*[\[(]?[0-9\u06F0-\u06F9\u0660-\u0669]{1,3}[)\].,،:]\s*(.+)$""",
    )
    private val BULLET = Regex(
        """^\s*[-–—*•·▪◦✓✔+]\s+(.+)$""",
    )

    /** A URL + nothing else is a link, not text: send it to the web parser. */
    fun looksLikeUrlOnly(text: String): Boolean {
        val t = text.trim()
        return URL.matchEntire(t) != null
    }

    /** Strip a leading «1.» / «-» / «•» marker; null when the line has none. */
    fun stripMarker(line: String): String? =
        NUMBERED.find(line)?.groupValues?.get(1)?.trim()?.ifEmpty { null }
            ?: BULLET.find(line)?.groupValues?.get(1)?.trim()?.ifEmpty { null }

    /**
     * URL branch (#78 will fill this in by fetching the page).
     *
     * Kept as its own entry point now so the router below has somewhere to send
     * a link and the review screen already shows a real draft: until #78 lands,
     * a shared link yields its URL as the source and the page title guess, with
     * the two lists empty for the user to fill in.
     */
    fun parseUrl(url: String): RecipeDraft {
        val slug = url.trimEnd('/').substringAfterLast('/').substringBefore('?')
        return RecipeDraft(
            title = slug.replace('-', ' ').replace('_', ' ').take(80),
            sourceUrl = url.trim(),
        )
    }

    /** One field, two parsers: a link goes to [parseUrl], anything else to [parse]. */
    fun parseAny(text: String): RecipeDraft =
        if (looksLikeUrlOnly(text)) parseUrl(URL.find(text)!!.value) else parse(text)

    fun parse(text: String): RecipeDraft {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return RecipeDraft()

        val url = URL.find(trimmed)?.value
        if (looksLikeUrlOnly(trimmed)) return RecipeDraft(sourceUrl = url)

        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val hasHeadings = lines.any { HEADING.containsMatchIn(it) }

        // Title = first line, but only when the blob has structure worth naming
        // and that first line is not itself a heading; an unstructured blob is all
        // ingredients, title stays blank.
        val firstIsHeading = lines.isNotEmpty() && HEADING.containsMatchIn(lines.first())
        val structured = hasHeadings || lines.any { stripMarker(it) != null }
        val title = if (structured && !firstIsHeading) lines.first() else ""

        if (hasHeadings) {
            val ingredients = mutableListOf<String>()
            val steps = mutableListOf<String>()
            var current: MutableList<String>? = null
            for (line in lines) {
                val heading = HEADING.find(line)?.groupValues?.get(1)?.lowercase() ?: ""
                when {
                    heading.startsWith("مواد") || heading.startsWith("ingredient") -> {
                        current = ingredients
                    }
                    heading.startsWith("طرز") || heading.startsWith("پخت") ||
                        heading.startsWith("دستور") || heading.startsWith("step") ||
                        heading.startsWith("description") -> {
                        current = steps
                    }
                    line == title || line == url -> Unit // name / link, not a material
                    current != null -> current.add(stripMarker(line) ?: line)
                    else -> Unit // pre-heading filler that is not the title
                }
            }
            return RecipeDraft(
                title = title,
                ingredients = ingredients,
                steps = steps,
                sourceUrl = url,
            )
        }

        // No headings: marked lines are steps, the rest are ingredients.
        if (structured) {
            val ingredients = mutableListOf<String>()
            val steps = mutableListOf<String>()
            for (line in lines) {
                if (line == title) continue
                val stripped = stripMarker(line)
                if (stripped != null) steps.add(stripped) else ingredients.add(line)
            }
            return RecipeDraft(
                title = title,
                ingredients = ingredients,
                steps = steps,
                sourceUrl = url,
            )
        }

        return RecipeDraft(ingredients = lines, sourceUrl = url)
    }
}
