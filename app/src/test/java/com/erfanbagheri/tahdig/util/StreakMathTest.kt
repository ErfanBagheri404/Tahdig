package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * #120: streak, freeze and weekly-floor math. Pure functions, no Android.
 */
class StreakMathTest {

    private val mon = LocalDate.of(2026, 9, 21) // Monday
    private val tue = mon.plusDays(1)
    private val wed = mon.plusDays(2)
    private val thu = mon.plusDays(3)
    private val sat = LocalDate.of(2026, 9, 19) // Saturday of the same week

    @Test
    fun noCooks_isZeroStreak() {
        val s = StreakMath.compute(emptySet(), mon, freezesLeft = 1, weeklyFloor = 3)
        assertEquals(0, s.current)
        assertEquals(0, s.longest)
    }

    @Test
    fun cookedToday_isAtLeastOne() {
        val s = StreakMath.compute(setOf(mon), mon, 0, 3)
        assertEquals(1, s.current)
        assertEquals(1, s.longest)
    }

    @Test
    fun consecutiveDays_incrementStreak() {
        val s = StreakMath.compute(setOf(sat, mon, tue), tue, 0, 3)
        assertEquals(2, s.current) // mon, tue
        assertEquals(2, s.longest) // sat is a separate run (sun missing)
    }

    @Test
    fun longestRun_survivesReset() {
        // 5 in a row long ago, then a 1-day streak today.
        val old = (1..5).map { LocalDate.of(2026, 1, it) }
        val s = StreakMath.compute((old + mon).toSet(), mon, 0, 3)
        assertEquals(1, s.current)
        assertEquals(5, s.longest)
    }

    @Test
    fun yesterdayCooked_todayPending_keepsStreakWithoutSpending() {
        // gap == 1: today isn't over yet — no token spent, no freeze recorded.
        val s = StreakMath.compute(setOf(mon), tue, freezesLeft = 1, weeklyFloor = 7)
        assertEquals(1, s.current)
        assertEquals(1, s.freezesLeft)
        assertEquals(null, s.frozenDay)
    }

    @Test
    fun fullDaySkip_withFreeze_spendsOneTokenAndHolds() {
        // last=Mon, today=Wed → Tuesday missed; one freeze absorbs it.
        val s = StreakMath.compute(setOf(mon), wed, freezesLeft = 1, weeklyFloor = 7)
        assertEquals(1, s.current)
        assertEquals(0, s.freezesLeft)
        assertEquals(wed, s.frozenDay)
    }

    @Test
    fun missedDays_exceedFreeze_resetsWithoutSpending() {
        // last=Mon, today=Thu → 2 missed > 1 freeze; floor 7 unmet → reset.
        val s = StreakMath.compute(setOf(mon), thu, freezesLeft = 1, weeklyFloor = 7)
        assertEquals(0, s.current)
        assertEquals(1, s.freezesLeft) // failed attempt never burns the token
        assertEquals(null, s.frozenDay)
    }

    @Test
    fun gapWithoutFreeze_resetsStreakButKeepsLongest() {
        val s = StreakMath.compute(setOf(mon, tue), thu, freezesLeft = 0, weeklyFloor = 7)
        assertEquals(0, s.current)
        assertEquals(2, s.longest)
    }

    @Test
    fun futureCooks_areIgnored() {
        val s = StreakMath.compute(setOf(mon, thu), tue, 0, 3)
        assertEquals(1, s.current)
    }

    @Test
    fun weekStart_isSaturday() {
        assertEquals(sat, StreakMath.weekStart(mon))
        assertEquals(sat, StreakMath.weekStart(LocalDate.of(2026, 9, 25))) // Friday
    }

    @Test
    fun cooksThisWeek_countsOnlyCurrentWeek() {
        // Week containing Mon 9/21 starts Sat 9/19 and ends Fri 9/25.
        val outside = LocalDate.of(2026, 9, 12) // prior Saturday
        val inWeek = LocalDate.of(2026, 9, 20)  // Sunday of the same week
        assertEquals(2, StreakMath.cooksThisWeek(listOf(outside, sat, mon), mon))
        assertEquals(3, StreakMath.cooksThisWeek(listOf(outside, sat, mon, inWeek), mon))
    }

    @Test
    fun weeklyFloorMet_rescuesGapWithoutFreezes() {
        // Cooked Sat+Mon (floor 2 met). Today is Thu: Tue+Wed missed, no freeze.
        val s = StreakMath.compute(setOf(sat, mon), thu, freezesLeft = 0, weeklyFloor = 2)
        assertEquals(2, s.thisWeek)
        assertEquals(1, s.current) // floor met, streak survives the gap
        assertEquals(0, s.freezesLeft)
    }

    @Test
    fun weeklyFloorNotMet_resets() {
        // Only one cook this week, floor 2, gap longer than a freeze can cover.
        val s = StreakMath.compute(setOf(mon), thu, freezesLeft = 1, weeklyFloor = 2)
        assertEquals(0, s.current)
    }

    @Test
    fun grantMonthly_isIdempotentWithinMonth() {
        val (n, m) = StreakMath.grantMonthly(1, lastGrantMonth = 9, thisMonth = 9)
        assertEquals(1, n)
        assertEquals(9, m)
    }

    @Test
    fun grantMonthly_capsBank() {
        val (n, _) = StreakMath.grantMonthly(3, lastGrantMonth = 8, thisMonth = 9)
        assertEquals(3, n) // MAX_FREEZE_BANK
    }

    @Test
    fun grantMonthly_neverGoesBelowZero() {
        val (n, m) = StreakMath.grantMonthly(0, lastGrantMonth = 8, thisMonth = 9)
        assertEquals(1, n)
        assertEquals(9, m)
    }
}
