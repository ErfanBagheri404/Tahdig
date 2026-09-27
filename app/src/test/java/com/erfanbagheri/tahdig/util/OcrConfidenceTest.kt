package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #76 — the confidence threshold the review screen flags on.
 *
 * ML Kit itself is stubbed as plain [OcrConfidence.OcrLine]s (the object takes
 * no ML Kit type), so these run on the JVM with no device and no Robolectric.
 */
class OcrConfidenceTest {

    private fun line(text: String, vararg scores: Float?) =
        OcrConfidence.OcrLine(text, scores.toList())

    @Test
    fun `a line below the threshold is flagged`() {
        val flagged = OcrConfidence.lowConfidenceLines(
            listOf(line("۱ پیمانه برنج", 0.95f, 0.42f)),
        )
        assertEquals(listOf("۱ پیمانه برنج"), flagged)
    }

    @Test
    fun `a line at or above the threshold is not flagged`() {
        val flagged = OcrConfidence.lowConfidenceLines(
            listOf(
                line("۲ پیمانه شکر", 0.97f, 0.88f),
                line("نمک", 0.61f),
            ),
        )
        assertEquals(emptyList<String>(), flagged)
    }

    @Test
    fun `the exact threshold value itself is not flagged (at, not below)`() {
        // 0.6f is the named constant; a word scoring exactly it is trusted.
        val flagged = OcrConfidence.lowConfidenceLines(listOf(line("روغن", OcrConfidence.LOW_CONFIDENCE_THRESHOLD)))
        assertEquals(emptyList<String>(), flagged)
    }

    @Test
    fun `words with unknown confidence are skipped, known ones decide`() {
        // ML Kit omits confidence on some symbols; null must not poison the min.
        val flagged = OcrConfidence.lowConfidenceLines(
            listOf(line("زعفران", null, 0.55f, null)),
        )
        assertEquals(listOf("زعفران"), flagged)
    }

    @Test
    fun `a line with no confidence at all is trusted, not flagged`() {
        // Silence is not evidence of a bad scan — flagging it would spam every
        // clean scan with a warning row.
        val flagged = OcrConfidence.lowConfidenceLines(listOf(line("۳۰۰ گرم گوشت")))
        assertEquals(emptyList<String>(), flagged)
    }

    @Test
    fun `blank lines are dropped, not flagged`() {
        val flagged = OcrConfidence.lowConfidenceLines(
            listOf(line("   "), line("دستور", 0.1f)),
        )
        assertEquals(listOf("دستور"), flagged)
    }

    @Test
    fun `text is trimmed but otherwise untouched`() {
        // The flagged string must match what the review screen shows, so the
        // user can find and fix the exact line.
        assertEquals(listOf(" آب لیمو "), OcrConfidence.lowConfidenceLines(listOf(line(" آب لیمو ", 0.1f))))
    }

    @Test
    fun `only the failing lines of a mixed page are flagged`() {
        val page = listOf(
            line("مواد لازم", 0.99f),
            line("۲ عدد پیاز", 0.9f, 0.3f), // low
            line("۱ قاشق زردچوبه", 0.85f),
            line("نصف استکان آبغوره", 0.2f, 0.4f), // low
        )
        assertEquals(listOf("۲ عدد پیاز", "نصف استکان آبغوره"), OcrConfidence.lowConfidenceLines(page))
    }

    @Test
    fun `confidenceOf is the minimum known word score`() {
        assertEquals(0.42f, OcrConfidence.confidenceOf(line("x", 0.95f, 0.42f, null)))
        assertNull(OcrConfidence.confidenceOf(line("x")))
    }

    @Test
    fun `the threshold constant is the documented 0 point 6`() {
        assertEquals(0.6f, OcrConfidence.LOW_CONFIDENCE_THRESHOLD)
    }

    @Test
    fun `empty input yields no flags`() {
        assertTrue(OcrConfidence.lowConfidenceLines(emptyList()).isEmpty())
    }
}
