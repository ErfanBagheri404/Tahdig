package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Heat + doneness extraction (#97). The acceptance fixtures are the issue's own
 * example («شعله ملایم ۴۵ دقیقه») plus the negation cases the table order exists for.
 */
class StepCuesTest {

    @Test
    fun `medium flame from the acceptance fixture`() {
        val text = "شعله ملایم ۴۵ دقیقه بپزد"
        assertEquals(HeatTagger.Level.MEDIUM, HeatTagger.levelOf(text))
        assertEquals("▮▮", HeatTagger.glyph(HeatTagger.Level.MEDIUM))
    }

    @Test
    fun `low medium and high are distinguished`() {
        assertEquals(HeatTagger.Level.LOW, HeatTagger.levelOf("شعله کم"))
        assertEquals(HeatTagger.Level.MEDIUM, HeatTagger.levelOf("حرارت متوسط"))
        assertEquals(HeatTagger.Level.HIGH, HeatTagger.levelOf("شعله زیاد کنید"))
    }

    @Test
    fun `negation does not report the opposite`() {
        // «زیاد نکنید» must read LOW — the whole reason the table is ordered.
        assertEquals(HeatTagger.Level.LOW, HeatTagger.levelOf("شعله را زیاد نکنید"))
        assertEquals(HeatTagger.Level.LOW, HeatTagger.levelOf("حرارت را تند نکنید"))
    }

    @Test
    fun `turning the heat off is its own level`() {
        assertEquals(HeatTagger.Level.OFF, HeatTagger.levelOf("شعله را خاموش کنید"))
        assertEquals("", HeatTagger.glyph(HeatTagger.Level.OFF))
    }

    @Test
    fun `a step with no heat word reports null`() {
        assertNull(HeatTagger.levelOf("مواد را مخلوط کنید"))
        assertNull(HeatTagger.levelOf(""))
    }

    @Test
    fun `doneness cues are extracted from a step`() {
        val cues = DonenessCues.of("صبر کنید تا ته‌دیگ طلایی و یکدست شود")
        assertTrue("طلایی" in cues)
        assertTrue("یکدست" in cues)
    }

    @Test
    fun `a step without cues yields none`() {
        assertEquals(emptyList<String>(), DonenessCues.of("پیاز را خرد کنید"))
    }

    @Test
    fun `cues are deduped`() {
        val cues = DonenessCues.of("طلایی شود، کاملاً طلایی")
        assertEquals(1, cues.count { it == "طلایی" })
    }

    @Test
    fun `arabic letter variants still match`() {
        // normalize folds ي→ی: a step typed with Arabic yeh must still find «طلایی».
        assertTrue(DonenessCues.of("تا طلايي شود").isNotEmpty())
    }
}
