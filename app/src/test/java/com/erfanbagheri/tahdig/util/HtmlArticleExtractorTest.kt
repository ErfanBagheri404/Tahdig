package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlArticleExtractorTest {

    @Test
    fun `extracts article text and drops ads comments nav footer`() {
        val html = """
            <html><head><title>Test</title><style>.x{}</style><script>var a=1;</script></head>
            <body>
            <nav>Home About</nav>
            <div class="ad">Buy now</div>
            <article>
            <h1>Recipe Title</h1>
            <p>First paragraph.</p>
            <p>Second paragraph.</p>
            </article>
            <div idcomments">Nice post!</div>
            <footer>Copyright 2026</footer>
            </body></html>
        """.trimIndent()

        val text = HtmlArticleExtractor.extract(html)

        assertTrue(text.contains("Recipe Title"))
        assertTrue(text.contains("First paragraph."))
        assertTrue(text.contains("Second paragraph."))
        assertFalse(text.contains("Buy now"))
        assertFalse(text.contains("Nice post!"))
        assertFalse(text.contains("Copyright"))
        assertFalse(text.contains("Home About"))
    }

    @Test
    fun `falls back to largest text block when no article or main`() {
        val html = """
            <html><body>
            <div class="sidebar">short</div>
            <div class="content">
            <p>Long paragraph one with enough text.</p>
            <p>Long paragraph two with enough text.</p>
            <p>Long paragraph three with enough text.</p>
            </div>
            </body></html>
        """.trimIndent()

        val text = HtmlArticleExtractor.extract(html)

        assertTrue(text.contains("Long paragraph one"))
        assertTrue(text.contains("Long paragraph two"))
        assertTrue(text.contains("Long paragraph three"))
        assertFalse(text.contains("short"))
    }

    @Test
    fun `strips script and style content`() {
        val html = """
            <html><head>
            <script>console.log("hidden");</script>
            <style>.hidden { display: none; }</style>
            </head><body><article><p>Visible text here.</p></article></body></html>
        """.trimIndent()

        val text = HtmlArticleExtractor.extract(html)

        assertTrue(text.contains("Visible text here."))
        assertFalse(text.contains("hidden"))
        assertFalse(text.contains("console.log"))
    }

    @Test
    fun `decodes common html entities`() {
        val html = "<html><body><article><p>Tom &amp; Jerry &lt;3 &#1607;&#1604;&#1575;</p></article></body></html>"

        val text = HtmlArticleExtractor.extract(html)

        assertTrue(text.contains("Tom & Jerry <3"))
        assertTrue(text.contains("هلا"))
    }

    @Test
    fun `empty or blank input returns empty string`() {
        assertEquals("", HtmlArticleExtractor.extract(""))
        assertEquals("", HtmlArticleExtractor.extract("   \n  "))
    }

    @Test
    fun `handles malformed html gracefully`() {
        val html = "<html><body><article><p>Unclosed paragraph<p>Another</article></body></html>"

        val text = HtmlArticleExtractor.extract(html)

        assertTrue(text.contains("Unclosed paragraph"))
        assertTrue(text.contains("Another"))
    }

    @Test
    fun `strips advertisement selectors`() {
        val html = """
            <html><body>
            <article><p>Real content here.</p></article>
            <div class="advertisement">Ad one</div>
            <div class="ads">Ad two</div>
            <div class="comment">Comment text</div>
            </body></html>
        """.trimIndent()

        val text = HtmlArticleExtractor.extract(html)

        assertTrue(text.contains("Real content here."))
        assertFalse(text.contains("Ad one"))
        assertFalse(text.contains("Ad two"))
        assertFalse(text.contains("Comment text"))
    }

    @Test
    fun `main tag is preferred over article when both exist`() {
        val html = """
            <html><body>
            <article><p>Article content.</p></article>
            <main><p>Main content is longer and should win.</p></main>
            </body></html>
        """.trimIndent()

        val text = HtmlArticleExtractor.extract(html)

        assertTrue(text.contains("Main content"))
    }

    @Test
    fun `whitespace is normalized`() {
        val html = "<html><body><article><p>Line one.</p>   <p>Line two.</p>\n\n<p>Line three.</p></article></body></html>"

        val text = HtmlArticleExtractor.extract(html)

        assertTrue(text.contains("Line one."))
        assertTrue(text.contains("Line two."))
        assertTrue(text.contains("Line three."))
        assertFalse(text.contains("   "))
    }
}
