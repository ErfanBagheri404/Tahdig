package com.erfanbagheri.tahdig.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Body-weight trend math. Pure — dates come in, no clock.
 *
 * The defining decision: a 7-day moving average windows over CALENDAR days,
 * not over the last 7 entries. Someone who weighs themselves twice a week
 * still gets a true weekly average, and a day with no entry contributes
 * nothing rather than being counted as a zero (which would drag the line
 * down and invent weight loss that never happened).
 */
object WeightTrend {

    data class Entry(val date: LocalDate, val kg: Double)

    data class Point(val date: LocalDate, val kg: Double, val average: Double?)

    /**
     * [entries] may be sparse and unordered. Returns one point per entry,
     * oldest first, each carrying the trailing [window]-day average over
     * whatever entries exist in that calendar window (null when the window
     * has no entries at all — an unknown average must not render as 0).
     */
    fun series(entries: List<Entry>, window: Int = 7): List<Point> {
        if (entries.isEmpty()) return emptyList()
        val sorted = entries.sortedBy { it.date }
        return sorted.map { entry ->
            val start = entry.date.minusDays((window - 1).toLong())
            val inWindow = sorted.filter { it.date >= start && it.date <= entry.date }
            Point(
                date = entry.date,
                kg = entry.kg,
                average = inWindow.takeIf { it.isNotEmpty() }?.map { it.kg }?.average(),
            )
        }
    }

    /** Total change from the first to the last entry, or null with < 2 entries. */
    fun deltaKg(entries: List<Entry>): Double? {
        if (entries.size < 2) return null
        val sorted = entries.sortedBy { it.date }
        return sorted.last().kg - sorted.first().kg
    }

    /** Days between the first and last entry — the span the delta covers. */
    fun spanDays(entries: List<Entry>): Long? {
        if (entries.size < 2) return null
        val sorted = entries.sortedBy { it.date }
        return ChronoUnit.DAYS.between(sorted.first().date, sorted.last().date)
    }

    /**
     * Per-week rate of change (kg/week), or null when the span is too short
     * to say anything honest. A two-day span extrapolated to a week is
     * noise dressed as a trend, so require at least [minDays].
     */
    fun ratePerWeek(entries: List<Entry>, minDays: Long = 3): Double? {
        val delta = deltaKg(entries) ?: return null
        val days = spanDays(entries) ?: return null
        if (days < minDays) return null
        return delta / days * 7
    }

    /**
     * Whether the logged weight has drifted far enough from the weight the
     * calorie goal was computed from to be worth re-deriving it. A 2kg
     * threshold keeps the prompt from firing on normal daily fluctuation.
     */
    fun goalStale(goalWeightKg: Double?, latestKg: Double?, thresholdKg: Double = 2.0): Boolean {
        if (goalWeightKg == null || latestKg == null) return false
        return kotlin.math.abs(latestKg - goalWeightKg) >= thresholdKg
    }

    /** Farsi delta line for the trend header. */
    fun deltaLine(deltaKg: Double): String? {
        if (kotlin.math.abs(deltaKg) < 0.05) return null
        val rounded = (kotlin.math.abs(deltaKg) * 10).roundToInt() / 10.0
        val text = PersianText.toPersianDigits(rounded)
        return if (deltaKg < 0) "$text کیلو کمتر از اول" else "$text کیلو بیشتر از اول"
    }
}
