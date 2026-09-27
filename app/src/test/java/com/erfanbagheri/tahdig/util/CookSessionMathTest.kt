package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Force-stop restore math (#93 AC: «Unit tests for session serialize/restore math»
 * + «resume restores step + remaining time»).
 */
class CookSessionMathTest {

    // ── remaining ──────────────────────────────────────────────────

    @Test
    fun `paused session restores exactly its stored remaining`() {
        assertEquals(
            120_000L,
            CookSessionMath.restoreRemaining(
                storedRemainingMs = 120_000L, storedAtMs = 1_000L, nowMs = 999_999L, paused = true,
            ),
        )
    }

    @Test
    fun `running session subtracts the time the process was dead`() {
        // saved 30s remaining at t=0, force-stopped for 12s, resumed at t=12s
        assertEquals(
            18_000L,
            CookSessionMath.restoreRemaining(
                storedRemainingMs = 30_000L, storedAtMs = 0L, nowMs = 12_000L, paused = false,
            ),
        )
    }

    @Test
    fun `restore at the same instant is the stored value (serialize round-trip)`() {
        val stored = 245_000L
        assertEquals(
            stored,
            CookSessionMath.restoreRemaining(stored, 5_000L, 5_000L, paused = false),
        )
    }

    @Test
    fun `countdown that ran past zero clamps to zero, never negative`() {
        assertEquals(
            0L,
            CookSessionMath.restoreRemaining(
                storedRemainingMs = 10_000L, storedAtMs = 0L, nowMs = 60_000L, paused = false,
            ),
        )
        assertEquals(
            0L,
            CookSessionMath.restoreRemaining(-5L, 0L, 0L, paused = true),
        )
    }

    @Test
    fun `clock skew (now earlier than stored) is ignored, not negative elapsed`() {
        assertEquals(
            30_000L,
            CookSessionMath.restoreRemaining(30_000L, 100_000L, 50_000L, paused = false),
        )
    }

    // ── step index ─────────────────────────────────────────────────

    @Test
    fun `stored step inside range is restored as-is`() {
        assertEquals(4, CookSessionMath.restoreStep(4, 9))
        assertEquals(0, CookSessionMath.restoreStep(0, 9))
        assertEquals(8, CookSessionMath.restoreStep(8, 9))
    }

    @Test
    fun `stored step past a shrunken recipe clamps to the last step`() {
        assertEquals(2, CookSessionMath.restoreStep(7, 3))
    }

    @Test
    fun `negative or empty-recipe step lands on zero, never crashes`() {
        assertEquals(0, CookSessionMath.restoreStep(-1, 9))
        assertEquals(0, CookSessionMath.restoreStep(3, 0))
        assertEquals(0, CookSessionMath.restoreStep(3, -2))
    }

    // ── swipe direction (RTL correctness) ──────────────────────────

    @Test
    fun `rtl swipe right advances, swipe left goes back`() {
        assertEquals(1, CookSessionMath.stepForSwipe(+150f, rtl = true, current = 0, lastIndex = 8))
        assertEquals(0, CookSessionMath.stepForSwipe(-150f, rtl = true, current = 1, lastIndex = 8))
    }

    @Test
    fun `ltr swipe is mirrored - left advances`() {
        assertEquals(1, CookSessionMath.stepForSwipe(-150f, rtl = false, current = 0, lastIndex = 8))
        assertEquals(0, CookSessionMath.stepForSwipe(+150f, rtl = false, current = 1, lastIndex = 8))
    }

    @Test
    fun `drag below threshold is a touch, not a page turn`() {
        assertEquals(3, CookSessionMath.stepForSwipe(+40f, rtl = true, current = 3, lastIndex = 8))
        assertEquals(3, CookSessionMath.stepForSwipe(-40f, rtl = false, current = 3, lastIndex = 8))
    }

    @Test
    fun `swipe at recipe edges clamps instead of escaping`() {
        assertEquals(0, CookSessionMath.stepForSwipe(-150f, rtl = true, current = 0, lastIndex = 8))
        assertEquals(8, CookSessionMath.stepForSwipe(+150f, rtl = true, current = 8, lastIndex = 8))
        assertEquals(0, CookSessionMath.stepForSwipe(+150f, rtl = true, current = 0, lastIndex = 0))
    }
}
