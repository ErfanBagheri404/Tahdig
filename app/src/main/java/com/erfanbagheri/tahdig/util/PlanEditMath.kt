package com.erfanbagheri.tahdig.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Plan-editing math (#83): drag/drop reorder, duplicate a day, repeat last week.
 *
 * The `meal_plan` unique key is (weekStartEpochDay, dayIndex, mealSlot), so
 * every write targets exactly one cell and no write needs a sibling-slot rule.
 *
 * AC: "week-offset date math (Saturday-start Iranian week)" — [weekStartOf] and
 * [weekStartOffset] ARE that math; [shiftDate] is the AC's "dates shift, dishes
 * identical".
 */
object PlanEditMath {

    /** Grid cell: week anchor (epochDay of its شنبه), day 0=شنبه..6=جمعه, meal-slot label. */
    data class Slot(val weekStartEpochDay: Long, val dayIndex: Int, val mealSlot: String) {
        /** The concrete calendar day this cell shows. */
        fun date(): LocalDate = LocalDate.ofEpochDay(weekStartEpochDay).plusDays(dayIndex.toLong())
    }

    /** One (cell, dish) row — the shape both the DAO and this math speak. */
    data class Row(val slot: Slot, val foodId: Long)

    /** Both rows a drop touches: the target gains the dish, the source ends up empty. */
    data class Move(
        val target: Row,
        val sourceEmptied: Boolean,
        /** Rows displaced by the target, to be restored if the user undoes. */
        val displaced: Row?,
    )

    /** شنبه opening the Iranian week that contains [day]. */
    fun weekStartOf(day: LocalDate): LocalDate =
        day.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))

    /** epochDay of the شنبه opening the week [n] weeks after [today]'s week; n may be negative. */
    fun weekStartOffset(today: LocalDate, n: Int): Long =
        weekStartOf(today).plusWeeks(n.toLong()).toEpochDay()

    /** The date shown for a cell — the AC's "dates shift". */
    fun shiftDate(slot: Slot, weeks: Int): Slot =
        Slot(slot.weekStartEpochDay + 7L * weeks, slot.dayIndex, slot.mealSlot)

    /**
     * Where a long-press drag ends up.
     *
     * The cell is addressed by (week, dayIndex, mealSlot) — never by visual
     * position. RTL mirrors the DRAWING of the day tabs (شنبه rightmost), never
     * the index, so a finger moving toward a later روز lands on the later
     * dayIndex; the caller passes the index the drop was measured against.
     */
    fun dropTarget(source: Slot, targetDayIndex: Int, targetMealSlot: String): Slot =
        Slot(source.weekStartEpochDay, targetDayIndex, targetMealSlot)

    /**
     * The two writes one drop performs. Dropping onto an occupied cell SWAPS
     * rather than destroys: a drag is a move the user can undo, and silently
     * deleting the dish it landed on is the one outcome nobody expects.
     * Dropping a cell back on itself is a no-op ([sourceEmptied] false, no displaced row).
     */
    fun planMove(source: Row, targetDayIndex: Int, targetMealSlot: String, targetOccupant: Row?): Move {
        val target = dropTarget(source.slot, targetDayIndex, targetMealSlot)
        if (target == source.slot) return Move(Row(target, source.foodId), sourceEmptied = false, displaced = null)
        return Move(
            target = Row(target, source.foodId),
            sourceEmptied = true,
            displaced = targetOccupant,
        )
    }

    /**
     * Reorder rows inside one day (#83 drag-and-drop between meal slots) while
     * keeping the SET of dishes and the SET of occupied cells untouched: each
     * dish lands in the cell whose slot the finger dropped it on.
     *
     * `from`/`to` are positions in the day-ordered list the screen draws, so the
     * RTL day-tab direction never enters the arithmetic.
     */
    fun reorder(dayRows: List<Row>, from: Int, to: Int): List<Row> {
        if (from !in dayRows.indices || to !in dayRows.indices || from == to) return dayRows
        val out = dayRows.toMutableList()
        val moved = out.removeAt(from)
        out.add(to, moved)
        return out
    }

    /** #83 «تکرار» — copy the dish into the next slot of the same day; occupied cells are left alone. */
    fun duplicateSlot(rows: List<Row>, source: Slot, slotsInDay: List<String>): List<Row> {
        val row = rows.firstOrNull { it.slot == source } ?: return rows
        val slotIndex = slotsInDay.indexOf(source.mealSlot)
        if (slotIndex < 0) return rows
        val nextSlot = slotsInDay.getOrNull(slotIndex + 1) ?: return rows
        val target = Slot(source.weekStartEpochDay, source.dayIndex, nextSlot)
        if (rows.any { it.slot == target }) return rows
        return (rows + Row(target, row.foodId)).sortedBy { it.slot.dayIndex }
    }

    /** #83 «تکرار روز» — copy every dish of [dayIndex] onto the next day, skipping occupied cells. */
    fun duplicateDay(rows: List<Row>, dayIndex: Int, slotsInDay: List<String>, dayCount: Int = 7): List<Row> {
        val source = rows.filter { it.slot.dayIndex == dayIndex }
        if (source.isEmpty()) return rows
        val targetDay = (dayIndex + 1) % dayCount
        val out = rows.toMutableList()
        for (row in source) {
            val target = Slot(row.slot.weekStartEpochDay, targetDay, row.slot.mealSlot)
            if (out.none { it.slot == target }) out.add(Row(target, row.foodId))
        }
        return out.sortedWith(compareBy({ it.slot.dayIndex }, { it.slot.mealSlot }))
    }

    /**
     * #83 AC «کپی هفته قبل» — every row of [lastWeekRows] re-anchored to the
     * current week: same day, same meal, same dish, date shifted by [weeks].
     * Cells already occupied in the target week are left alone, so the copy can
     * never silently drop a dish the user already planned.
     */
    fun repeatLastWeek(
        lastWeekRows: List<Row>,
        currentWeekRows: List<Row>,
        currentWeekStartEpochDay: Long,
        weeks: Int = -1,
    ): List<Row> {
        // The occupancy check must run against the TARGET week. Seeding it with
        // `lastWeekRows` (the source week) compares different week anchors and
        // never collides, which silently overwrote dishes the user already
        // planned — the one outcome the AC forbids.
        val taken = currentWeekRows.map { it.slot }.toMutableSet()
        val out = mutableListOf<Row>()
        for (row in lastWeekRows) {
            val target = shiftDate(row.slot, weeks).copy(weekStartEpochDay = currentWeekStartEpochDay)
            if (target in taken) continue
            taken.add(target)
            out.add(Row(target, row.foodId))
        }
        return out.sortedWith(compareBy({ it.slot.dayIndex }, { it.slot.mealSlot }))
    }
}
