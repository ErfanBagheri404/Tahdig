package com.erfanbagheri.tahdig.util

import kotlin.math.abs

/**
 * Cook-session restore math (#93). Pure so force-stop restoration is unit-testable
 * without a DB or clock: every function takes `now` explicitly.
 *
 * The session row stores (remainingMs, paused, updatedAt) at the last *state
 * change* (step change / pause / resume) — never per tick. Restore then derives
 * the live countdown with wall-clock subtraction, which is exact as long as the
 * timer's running state did not change since that write (it can't: any such
 * change writes a new row).
 */
object CookSessionMath {

    /**
     * Remaining time at restore. Paused sessions keep their stored value; running
     * ones count the wall-clock time spent dead. Never negative.
     */
    fun restoreRemaining(
        storedRemainingMs: Long,
        storedAtMs: Long,
        nowMs: Long,
        paused: Boolean,
    ): Long {
        if (paused) return storedRemainingMs.coerceAtLeast(0L)
        val elapsed = (nowMs - storedAtMs).coerceAtLeast(0L)
        return (storedRemainingMs - elapsed).coerceAtLeast(0L)
    }

    /**
     * Clamp a stored step index onto the recipe's current step count — a reseeded
     * or edited recipe must not land the user past the end.
     */
    fun restoreStep(storedIndex: Int, stepCount: Int): Int {
        if (stepCount <= 0) return 0
        return storedIndex.coerceIn(0, stepCount - 1)
    }

    /** Drag distance (px) below which a swipe is a touch, not a page turn. */
    const val SWIPE_THRESHOLD_PX = 96f

    /**
     * Horizontal swipe → new step, RTL-correct. In RTL the next page lives to the
     * LEFT, so dragging content rightward (dx > 0) advances; LTR is mirrored.
     * Out-of-range results clamp instead of escaping the recipe.
     */
    fun stepForSwipe(dx: Float, rtl: Boolean, current: Int, lastIndex: Int): Int {
        if (abs(dx) < SWIPE_THRESHOLD_PX) return current
        val forward = if (rtl) dx > 0f else dx < 0f
        return if (forward) (current + 1).coerceAtMost(lastIndex)
        else (current - 1).coerceAtLeast(0)
    }
}
