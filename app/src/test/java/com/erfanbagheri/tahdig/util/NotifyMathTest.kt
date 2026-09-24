package com.erfanbagheri.tahdig.util

import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Scheduling decisions for the #122 smart notifier. */
class NotifyMathTest {

    private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)

    // Streak-at-risk evening decisions.

    @Test fun streakLive_noCookToday_notifies() {
        assertEquals(
            NotifyMath.Action.STREAK_RISK,
            NotifyMath.eveningAction(cookedToday = false, streakDays = 20, weeklyFloorMet = false),
        )
    }

    @Test fun cookedToday_silentEvenWithLiveStreak() {
        assertEquals(
            NotifyMath.Action.NOTHING,
            NotifyMath.eveningAction(cookedToday = true, streakDays = 20, weeklyFloorMet = false),
        )
    }

    @Test fun noLiveStreak_nothingAtRisk() {
        assertEquals(
            NotifyMath.Action.NOTHING,
            NotifyMath.eveningAction(cookedToday = false, streakDays = 0, weeklyFloorMet = false),
        )
    }

    @Test fun weeklyFloorMet_silent() {
        // AC: streak live + no cook today, but the week already meets the
        // user's cook floor -> silent, not streak-risk.
        assertEquals(
            NotifyMath.Action.NOTHING,
            NotifyMath.eveningAction(cookedToday = false, streakDays = 20, weeklyFloorMet = true),
        )
    }

    @Test fun floorMet_silent_evenOnFirstEverCook() {
        // The floor gate is checked before the streak branch, so a user whose
        // floor is already satisfied is silent regardless of streak length.
        assertEquals(
            NotifyMath.Action.NOTHING,
            NotifyMath.eveningAction(cookedToday = false, streakDays = 1, weeklyFloorMet = true),
        )
    }

    // Re-engagement tiers: 24h vs 7d distinct copy, one-shot each per period.

    @Test fun idle23h_nothing() {
        assertNull(NotifyMath.idleAction(hoursIdle = 23.0, firedTierHours = 0.0))
    }

    @Test fun idle24h_fires24() {
        assertEquals(
            NotifyMath.Action.IDLE_24H,
            NotifyMath.idleAction(hoursIdle = 24.0, firedTierHours = 0.0),
        )
    }

    @Test fun idle7d_fires7d_not24() {
        assertEquals(
            NotifyMath.Action.IDLE_7D,
            NotifyMath.idleAction(hoursIdle = 7 * 24.0, firedTierHours = 0.0),
        )
    }

    @Test fun idle24_firesOnce_deduped() {
        // Same period, same tier -> second call silent (the stamp).
        assertNull(
            NotifyMath.idleAction(hoursIdle = 30.0, firedTierHours = NotifyMath.stampAfter(NotifyMath.Action.IDLE_24H)),
        )
    }

    @Test fun idle7d_firesOnce_deduped() {
        assertNull(
            NotifyMath.idleAction(hoursIdle = 200.0, firedTierHours = NotifyMath.stampAfter(NotifyMath.Action.IDLE_7D)),
        )
    }

    @Test fun idle7d_after24fired_escalates() {
        // The 24h tier fired earlier this period; reaching 7d fires the
        // stronger tier exactly once.
        assertEquals(
            NotifyMath.Action.IDLE_7D,
            NotifyMath.idleAction(hoursIdle = 170.0, firedTierHours = NotifyMath.IDLE_24H_HOURS),
        )
    }

    @Test fun neverCooked_null_meansNothing() {
        assertNull(NotifyMath.idleAction(hoursIdle = null, firedTierHours = 0.0))
    }

    // Quiet hours: half-open window math, timers always pass.

    @Test fun quietFlat_insideSuppressed() {
        assertTrue(NotifyMath.inQuietHours(t(23, 30), t(22), t(7)))
    }

    @Test fun quietFlat_exactlyAtFrom_suppressed() {
        // Boundary: a fire AT `from` is inside (half-open [from, until)).
        assertTrue(NotifyMath.inQuietHours(t(22), t(22), t(7)))
    }

    @Test fun quietFlat_exactlyAtUntil_allowed() {
        assertFalse(NotifyMath.inQuietHours(t(7), t(22), t(7)))
    }

    @Test fun quietWrapsMidnight_afterMidnight_stillQuiet() {
        assertTrue(NotifyMath.inQuietHours(t(3), t(22), t(7)))
    }

    @Test fun quietWrapsMidnight_midday_allowed() {
        assertFalse(NotifyMath.inQuietHours(t(12), t(22), t(7)))
    }

    @Test fun quietFlat_noWrap() {
        assertTrue(NotifyMath.inQuietHours(t(14), t(13), t(15)))
        assertFalse(NotifyMath.inQuietHours(t(15), t(13), t(15)))
    }

    @Test fun reminderInQuiet_dropped() {
        assertFalse(NotifyMath.mayPost(t(23), NotifyMath.Kind.REMINDER, t(22), t(7)))
    }

    @Test fun reminderOutsideQuiet_allowed() {
        assertTrue(NotifyMath.mayPost(t(20, 30), NotifyMath.Kind.REMINDER, t(22), t(7)))
    }

    @Test fun timerInQuiet_alwaysAllowed() {
        // The core rule: quiet hours never suppress an active timer.
        assertTrue(NotifyMath.mayPost(t(23, 30), NotifyMath.Kind.TIMER, t(22), t(7)))
        assertTrue(NotifyMath.mayPost(t(3), NotifyMath.Kind.TIMER, t(22), t(7)))
    }

    // Reminder hour + trigger time.

    @Test fun reminderHour_userPickWins() {
        assertEquals(21, NotifyMath.reminderHour(21))
    }

    @Test fun reminderHour_zeroMeansUnset_usesDefault() {
        assertEquals(20, NotifyMath.reminderHour(0))
    }

    @Test fun nextTrigger_strictlyFuture_beforeHour() {
        val zone = ZoneId.of("Asia/Tehran")
        val now = java.time.ZonedDateTime.of(2026, 9, 24, 19, 0, 0, 0, zone)
            .toInstant().toEpochMilli()
        val at = java.time.Instant.ofEpochMilli(NotifyMath.nextTriggerAt(now, 20, zone))
            .atZone(zone).toLocalDateTime()
        assertEquals(24, at.dayOfMonth)
        assertEquals(20, at.hour)
        assertEquals(30, at.minute)
    }

    @Test fun nextTrigger_afterHour_rollsToTomorrow() {
        val zone = ZoneId.of("Asia/Tehran")
        val now = java.time.ZonedDateTime.of(2026, 9, 24, 21, 0, 0, 0, zone)
            .toInstant().toEpochMilli()
        val at = java.time.Instant.ofEpochMilli(NotifyMath.nextTriggerAt(now, 20, zone))
            .atZone(zone).toLocalDateTime()
        assertEquals(25, at.dayOfMonth)
    }
}
