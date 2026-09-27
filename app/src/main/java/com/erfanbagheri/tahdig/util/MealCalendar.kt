package com.erfanbagheri.tahdig.util

import java.time.LocalDate
import java.time.ZoneId

/**
 * Meal-plan → device-calendar math (#84). Pure — no Context, no provider, so
 * slot→time, identity and title formatting are unit-testable on the JVM.
 * The same shape as [StreakMath]/[TasteScorer].
 *
 * - Week starts Saturday (Farsi week); [weekStart] mirrors StreakMath.
 * - Identity is a stable derivable key (week + day + slot), never the dish
 *   name, so editing a slot UPDATES the same event instead of doubling it.
 */
object MealCalendar {

    /** Dedicated device calendar created on first export. */
    const val CALENDAR_NAME = "ته‌دیگ"

    /** Timed events, one hour each. */
    const val EVENT_DURATION_MIN = 60L

    /** Meal slot → hour of day. Unknown slots land on noon, never midnight. */
    fun slotHour(slot: String): Int = when (slot) {
        "صبحانه" -> 8
        "ناهار" -> 13
        "شام" -> 20
        else -> 12
    }

    /** Saturday-indexed week start; stable across the whole week. */
    fun weekStart(day: LocalDate): LocalDate {
        // LocalDate: Monday=1 ... Sunday=7. Saturday = 6.
        val back = (day.dayOfWeek.value + 1) % 7 // Sat->0, Sun->1, Mon->2 ...
        return day.minusDays(back.toLong())
    }

    /** Epoch millis of the event start. [zone] is explicit so tests pin it. */
    fun eventStart(dayIndex: Int, slot: String, weekStart: LocalDate, zone: ZoneId): Long =
        weekStart.plusDays(dayIndex.coerceIn(0, 6).toLong())
            .atTime(slotHour(slot), 0)
            .atZone(zone).toInstant().toEpochMilli()

    /** Event end = start + [EVENT_DURATION_MIN]. */
    fun eventEnd(dayIndex: Int, slot: String, weekStart: LocalDate, zone: ZoneId): Long =
        eventStart(dayIndex, slot, weekStart, zone) + EVENT_DURATION_MIN * 60_000L

    /**
     * Stable identity for one planned slot in one week, e.g.
     * `tahdig:2026-09-26:3:ناهار`. Written to the event's CUSTOM_APP_URI so
     * re-export finds and updates the same row.
     */
    fun eventKey(weekStart: LocalDate, dayIndex: Int, slot: String): String =
        "tahdig:$weekStart:${dayIndex.coerceIn(0, 6)}:$slot"

    /** Event title, e.g. «ناهار: قورمه‌سبزی». Display only — never an identity. */
    fun title(slot: String, dishName: String): String = "$slot: $dishName"

    /** Event description; display only. */
    fun description(dishName: String): String = "$dishName — از برنامهٔ هفتگی ته‌دیگ"
}
