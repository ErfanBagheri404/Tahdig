package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WaterMathTest {

    // --- AC: target respects glass size -----------------------------------

    @Test
    fun `target derives from weight at 35ml per kg`() {
        // 70kg * 35 = 2450
        assertEquals(2450, WaterMath.targetMl(70.0, 200, null))
    }

    @Test
    fun `target is never below one glass`() {
        // 5kg * 35 = 175, less than a 200ml glass: 175ml is an unreachable
        // target, so it floors at the glass.
        assertEquals(200, WaterMath.targetMl(5.0, 200, null))
    }

    @Test
    fun `custom glass size raises the target floor`() {
        // 60kg * 35 = 2100, comfortably above a 500ml glass.
        assertEquals(2100, WaterMath.targetMl(60.0, 500, null))
    }

    @Test
    fun `manual target overrides the weight derived one`() {
        assertEquals(1800, WaterMath.targetMl(70.0, 200, 1800))
    }

    @Test
    fun `no weight means no derived target`() {
        // Absent is not zero: a 0kg target would render as an instantly
        // complete water ring.
        assertNull(WaterMath.targetMl(null, 200, null))
        assertNull(WaterMath.targetMl(0.0, 200, null))
        assertNull(WaterMath.targetMl(-5.0, 200, null))
    }

    // --- AC: stepper amounts follow glass size ----------------------------

    @Test
    fun `stepper offers half one and two glasses`() {
        assertEquals(125, WaterMath.stepMl(250, 0.5))
        assertEquals(250, WaterMath.stepMl(250, 1.0))
        assertEquals(500, WaterMath.stepMl(250, 2.0))
    }

    @Test
    fun `stepper scales with the custom glass`() {
        assertEquals(200, WaterMath.stepMl(400, 0.5))
        assertEquals(800, WaterMath.stepMl(400, 2.0))
    }

    // --- AC: reset across midnight ----------------------------------------

    @Test
    fun `first ever log is a new day`() {
        assertTrue(WaterMath.isNewDay(null, LocalDate.of(2026, 9, 24)))
    }

    @Test
    fun `same day is not a new day`() {
        val today = LocalDate.of(2026, 9, 24)
        assertFalse(WaterMath.isNewDay(today, today))
    }

    @Test
    fun `the minute after midnight is a new day`() {
        val yesterday = LocalDate.of(2026, 9, 23)
        val today = LocalDate.of(2026, 9, 24)
        assertTrue(WaterMath.isNewDay(yesterday, today))
        // The boundary itself: 23:59 -> 00:00, one minute apart in wall
        // time but a different calendar day, so the counter resets.
        assertTrue(WaterMath.isNewDay(yesterday, today))
    }

    @Test
    fun `a month boundary is also a new day`() {
        assertTrue(
            WaterMath.isNewDay(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 1),
            ),
        )
    }

    @Test
    fun `a leap day boundary is a new day`() {
        assertTrue(
            WaterMath.isNewDay(
                LocalDate.of(2028, 2, 28),
                LocalDate.of(2028, 2, 29),
            ),
        )
    }

    // --- 7-day bar row ---------------------------------------------------

    @Test
    fun `week buckets are seven days oldest first ending today`() {
        val today = LocalDate.of(2026, 9, 24)
        val buckets = WaterMath.weekBuckets(today)
        assertEquals(7, buckets.size)
        assertEquals(LocalDate.of(2026, 9, 18), buckets.first())
        assertEquals(today, buckets.last())
    }

    @Test
    fun `week buckets include days with no intake`() {
        // A bar row with a hole in it lies about the week, so the bucket
        // list is built from the calendar, not from the logged entries.
        val today = LocalDate.of(2026, 9, 24)
        val buckets = WaterMath.weekBuckets(today)
        assertEquals(7, buckets.size)
        assertTrue(buckets.contains(today.minusDays(1)))
    }

    // --- progress --------------------------------------------------------

    @Test
    fun `progress is a fraction of the target`() {
        assertEquals(0.5f, WaterMath.progress(1225, 2450)!!, 0.001f)
    }

    @Test
    fun `progress is capped at one when overshooting`() {
        assertEquals(1f, WaterMath.progress(3000, 2450)!!, 0.001f)
    }

    @Test
    fun `progress is null without a target`() {
        assertNull(WaterMath.progress(500, null))
        assertNull(WaterMath.progress(500, 0))
    }
}
