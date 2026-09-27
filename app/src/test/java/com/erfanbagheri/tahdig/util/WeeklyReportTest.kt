package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeeklyReportTest {

    private fun day(n: Long, cal: Int, pro: Int = 0, fat: Int = 0, carb: Int = 0) =
        WeeklyReport.DayTotals(LocalDate.of(2026, 9, 1).plusDays(n), cal, pro, fat, carb)

    private fun view(cal: Int, pro: Int = 0, fat: Int = 0, carb: Int = 0) =
        NutritionLogEntityView(cal, pro, fat, carb)

    // --- AC: aggregates match hand-computed fixtures -----------------------

    @Test
    fun `totals sum the rows of each day`() {
        val rows = listOf(
            LocalDate.of(2026, 9, 1) to view(400, 20, 10, 50),
            LocalDate.of(2026, 9, 1) to view(300, 15, 5, 30),
            LocalDate.of(2026, 9, 2) to view(500, 25, 12, 60),
        )
        val days = WeeklyReport.totalsByDay(rows)
        assertEquals(2, days.size)
        assertEquals(700, days[0].calories)
        assertEquals(35, days[0].protein)
        assertEquals(500, days[1].calories)
    }

    @Test
    fun `averages divide by the LOGGED days not by seven`() {
        // 2000 over 3 logged days = 666.67 -> 667. Averaging over 7 would
        // report 286 and look like starvation nobody logged.
        val s = WeeklyReport.summarize(listOf(day(0, 600), day(1, 700), day(2, 700)))!!
        assertEquals(667, s.avgCalories)
        assertEquals(3, s.loggedDays)
    }

    @Test
    fun `an unlogged day never drags the average down`() {
        val s = WeeklyReport.summarize(listOf(day(0, 800), day(1, 0), day(2, 800)))!!
        assertEquals(800, s.avgCalories)
        assertEquals(2, s.loggedDays)
        assertEquals(1600, s.totalCalories)
    }

    @Test
    fun `macro averages compute the same way`() {
        // (100+50)/2 = 75 protein, (200+100)/2 = 150 fat
        val s = WeeklyReport.summarize(
            listOf(day(0, 0, 100, 200, 300), day(1, 0, 50, 100, 50)),
        )!!
        assertEquals(75, s.avgProtein)
        assertEquals(150, s.avgFat)
        assertEquals(175, s.avgCarbs)
    }

    @Test
    fun `a week with nothing logged has no summary`() {
        // Null, not a zero-average card that reads like "you ate nothing".
        assertNull(WeeklyReport.summarize(emptyList()))
        assertNull(WeeklyReport.summarize(listOf(day(0, 0), day(1, 0))))
    }

    @Test
    fun `a day logged as only carbs counts as logged`() {
        val s = WeeklyReport.summarize(listOf(day(0, 0, 0, 0, 50)))!!
        assertEquals(1, s.loggedDays)
    }

    // --- top dishes --------------------------------------------------------

    @Test
    fun `top dishes rank by log count and cap at three`() {
        val counts = listOf("الف" to 2, "ب" to 5, "ج" to 4, "د" to 9, "ه" to 1)
        val top = WeeklyReport.topDishes(counts)
        assertEquals(3, top.size)
        assertEquals(listOf("د", "ب", "ج"), top.map { it.first })
    }

    @Test
    fun `ties break alphabetically so the list is stable`() {
        val counts = listOf("نارنج" to 3, "آب" to 3, "پ" to 3)
        assertEquals(listOf("آب", "نارنج", "پ"), WeeklyReport.topDishes(counts).map { it.first })
    }

    @Test
    fun `no dishes logged yields an empty top list`() {
        assertTrue(WeeklyReport.topDishes(emptyList()).isEmpty())
    }

    // --- logging streak ----------------------------------------------------

    @Test
    fun `streak counts consecutive logged days ending today`() {
        val days = listOf(day(0, 500), day(1, 500), day(2, 500))
        assertEquals(3, WeeklyReport.loggingStreak(days, LocalDate.of(2026, 9, 3)))
    }

    @Test
    fun `today not logged yet does not reset the streak`() {
        // Opening the app before breakfast must not zero a 2-day streak.
        val days = listOf(day(0, 500), day(1, 500))
        assertEquals(2, WeeklyReport.loggingStreak(days, LocalDate.of(2026, 9, 3)))
    }

    @Test
    fun `a missed day in the middle breaks the streak`() {
        val days = listOf(day(0, 500), day(1, 0), day(2, 500))
        assertEquals(1, WeeklyReport.loggingStreak(days, LocalDate.of(2026, 9, 3)))
    }

    @Test
    fun `no logs at all is a zero streak`() {
        assertEquals(0, WeeklyReport.loggingStreak(emptyList(), LocalDate.of(2026, 9, 3)))
    }

    @Test
    fun `the last entry being old does not count as a live streak`() {
        val days = listOf(day(0, 500))
        // Today is the 10th, the only log the 1st: the streak ended.
        assertEquals(0, WeeklyReport.loggingStreak(days, LocalDate.of(2026, 9, 10)))
    }

    // --- carry-over --------------------------------------------------------

    @Test
    fun `unspent calories roll into tomorrow`() {
        // 2000 target, 1500 eaten -> 500 carried.
        assertEquals(500, WeeklyReport.carryOver(2000, 1500, enabled = true))
    }

    @Test
    fun `overshooting the target carries nothing`() {
        // Never negative: a day over budget must not REDUCE tomorrow's
        // allowance into a healthy-looking negative number.
        assertEquals(0, WeeklyReport.carryOver(2000, 2500, enabled = true))
    }

    @Test
    fun `carry over is off when the toggle is off`() {
        assertEquals(0, WeeklyReport.carryOver(2000, 1000, enabled = false))
    }

    @Test
    fun `carry over needs a target to be meaningful`() {
        assertEquals(0, WeeklyReport.carryOver(0, 0, enabled = true))
    }
}
