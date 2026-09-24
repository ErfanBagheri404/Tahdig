package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeightTrendTest {

    private fun day(n: Long) = LocalDate.of(2026, 9, 1).plusDays(n)
    private fun e(n: Long, kg: Double) = WeightTrend.Entry(day(n), kg)

    // --- AC: moving average over sparse data -----------------------------

    @Test
    fun `average over a single entry is that entry`() {
        val s = WeightTrend.series(listOf(e(0, 80.0)))
        assertEquals(80.0, s.single().average!!, 0.0001)
    }

    @Test
    fun `average averages every entry inside the window`() {
        val s = WeightTrend.series(listOf(e(0, 80.0), e(1, 82.0), e(2, 84.0)))
        // At day 2 the window holds all three: (80+82+84)/3
        assertEquals(82.0, s[2].average!!, 0.0001)
    }

    @Test
    fun `the window drops entries that fall out of seven days`() {
        val s = WeightTrend.series(
            (0L..8L).map { e(it, 80.0 + it) },
        )
        // Day 8's window is days 2..8 = seven entries 82..89
        assertEquals((2L..8L).map { 80.0 + it }.average(), s[8].average!!, 0.0001)
        // The very first entry is seven days out and must NOT be in day 8.
        assertFalse(s[8].average!!.let { it == 80.0 + (0L..8L).average() })
    }

    @Test
    fun `gaps do not break the average`() {
        // 10 days apart: the AC's exact concern. Day 10's window holds only
        // itself, so the average is the day's own weight, not a 7-entry
        // mean stretched across the gap and not a zero-filled one.
        val s = WeightTrend.series(listOf(e(0, 80.0), e(10, 78.0)))
        assertEquals(2, s.size)
        assertEquals(78.0, s[1].average!!, 0.0001)
    }

    @Test
    fun `an irregular gap inside the window still averages what exists`() {
        // Entries at days 0, 3, 5 — day 5's window holds all three.
        val s = WeightTrend.series(listOf(e(0, 70.0), e(3, 72.0), e(5, 71.0)))
        assertEquals((70.0 + 72.0 + 71.0) / 3, s[2].average!!, 0.0001)
    }

    @Test
    fun `entries are sorted oldest first whatever order they arrive in`() {
        val s = WeightTrend.series(listOf(e(5, 71.0), e(0, 70.0), e(3, 72.0)))
        assertEquals(day(0), s[0].date)
        assertEquals(day(3), s[1].date)
        assertEquals(day(5), s[2].date)
    }

    @Test
    fun `an empty log has no series`() {
        assertTrue(WeightTrend.series(emptyList()).isEmpty())
    }

    // --- delta / span / rate ---------------------------------------------

    @Test
    fun `delta is last minus first`() {
        assertEquals(-2.0, WeightTrend.deltaKg(listOf(e(0, 80.0), e(7, 78.0)))!!, 0.0001)
    }

    @Test
    fun `a single entry has no delta`() {
        assertNull(WeightTrend.deltaKg(listOf(e(0, 80.0))))
        assertNull(WeightTrend.deltaKg(emptyList()))
    }

    @Test
    fun `a weight gain is a positive delta`() {
        assertEquals(1.5, WeightTrend.deltaKg(listOf(e(0, 70.0), e(3, 71.5)))!!, 0.0001)
    }

    @Test
    fun `span counts calendar days not entries`() {
        // 10 entries but 9 days apart.
        val entries = (0L..9L).map { e(it, 80.0) }
        assertEquals(9L, WeightTrend.spanDays(entries))
    }

    @Test
    fun `rate per week extrapolates the daily change`() {
        // -2kg over 7 days = -2kg per week.
        assertEquals(-2.0, WeightTrend.ratePerWeek(listOf(e(0, 80.0), e(7, 78.0)))!!, 0.0001)
    }

    @Test
    fun `rate per week doubles a fourteen day change`() {
        // -2kg over 14 days = -1kg per week.
        assertEquals(-1.0, WeightTrend.ratePerWeek(listOf(e(0, 80.0), e(14, 78.0)))!!, 0.0001)
    }

    @Test
    fun `a span too short to extrapolate yields no rate`() {
        // Two days out is noise, not a trend.
        assertNull(WeightTrend.ratePerWeek(listOf(e(0, 80.0), e(1, 79.9))))
    }

    // --- BMR prompt threshold --------------------------------------------

    @Test
    fun `goal is stale once weight moves two kilos`() {
        assertTrue(WeightTrend.goalStale(70.0, 72.0))
    }

    @Test
    fun `daily fluctuation does not trigger the goal prompt`() {
        assertFalse(WeightTrend.goalStale(70.0, 70.4))
    }

    @Test
    fun `the threshold boundary counts as stale`() {
        assertTrue(WeightTrend.goalStale(70.0, 68.0))
    }

    @Test
    fun `no logged weight means no prompt`() {
        assertFalse(WeightTrend.goalStale(70.0, null))
        assertFalse(WeightTrend.goalStale(null, 75.0))
    }

    // --- Farsi delta line -------------------------------------------------

    @Test
    fun `loss line reads in Persian with a Persian decimal separator`() {
        assertEquals("۲٫۱ کیلو کمتر از اول", WeightTrend.deltaLine(-2.1))
    }

    @Test
    fun `gain line reads as a gain`() {
        assertEquals("۰٫۵ کیلو بیشتر از اول", WeightTrend.deltaLine(0.5))
    }

    @Test
    fun `no line when the weight is unchanged`() {
        assertNull(WeightTrend.deltaLine(0.0))
        assertNull(WeightTrend.deltaLine(0.04))
    }

    @Test
    fun `the line never contains an ASCII dot`() {
        val line = WeightTrend.deltaLine(-3.75)!!
        assertFalse(line.contains('.'))
        assertNotNull(line)
    }
}
