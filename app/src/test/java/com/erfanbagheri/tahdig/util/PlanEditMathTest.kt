package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * #83 — the pure plan-editing math: reorder within a day, move a dish between
 * days, duplicate a day, repeat last week. These are the acceptance checkboxes;
 * the UI is a thin caller of exactly these functions.
 */
class PlanEditMathTest {

    private val wk = 1000L
    private fun slot(day: Int, meal: String) = PlanEditMath.Slot(wk, day, meal)
    private fun row(day: Int, meal: String, food: Long) = PlanEditMath.Row(slot(day, meal), food)

    // ---- week anchor math (Iranian week = Saturday start) ----

    @Test fun `weekStartOf lands on Saturday of the same week`() {
        val wed = LocalDate.of(2026, 9, 23) // Wednesday
        val sat = PlanEditMath.weekStartOf(wed)
        assertEquals(DayOfWeek.SATURDAY, sat.dayOfWeek)
        assertTrue(sat <= wed)
        assertTrue(wed.toEpochDay() - sat.toEpochDay() < 7)
    }

    @Test fun `weekStartOffset walks weeks both directions`() {
        val today = LocalDate.of(2026, 9, 23)
        val thisWeek = PlanEditMath.weekStartOf(today).toEpochDay()
        assertEquals(thisWeek + 7, PlanEditMath.weekStartOffset(today, 1))
        assertEquals(thisWeek - 7, PlanEditMath.weekStartOffset(today, -1))
        assertEquals(thisWeek, PlanEditMath.weekStartOffset(today, 0))
    }

    @Test fun `shiftDate moves the week anchor and keeps day and meal`() {
        val moved = PlanEditMath.shiftDate(slot(3, "ناهار"), 2)
        assertEquals(wk + 14, moved.weekStartEpochDay)
        assertEquals(3, moved.dayIndex)
        assertEquals("ناهار", moved.mealSlot)
    }

    // ---- move between days ----

    @Test fun `move onto an empty cell empties the source`() {
        val src = row(1, "شام", 7)
        val m = PlanEditMath.planMove(src, targetDayIndex = 2, targetMealSlot = "شام", targetOccupant = null)
        assertEquals(PlanEditMath.Slot(wk, 2, "شام"), m.target.slot)
        assertEquals(7L, m.target.foodId)
        assertTrue(m.sourceEmptied)
        assertNull(m.displaced)
    }

    @Test fun `move onto an occupied cell carries the displaced dish`() {
        val src = row(1, "شام", 7)
        val occupant = row(2, "شام", 9)
        val m = PlanEditMath.planMove(src, 2, "شام", occupant)
        assertTrue(m.sourceEmptied)
        assertEquals(9L, m.displaced?.foodId)
    }

    @Test fun `move onto the same cell is a no-op`() {
        val src = row(1, "شام", 7)
        val m = PlanEditMath.planMove(src, 1, "شام", null)
        assertFalse(m.sourceEmptied)
        assertNull(m.displaced)
        assertEquals(7L, m.target.foodId)
    }

    // ---- reorder within a day ----

    @Test fun `reorder moves a row and keeps the set`() {
        val rows = listOf(row(0, "صبحانه", 1), row(0, "ناهار", 2), row(0, "شام", 3))
        val out = PlanEditMath.reorder(rows, from = 0, to = 2)
        assertEquals(2L, out.first().foodId)
        assertEquals(1L, out[2].foodId)
        assertEquals(rows.map { it.foodId }.sorted(), out.map { it.foodId }.sorted())
    }

    @Test fun `reorder out of range returns the same list`() {
        val rows = listOf(row(0, "ناهار", 2))
        assertEquals(rows, PlanEditMath.reorder(rows, from = 5, to = 0))
        assertEquals(rows, PlanEditMath.reorder(rows, from = 0, to = 0))
    }

    // ---- duplicate a day ----

    @Test fun `duplicateDay copies dishes to the next day and skips occupied cells`() {
        val rows = listOf(row(0, "ناهار", 1), row(0, "شام", 2), row(1, "ناهار", 5))
        val out = PlanEditMath.duplicateDay(rows, dayIndex = 0, slotsInDay = listOf("ناهار", "شام"))
        // day 1 already has ناهار (dish 5) -> must not be overwritten; شام gets copied
        val day1 = out.filter { it.slot.dayIndex == 1 }.associate { it.slot.mealSlot to it.foodId }
        assertEquals(5L, day1["ناهار"])
        assertEquals(2L, day1["شام"])
    }

    @Test fun `duplicateDay wraps the last day to the first`() {
        val rows = listOf(row(6, "شام", 4))
        val out = PlanEditMath.duplicateDay(rows, dayIndex = 6, slotsInDay = listOf("شام"))
        assertTrue(out.any { it.slot.dayIndex == 0 && it.slot.mealSlot == "شام" && it.foodId == 4L })
    }

    // ---- repeat last week (the overwritten-dish bug) ----

    @Test fun `repeatLastWeek fills only empty cells of the current week`() {
        val lastWeek = listOf(row(0, "ناهار", 1), row(1, "شام", 2))
        // The current week already has dish 9 planned for day 0 ناهار.
        val currentStart = wk + 7
        val currentWeek = listOf(PlanEditMath.Row(PlanEditMath.Slot(currentStart, 0, "ناهار"), 9))
        val out = PlanEditMath.repeatLastWeek(lastWeek, currentWeek, currentStart)

        val planned = out.associate { it.slot.dayIndex to it.foodId }
        // The pre-existing dish survives untouched.
        val kept = currentWeek.first()
        assertEquals(9L, kept.foodId)
        // The repeat only fills the empty day-1 slot.
        assertEquals(mapOf(1 to 2L), planned)
        for (r in out) assertEquals(currentStart, r.slot.weekStartEpochDay)
    }

    @Test fun `repeatLastWeek copies every empty slot with dates shifted`() {
        val lastWeek = listOf(row(0, "ناهار", 1), row(1, "شام", 2))
        val currentStart = wk + 7
        val out = PlanEditMath.repeatLastWeek(lastWeek, emptyList(), currentStart)
        assertEquals(2, out.size)
        assertEquals(
            listOf(Pair(0, 1L), Pair(1, 2L)),
            out.map { it.slot.dayIndex to it.foodId },
        )
    }
}
