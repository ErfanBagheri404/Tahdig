package com.erfanbagheri.tahdig.util

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * Meal-slot assignment and weekly aggregation (#114).
 *
 * Pure: times and dates come in as arguments, never read from a clock, which
 * is what makes "a log at 11:00 lands in LUNCH" an assertable fact rather
 * than something you have to catch at 11:00 in the morning.
 */
object MealDiary {

    /**
     * The slots the diary shows, in the order they appear. LIGHT_DINNER is
     * included on purpose: a 23:00 log lands there, and a slot the diary
     * never renders would hide real rows.
     */
    val SLOTS = listOf(
        MealTimeHelper.BREAKFAST,
        MealTimeHelper.LUNCH,
        MealTimeHelper.SNACK,
        MealTimeHelper.DINNER,
        MealTimeHelper.LIGHT_DINNER,
    )

    /** Slots that actually have rows, in display order — empty ones drop out. */
    fun visibleSlots(grouped: Map<String, List<*>>): List<String> =
        SLOTS.filter { !grouped[it].isNullOrEmpty() }

    /**
     * Slot boundaries as minutes-since-midnight, matching the ranges
     * [MealTimeHelper] uses for its own clock-based guess. Kept here as data
     * so the diary's rule is readable and testable in one place.
     */
    private val RANGES = listOf(
        Triple(360, 629, MealTimeHelper.BREAKFAST),    // 06:00–10:29
        Triple(690, 869, MealTimeHelper.LUNCH),        // 11:30–14:29
        Triple(900, 1049, MealTimeHelper.SNACK),      // 15:00–17:29
        Triple(1110, 1319, MealTimeHelper.DINNER),     // 18:30–21:59
    )

    /**
     * The slot a log at [minutesOfDay] belongs to. Gaps between the ranges
     * are SNACK, matching [MealTimeHelper] — a 10:45 log is a mid-morning
     * snack, not a breakfast stretched twenty minutes. The late-evening tail
     * and the post-midnight hour read as LIGHT_DINNER, same as the clock.
     */
    fun slotForMinutes(minutesOfDay: Int): String {
        val t = minutesOfDay.coerceIn(0, 1439)
        RANGES.firstOrNull { t in it.first..it.second }?.let { return it.third }
        return if (t >= 1320 || t <= 59) MealTimeHelper.LIGHT_DINNER else MealTimeHelper.SNACK
    }

    /** Convenience over [slotForMinutes] — callers pass a LocalTime, not math. */
    fun slotFor(time: LocalTime): String = slotForMinutes(time.hour * 60 + time.minute)

    /** Group rows into slots, dropping any bucket the user never wrote in. */
    fun <T> groupBySlot(rows: List<T>, slotOf: (T) -> String): Map<String, List<T>> =
        rows.groupBy(slotOf)
}
