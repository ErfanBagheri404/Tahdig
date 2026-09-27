package com.erfanbagheri.tahdig.util

import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Water intake math. Pure — no Context, no clock: the caller passes the
 * date it believes it is, which is what makes the midnight boundary
 * testable at all.
 */
object WaterMath {

    /** Default target: 35ml per kg of body weight (the common clinical rule). */
    const val ML_PER_KG = 35.0

    /**
     * A single glass step, and the amounts the stepper offers.
     * The issue asks for +0.5 / +1 / +2 custom — those are multiples of the
     * glass, not fixed millilitres, so a 250ml glass gives 125 / 250 / 500.
     */
    val STEP_MULTIPLIERS = listOf(0.5, 1.0, 2.0)

    const val DEFAULT_GLASS_ML = 200

    /**
     * Daily target in ml. [overrideMl] wins when the user set one, so a
     * manual target is never silently replaced by the weight-derived one.
     * Returns null without a weight rather than assuming a 60kg adult.
     */
    fun targetMl(weightKg: Double?, glassSizeMl: Int, overrideMl: Int?): Int? {
        overrideMl?.let { return it.coerceAtLeast(0) }
        val kg = weightKg?.takeIf { it > 0 } ?: return null
        return (kg * ML_PER_KG).roundToInt().coerceAtLeast(glassSizeMl)
    }

    /** Millilitres added by tapping the stepper [multiplier] times the glass. */
    fun stepMl(glassSizeMl: Int, multiplier: Double): Int =
        (glassSizeMl * multiplier).roundToInt().coerceAtLeast(0)

    /**
     * The midnight boundary. Logged on [lastLoggedDate], viewed on [today]:
     * the counter is a fresh day iff the LOCAL date changed — comparing
     * absolute instants breaks across timezone offsets and DST, and
     * `toHours()` truncation would keep yesterday's total alive for up to
     * 24h after midnight.
     */
    fun isNewDay(lastLoggedDate: LocalDate?, today: LocalDate): Boolean =
        lastLoggedDate == null || lastLoggedDate != today

    /**
     * The day bucketing used by the 7-day bar row, oldest first. Built from
     * the CALENDAR, not from the logged entries: a bar row with a hole in it
     * lies about the week, so days with no intake still get a (zero) bar.
     */
    fun weekBuckets(today: LocalDate, days: Int = 7): List<LocalDate> =
        (days - 1 downTo 0).map { today.minusDays(it.toLong()) }

    /** Progress in 0..1, or null when there is no target to measure against. */
    fun progress(consumedMl: Int, targetMl: Int?): Float? {
        val target = targetMl?.takeIf { it > 0 } ?: return null
        return (consumedMl.toFloat() / target).coerceIn(0f, 1f)
    }
}
