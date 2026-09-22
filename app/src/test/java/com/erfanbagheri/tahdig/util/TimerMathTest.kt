package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #95 ACs: "Pause/resume preserves remaining ms exactly", "Unit tests for
 * remaining-time computation across pause/resume/clock-skew".
 *
 * Everything takes `now` explicitly — no system clock is read in the math, so
 * skew is just another argument.
 */
class TimerMathTest {

    private val T0 = 1_700_000_000_000L
    private val MIN = 60_000L

    // ── running ────────────────────────────────────────────────────

    @Test fun `remaining is the wall-clock distance to the end`() {
        assertEquals(5 * MIN, TimerMath.remaining(T0 + 5 * MIN, T0, 10 * MIN))
        assertEquals(0L, TimerMath.remaining(T0 + 5 * MIN, T0 + 5 * MIN, 10 * MIN))
    }

    @Test fun `remaining never goes negative past the end`() {
        assertEquals(0L, TimerMath.remaining(T0 + MIN, T0 + 99 * MIN, 10 * MIN))
    }

    @Test fun `a clock rolled back cannot inflate beyond the total`() {
        // Clock jumped back an hour: distance to endsAt exceeds anything set.
        assertEquals(10 * MIN, TimerMath.remaining(T0 + 10 * MIN, T0 - 3_600_000L, 10 * MIN))
    }

    @Test fun `a clock rolled forward simply reads zero`() {
        assertEquals(0L, TimerMath.remaining(T0 + 10 * MIN, T0 + 3_600_000L, 10 * MIN))
    }

    @Test fun `zero and negative totals cannot manufacture time`() {
        assertEquals(0L, TimerMath.remaining(T0, T0, 0L))
        assertEquals(0L, TimerMath.remaining(T0 + MIN, T0, -5L))
    }

    // ── pause / resume (AC: exact preservation) ────────────────────

    @Test fun `pause captures exactly what is left`() {
        val paused = TimerMath.pauseRemaining(T0 + 10 * MIN, T0 + 4 * MIN, 10 * MIN)
        assertEquals(6 * MIN, paused)
    }

    @Test fun `resume re-bases so the captured value is what counts down`() {
        val paused = 6 * MIN
        val resumeAt = T0 + 40 * MIN // pause lasted 36 minutes — irrelevant
        val end = TimerMath.resumeEnd(paused, resumeAt)
        assertEquals(paused, TimerMath.remaining(end, resumeAt, 10 * MIN))
        // …one second later, exactly one second is gone: to the millisecond.
        assertEquals(paused - 1000L, TimerMath.remaining(end, resumeAt + 1000L, 10 * MIN))
    }

    @Test fun `paused remaining survives an arbitrarily long pause untouched`() {
        val paused = TimerMath.pauseRemaining(T0 + 10 * MIN, T0 + 7 * MIN, 10 * MIN)
        // A month later the clock matters only through resume, never the store.
        val end = TimerMath.resumeEnd(paused, T0 + 30L * 24 * 60 * MIN)
        assertEquals(paused, TimerMath.remaining(end, T0 + 30L * 24 * 60 * MIN, 10 * MIN))
    }

    @Test fun `pause then immediate resume is a no-op on the countdown`() {
        val endsAt = TimerMath.resetEnd(10 * MIN, T0)
        // Paused 0ms into the run, resumed in the same millisecond.
        val p = TimerMath.pauseRemaining(endsAt, T0, 10 * MIN)
        val end = TimerMath.resumeEnd(p, T0)
        assertEquals(10 * MIN, TimerMath.remaining(end, T0, 10 * MIN))
    }

    @Test fun `reset restarts a full run`() {
        assertEquals(10 * MIN, TimerMath.remaining(TimerMath.resetEnd(10 * MIN, T0), T0, 10 * MIN))
    }

    // ── skew across pause/resume itself ────────────────────────────

    @Test fun `skewed clock at resume cannot exceed the paused value`() {
        // Clock rolled back between pause and resume: resumeEnd lands early,
        // remaining reads MORE than paused — clamped to the total, and the
        // paused value itself never exceeds the total either.
        val paused = 6 * MIN
        val end = TimerMath.resumeEnd(paused, T0 - 3_600_000L)
        val reported = TimerMath.remaining(end, T0 - 3_600_000L, 10 * MIN)
        assertEquals(paused, reported)
        assertTrue(reported <= 10 * MIN)
    }

    // ── TimerState round-trip (survives process death as JSON) ─────

    @Test fun `timer list survives a json round-trip unchanged`() {
        val list = listOf(
            TimerState(1, "ته‌دیگ", 10 * MIN, running = true, endsAtMs = T0 + MIN),
            TimerState(2, "ماست", 5 * MIN, running = false, pausedRemainingMs = 123456L, fired = false),
            TimerState(3, "پلو", 60 * MIN, running = false, fired = true),
        )
        assertEquals(list, TimerState.decode(TimerState.encode(list)))
    }

    @Test fun `a corrupt or missing prefs blob decodes to an empty list`() {
        assertTrue(TimerState.decode(null).isEmpty())
        assertTrue(TimerState.decode("").isEmpty())
        assertTrue(TimerState.decode("{not json").isEmpty())
    }

    @Test fun `display renders persian mmss`() {
        val t = TimerState(1, "x", 10 * MIN, running = true, endsAtMs = T0 + 5 * MIN + 12_000L)
        assertEquals("۰۵:۱۲", t.display(T0))
    }
}
