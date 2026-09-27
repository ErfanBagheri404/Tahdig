package com.erfanbagheri.tahdig.util

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Scheduling decisions for the smart notifier (#122).
 *
 * Pure and clock-injected: every method takes the "now" it must judge, so the
 * boundary cases (a fire landing exactly on the quiet-hour edge, a cook that
 * happened 23h ago) are unit tests, not emulator runs.
 *
 * The rule the issue cares about most: QUIET HOURS SUPPRESS REMINDERS AND
 * NEVER SUPPRESS A TIMER. A cooking timer is an intentional, user-started
 * thing — silencing it because the clock says 23:00 would break the feature
 * the user is actively relying on.
 */
object NotifyMath {

    /** What the evening check decided to do. */
    enum class Action { NOTHING, DAILY_SUGGESTION, STREAK_RISK, IDLE_24H, IDLE_7D }

    /** A notification kind, so quiet hours can treat timers differently. */
    enum class Kind { REMINDER, TIMER }

    /** Quiet window is half-open [from, until): a fire AT `from` is silenced. */
    fun inQuietHours(now: LocalTime, from: LocalTime, until: LocalTime): Boolean =
        if (from <= until) {
            now >= from && now < until
        } else {
            // Wraps midnight (22:00 → 07:00).
            now >= from || now < until
        }

    /**
     * True when this notification may be posted. The only rule: a reminder
     * inside quiet hours is dropped. A timer is always allowed.
     */
    fun mayPost(now: LocalTime, kind: Kind, quietFrom: LocalTime, quietUntil: LocalTime): Boolean =
        when (kind) {
            Kind.TIMER -> true
            Kind.REMINDER -> !inQuietHours(now, quietFrom, quietUntil)
        }

    /**
     * The evening decision.
     *
     * @param cookedToday the user has already cooked today.
     * @param streakDays live streak length; 0 means there is no streak to lose.
     * @param weeklyFloorMet the week already meets the user's cook floor.
     */
    fun eveningAction(
        cookedToday: Boolean,
        streakDays: Int,
        weeklyFloorMet: Boolean,
    ): Action {
        // A streak the user already lost has nothing at risk, and a day they
        // have cooked has nothing to warn about.
        if (streakDays <= 0 || cookedToday) return Action.NOTHING

        // AC: floor met -> silent. The weekly floor is a promise the user
        // already kept this week, so an evening nudge is pure noise.
        if (weeklyFloorMet) return Action.NOTHING

        // Streak risk outranks re-engagement: a live 20-day streak is worth
        // more to protect than a generic "come back" after a month.
        return Action.STREAK_RISK
    }

    /** Tier thresholds in hours — the two Duolingo-style copy variants. */
    const val IDLE_24H_HOURS = 24.0
    const val IDLE_7D_HOURS = 7 * 24.0

    /**
     * Re-engagement tier from the idle gap. Each tier fires ONCE per idle
     * period, deduped by a SINGLE prefs stamp: `firedTierHours` holds the
     * HIGHEST tier already fired this period (0 = none), and a cook resets it
     * to 0, re-arming both tiers for the next idle spell.
     *
     * Storing the threshold instead of a wall-clock timestamp is what makes
     * this one-shot: a plain "last fired at" stamp would be older than the
     * 7d threshold after a week of idleness and re-fire forever.
     *
     * @param firedTierHours stamp from prefs, 0 on first run.
     * @return the tier to fire, or null for nothing / already fired.
     */
    fun idleAction(hoursIdle: Double?, firedTierHours: Double): Action? {
        if (hoursIdle == null) return null
        if (hoursIdle >= IDLE_7D_HOURS && firedTierHours < IDLE_7D_HOURS) return Action.IDLE_7D
        if (hoursIdle >= IDLE_24H_HOURS && firedTierHours < IDLE_24H_HOURS) return Action.IDLE_24H
        return null
    }

    /** The stamp to persist after firing [action]'s tier. */
    fun stampAfter(action: Action): Double = when (action) {
        Action.IDLE_7D -> IDLE_7D_HOURS
        Action.IDLE_24H -> IDLE_24H_HOURS
        else -> 0.0
    }

    /**
     * The reminder hour actually used: the user's pick, or the evening
     * default. A setting of 0 is "never set", not midnight.
     */
    fun reminderHour(userHour: Int, defaultHour: Int = 20): Int =
        if (userHour in 1..23) userHour else defaultHour

    /** Epoch millis of the next fire at [hour]:[minute] local, strictly in the future. */
    fun nextTriggerAt(nowMillis: Long, hour: Int, zone: ZoneId, minute: Int = 30): Long {
        val local = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDateTime()
        var at = local.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (at.atZone(zone).toInstant().toEpochMilli() <= nowMillis) {
            at = at.plusDays(1)
        }
        return at.atZone(zone).toInstant().toEpochMilli()
    }
}
