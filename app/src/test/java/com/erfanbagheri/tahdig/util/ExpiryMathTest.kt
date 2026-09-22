package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #106 ACs: "Unit tests: days-to-expiry math, urgency multiplier, summary
 * string builder". Every clock is an argument — nothing reads the system time.
 */
class ExpiryMathTest {

    private val DAY = 86_400_000L
    private val NOW = 1_700_000_000_000L // fixed local-noon-ish instant

    /** Midnight of the local calendar day containing [ms]. */
    private fun localMidnight(ms: Long): Long {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private val today = localMidnight(NOW)

    // ── days-to-expiry math ────────────────────────────────────────

    @Test fun `undated rows have no days-to`() {
        assertNull(ExpiryMath.daysTo(null, NOW))
    }

    @Test fun `expiry today reads zero all day`() {
        assertEquals(0, ExpiryMath.daysTo(today, NOW))
        assertEquals(0, ExpiryMath.daysTo(today + DAY - 60_000L, NOW))
    }

    @Test fun `expiry in two days reads two`() {
        assertEquals(2, ExpiryMath.daysTo(today + 2 * DAY, NOW))
    }

    @Test fun `a date already past reads negative`() {
        assertEquals(-1, ExpiryMath.daysTo(today - DAY, NOW))
    }

    @Test fun `later in the same day does not cross into tomorrow`() {
        // NOW advanced to 23:59 local — tomorrow's expiry is still tomorrow.
        val late = today + DAY - 60_000L
        assertEquals(1, ExpiryMath.daysTo(today + DAY, late))
    }

    // ── urgency band ───────────────────────────────────────────────

    @Test fun `under three days is urgent, three is not`() {
        assertTrue(ExpiryMath.isUrgent(0))
        assertTrue(ExpiryMath.isUrgent(2))
        assertTrue(ExpiryMath.isUrgent(-1)) // past-due still needs attention
        assertFalse(ExpiryMath.isUrgent(3))
        assertFalse(ExpiryMath.isUrgent(null))
    }

    // ── urgency multiplier (AC: use-it-up ranking) ─────────────────

    @Test fun `boost climbs as the date gets closer`() {
        assertEquals(1.0, ExpiryMath.urgencyBoost(null), 0.0001)
        assertEquals(1.0, ExpiryMath.urgencyBoost(5), 0.0001)
        assertEquals(1.0, ExpiryMath.urgencyBoost(3), 0.0001)
        assertEquals(1.1, ExpiryMath.urgencyBoost(2), 0.0001)
        assertEquals(1.2, ExpiryMath.urgencyBoost(1), 0.0001)
        assertEquals(1.3, ExpiryMath.urgencyBoost(0), 0.0001)
    }

    @Test fun `spoiled staples are downranked, never boosted`() {
        assertTrue(ExpiryMath.urgencyBoost(-1) < 1.0)
    }

    @Test fun `the strongest expiring ingredient lifts the whole score`() {
        // AC: "multiplies coverage score by expiry urgency".
        assertEquals(1.0f, ExpiryMath.boostedScore(1.0f, emptyList()), 0.0001f)
        assertEquals(1.1f, ExpiryMath.boostedScore(1.0f, listOf(1.0, 1.1)), 0.0001f)
        // A spoiled item must not drag a fresh one down either — max, not min.
        assertEquals(1.0f, ExpiryMath.boostedScore(1.0f, listOf(1.0, 0.5)), 0.0001f)
        // A fresh item must not dilute an urgent one.
        assertEquals(0.66f, ExpiryMath.boostedScore(0.5f, listOf(1.0, 1.32)), 0.01f)
    }

    @Test fun `badge only when something expiring actually lifted`() {
        assertFalse(ExpiryMath.hasExpiryBadge(emptyList()))
        assertFalse(ExpiryMath.hasExpiryBadge(listOf(1.0, 1.0)))
        assertTrue(ExpiryMath.hasExpiryBadge(listOf(1.0, 1.1)))
    }

    // ── summary string builder ─────────────────────────────────────

    @Test fun `empty list builds no summary, the card hides itself`() {
        assertEquals("", ExpiryMath.summary(emptyList()))
        assertEquals("", ExpiryMath.summary(listOf("", "  ")))
    }

    @Test fun `summary counts in persian digits with three names`() {
        assertEquals(
            "۳ قلم تا ۳ روز آینده: شیر، ماست، نان",
            ExpiryMath.summary(listOf("شیر", "ماست", "نان")),
        )
    }

    @Test fun `more than three names truncates with a remainder`() {
        assertEquals(
            "۴ قلم تا ۳ روز آینده: شیر، ماست، نان و ۱ مورد دیگر",
            ExpiryMath.summary(listOf("شیر", "ماست", "نان", "پنیر")),
        )
    }

    // ── shelf-life defaults ────────────────────────────────────────

    @Test fun `known staples get a default expiry, unknown ones stay undated`() {
        assertEquals(7L, ExpiryMath.shelfDays("ماست"))
        assertNull(ExpiryMath.shelfDays("ارده"))
        assertNull(ExpiryMath.defaultExpiry("ارده", NOW))
    }

    @Test fun `shelf lookup tolerates zwnj and spacing variants`() {
        assertEquals(ExpiryMath.shelfDays("سیب‌زمینی"), ExpiryMath.shelfDays("سیب زمینی"))
        assertEquals(ExpiryMath.shelfDays("تخممرغ"), ExpiryMath.shelfDays("تخم مرغ"))
    }

    @Test fun `default expiry is added days after stocking`() {
        assertEquals(NOW + 7 * DAY, ExpiryMath.defaultExpiry("ماست", NOW))
    }
}
