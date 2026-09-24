package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CaffeineTest {

    private val today = LocalDate.of(2026, 9, 24)

    private fun e(day: Long, mg: Int) = Caffeine.Entry(today.plusDays(day), mg)

    // --- AC: 3 coffees -> 285mg accumulated, warning above cap ------------

    @Test
    fun `three coffees accumulate to 285 mg`() {
        // The AC's worked example: 95 x 3. Asserted as a literal, not by
        // multiplying with the same constant the code uses.
        val entries = listOf(e(0, 95), e(0, 95), e(0, 95))
        assertEquals(285, Caffeine.todayTotal(entries, today))
    }

    @Test
    fun `285 is under the adult cap so no warning yet`() {
        assertFalse(Caffeine.overCap(285, Caffeine.DEFAULT_CAP_MG))
    }

    @Test
    fun `a fourth coffee pushes past the adult cap`() {
        // 285 + 95 = 380 still under; a fifth 95 = 475 trips it.
        val five = listOf(e(0, 95), e(0, 95), e(0, 95), e(0, 95), e(0, 95))
        assertEquals(475, Caffeine.todayTotal(five, today))
        assertTrue(Caffeine.overCap(475, Caffeine.DEFAULT_CAP_MG))
    }

    @Test
    fun `exactly at the cap is not over`() {
        // 400 of 400 is allowed: over means past, same boundary rule as
        // NutrientCaps' at-cap WARN fix.
        assertFalse(Caffeine.overCap(400, 400))
        assertTrue(Caffeine.overCap(401, 400))
    }

    @Test
    fun `tea and nescafe presets match the issue`() {
        assertEquals(40, Caffeine.PRESETS.first { it.first == "چای" }.second)
        assertEquals(95, Caffeine.PRESETS.first { it.first == "قهوه" }.second)
        assertEquals(65, Caffeine.PRESETS.first { it.first == "نسکافه" }.second)
    }

    // --- AC: pregnancy mode lowers cap + shows safety warnings ------------

    @Test
    fun `pregnancy mode lowers the cap to 200`() {
        assertEquals(200, Caffeine.effectiveCap(null, pregnancyMode = true))
    }

    @Test
    fun `pregnancy mode overrides a custom cap`() {
        // Mode > default, and mode > custom: a 600 slider must not survive.
        assertEquals(200, Caffeine.effectiveCap(600, pregnancyMode = true))
        assertEquals(200, Caffeine.effectiveCap(100, pregnancyMode = true))
    }

    @Test
    fun `without pregnancy mode the custom cap wins over the default`() {
        assertEquals(400, Caffeine.effectiveCap(null, pregnancyMode = false))
        assertEquals(250, Caffeine.effectiveCap(250, pregnancyMode = false))
    }

    @Test
    fun `a nonsense custom cap is clamped to a real range`() {
        assertEquals(0, Caffeine.effectiveCap(-50, pregnancyMode = false))
        assertEquals(2000, Caffeine.effectiveCap(99_999, pregnancyMode = false))
    }

    @Test
    fun `285 mg trips the pregnancy cap but not the adult one`() {
        // The AC's example under both caps — the whole point of the mode.
        assertFalse(Caffeine.overCap(285, Caffeine.DEFAULT_CAP_MG))
        assertTrue(Caffeine.overCap(285, Caffeine.PREGNANCY_CAP_MG))
    }

    // --- accumulation scoping --------------------------------------------

    @Test
    fun `yesterday's coffee does not count toward today`() {
        val entries = listOf(e(0, 95), e(-1, 300))
        assertEquals(95, Caffeine.todayTotal(entries, today))
    }

    @Test
    fun `a day with nothing logged is zero`() {
        assertEquals(0, Caffeine.todayTotal(emptyList(), today))
    }

    @Test
    fun `the progress line renders Persian digits with the cap`() {
        assertEquals("۹۵ از ۴۰۰ میلی‌گرم", Caffeine.progressLine(95, 400, pregnancyMode = false))
    }

    @Test
    fun `the progress line names the mode when the cap was tightened`() {
        // A 200 line under a 600 slider would otherwise read as a bug.
        assertEquals(
            "۲۸۵ از ۲۰۰ میلی‌گرم · حالت بارداری",
            Caffeine.progressLine(285, 200, pregnancyMode = true),
        )
    }

    // --- food safety (conservative, same pattern as HalalFlags) -----------

    @Test
    fun `raw egg is flagged`() {
        val flags = Caffeine.safetyFlags("تخم‌مرغ خام، شکر")
        assertEquals(1, flags.size)
        assertEquals("تخم‌مرغ خام", flags.first().category)
    }

    @Test
    fun `cooked egg is never flagged`() {
        assertTrue(Caffeine.safetyFlags("تخم‌مرغ آب‌پز، نمک").isEmpty())
    }

    @Test
    fun `tuna is flagged as high mercury`() {
        val flags = Caffeine.safetyFlags("ماهی تن، سس مایونز")
        assertTrue(flags.any { it.category == "ماهی پرجیوه" })
    }

    @Test
    fun `plain fish never trips the mercury line`() {
        // Conservative: «ماهی» alone is most Persian fish and is not the
        // high-mercury case. Whole-word matching, not substring.
        assertTrue(Caffeine.safetyFlags("ماهی سفید، ادویه").isEmpty())
    }

    @Test
    fun `rare meat is flagged`() {
        val flags = Caffeine.safetyFlags("استیک ریر، کره")
        assertTrue(flags.any { it.category == "گوشت نیم‌پز" })
    }

    @Test
    fun `well done meat is not flagged`() {
        assertTrue(Caffeine.safetyFlags("گوشت چرخ‌کرده کاملاً پخته").isEmpty())
    }

    @Test
    fun `liver is flagged for vitamin A`() {
        val flags = Caffeine.safetyFlags("جگر گوسفند، پیاز")
        assertTrue(flags.any { it.category == "جگر" })
    }

    @Test
    fun `several risks accumulate into one warning line`() {
        val flags = Caffeine.safetyFlags("تخم‌مرغ خام، ماهی تن، جگر")
        assertEquals(3, flags.size)
        val text = Caffeine.warningText(flags)
        assertNotNull(text)
        assertTrue(text!!.startsWith("احتیاط در بارداری:"))
    }

    @Test
    fun `a clean dish has no warning text at all`() {
        assertNull(Caffeine.warningText(Caffeine.safetyFlags("آرد، آب، نمک")))
    }

    @Test
    fun `the warning never claims a dish is safe`() {
        // Same honesty rule as HalalFlags: it names risks, it certifies
        // nothing.
        val text = Caffeine.warningText(Caffeine.safetyFlags("تخم‌مرغ خام"))!!
        assertFalse(text.contains("بی‌خطر"))
        assertFalse(text.contains("سالم است"))
    }
}
