package com.erfanbagheri.tahdig.util

import java.time.LocalDate

/**
 * Cook-streak math (#120). Pure — no DB, no clock injected beyond [today], so
 * day-boundary and freeze logic are unit-testable without Robolectric.
 *
 * Design:
 * - A streak counts consecutive *local days* with at least one cook.
 * - One missed day can be absorbed by a freeze token instead of resetting.
 * - A weekly floor goal (1..7 cooks/week) preserves the streak even when a gap
 *   day happened inside a week where the floor was still met.
 *
 * ponytail: weeks start Saturday (Iranian calendar week). A user-configurable
 * week start belongs in Settings once someone asks for it.
 */
object StreakMath {

    /** One month's worth of freeze tokens, granted on the 1st, never banked below 0. */
    const val FREEZE_PER_MONTH = 1
    const val MAX_FREEZE_BANK = 3

    data class State(
        /** Consecutive days ending at [today] (or at the last cooked day if frozen). */
        val current: Int,
        /** Longest run ever observed. */
        val longest: Int,
        val freezesLeft: Int,
        /** Cooks in the week containing [today], against the floor. */
        val thisWeek: Int,
        val weeklyFloor: Int,
        /** True when a day was missed and a freeze is holding the streak. */
        val frozenDay: LocalDate? = null,
    )

    /** Saturday-indexed week key; stable across the whole week. */
    fun weekStart(day: LocalDate): LocalDate {
        // LocalDate: Monday=1 ... Sunday=7. Saturday = 6.
        val back = (day.dayOfWeek.value + 1) % 7 // Sat->0, Sun->1, Mon->2 ...
        return day.minusDays(back.toLong())
    }

    /**
     * Compute streak state from a set of cooked days and a prefs snapshot.
     *
     * @param cookedDays distinct local days with ≥1 cook (future days ignored).
     * @param freezesLeft tokens the user currently holds.
     * @param weeklyFloor 1..7 cooks needed per week to keep a streak alive.
     */
    fun compute(
        cookedDays: Set<LocalDate>,
        today: LocalDate,
        freezesLeft: Int,
        weeklyFloor: Int,
    ): State {
        val days = cookedDays.filter { it <= today }.toSortedSet()
        if (days.isEmpty()) {
            return State(0, 0, freezesLeft, 0, weeklyFloor)
        }

        val last = days.last()
        val gap = java.time.temporal.ChronoUnit.DAYS.between(last, today)

        // Longest-ever run over the whole history.
        val ordered = days.toList()
        var longest = 1
        var run = 1
        for (i in 1 until ordered.size) {
            if (java.time.temporal.ChronoUnit.DAYS.between(ordered[i - 1], ordered[i]) == 1L) {
                run++
                longest = maxOf(longest, run)
            } else {
                run = 1
            }
        }

        val thisWeek = cooksThisWeek(days, today)
        var freezes = freezesLeft
        var frozenDay: LocalDate? = null

        // gap 0 = cooked today; gap 1 = yesterday cooked, today still pending.
        // Neither spends a token — the day isn't over yet.
        if (gap >= 2) {
            val missed = (gap - 1).toInt() // full days without a cook, excluding today
            if (missed > freezes) {
                // Weekly floor rescue: a met floor keeps the streak through a gap.
                if (thisWeek < weeklyFloor) {
                    return State(0, longest, freezes, thisWeek, weeklyFloor)
                }
            } else {
                freezes -= missed
                frozenDay = today
            }
        }

        // Count the consecutive run ending at `last`.
        var streak = 1
        var i = ordered.size - 1
        while (i > 0) {
            if (java.time.temporal.ChronoUnit.DAYS.between(ordered[i - 1], ordered[i]) != 1L) break
            streak++
            i--
        }
        longest = maxOf(longest, streak)

        return State(streak, longest, freezes, thisWeek, weeklyFloor, frozenDay)
    }

    /** Cooks falling in the Saturday-start week containing [day]. */
    fun cooksThisWeek(cookedDays: Collection<LocalDate>, day: LocalDate): Int {
        val start = weekStart(day)
        val end = start.plusDays(6)
        return cookedDays.count { it >= start && it <= end }
    }

    /** Grant the monthly token. Called on first app open of a new month. */
    fun grantMonthly(current: Int, lastGrantMonth: Int?, thisMonth: Int): Pair<Int, Int> {
        if (lastGrantMonth == thisMonth) return current to thisMonth
        val granted = (current + FREEZE_PER_MONTH).coerceAtMost(MAX_FREEZE_BANK)
        return granted to thisMonth
    }
}
