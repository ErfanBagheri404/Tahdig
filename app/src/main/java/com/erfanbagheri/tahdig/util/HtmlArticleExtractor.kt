package com.erfanbagheri.tahdig.util

/**
 * Clean reader core (#78).
 *
 * Raw HTML from imported pages is hostile: ads, comments, nav, footer.
 * This strips the junk with common selectors, prefers `<article>`/`<main>`,
 * else falls back to the largest text block. Pure stdlib regex — no new
 * dependency for what a few patterns cover.
 *
 * ponytail: ceiling is heuristic tags; upgrade to jsoup/Readability when
 * fixtures show regex misfires.
 */
object HtmlArticleExtractor {

    private val SCRIPT = Regex("<script[^>]*>.*?</script>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val STYLE = Regex("<style[^>]*>.*?</style>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val COMMENT = Regex("<!--[\\s\\S]*?-->")
    private val NAV = Regex("<nav[^>]*>.*?</nav>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val FOOTER = Regex("<footer[^>]*>.*?</footer>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val HEADER_TAG = Regex("<header[^>]*>.*?</header>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val ASIDE = Regex("<aside[^>]*>.*?</aside>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

    /** .ad .ads #comments .comment [class*='advert'] and close kin, any container tag. */
    private val JUNK_CONTAINER = Regex(
        "<(div|section|aside|ul|ol|span)[^>]*(?:class|id)\\s*=\\s*[\"'][^\"']*(\\bad\\b|\\bads\\b|advert|comments?|respond|sidebar|popup|newsletter|share-?this|social)[^\"']*[\"'][^>]*>.*?</\\1>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val ARTICLE = Regex("<article[^>]*>(.*?)</article>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val MAIN = Regex("<main[^>]*>(.*?)</main>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val BODY = Regex("<body[^>]*>(.*?)</body>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val BLOCK_CONTAINER = Regex(
        "<(div|section)[^>]*>(.*?)</\\1>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val TAG = Regex("<[^>]+>")
    private val NUMERIC_ENTITY = Regex("&#(\\d+);")
    private val HEX_ENTITY = Regex("&#x([0-9a-fA-F]+);")

    fun extract(html: String): String {
        if (html.isBlank()) return ""
        var doc = html
        doc = COMMENT.replace(doc, " ")
        doc = SCRIPT.replace(doc, " ")
        doc = STYLE.replace(doc, " ")
        doc = NAV.replace(doc, " ")
        doc = FOOTER.replace(doc, " ")
        doc = HEADER_TAG.replace(doc, " ")
        doc = ASIDE.replace(doc, " ")
        // Repeat: nested junk containers need more than one pass.
        repeat(3) { doc = JUNK_CONTAINER.replace(doc, " ") }

        val article = ARTICLE.find(doc)?.groupValues?.get(1)?.trim().orEmpty()
        val main = MAIN.find(doc)?.groupValues?.get(1)?.trim().orEmpty()
        val picked = when {
            article.isNotBlank() && main.isNotBlank() ->
                if (clean(main).length >= clean(article).length) main else article
            article.isNotBlank() -> article
            main.isNotBlank() -> main
            else -> largestBlock(doc)
        }
        return clean(picked)
    }

    private fun largestBlock(doc: String): String {
        val scope = BODY.find(doc)?.groupValues?.get(1) ?: doc
        val candidates = BLOCK_CONTAINER.findAll(scope).map { it.groupValues[2] }.toList()
        if (candidates.isEmpty()) return scope
        return candidates.maxByOrNull { clean(it).length }.orEmpty()
    }

    private fun clean(fragment: String): String {
        var t = TAG.replace(fragment, "\n")
        t = decodeEntities(t)
        return t.lines()
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    private fun decodeEntities(s: String): String {
        var t = s
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
        t = NUMERIC_ENTITY.replace(t) {
            runCatching { it.groupValues[1].toInt().toChar().toString() }.getOrDefault(it.value)
        }
        t = HEX_ENTITY.replace(t) {
            runCatching { it.groupValues[1].toInt(16).toChar().toString() }.getOrDefault(it.value)
        }
        return t
    }
}
