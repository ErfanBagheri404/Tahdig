package com.erfanbagheri.tahdig.util

import java.time.LocalDate

/**
 * Gregorian → Jalali conversion for date-window rules (#88).
 * Arithmetic algorithm (jalaali-js / Birashk family), valid well past our range.
 */
object JalaliDate {

    data class JDate(val year: Int, val month: Int, val day: Int) {
        /** Comparison key: month*100+day, year ignored — windows compare within one Jalali year. */
        val key: Int get() = month * 100 + day
    }

    /** March-based cumulative days, jalaali-js `g_d_m` — deliberately WITHOUT a leap-day adjustment; the `(gy2+3)/4` term below carries leap days. */
    private val G_MONTH_DAYS = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)

    /**
     * Convert a Gregorian date to Jalali.
     * jalaali-js `toJalali`, ported line-for-line (breakpoints 12053/1461 cycles).
     */
    fun toJalali(date: LocalDate): JDate {
        var gy = date.year
        val gm = date.monthValue
        val gd = date.dayOfMonth
        val gDays = G_MONTH_DAYS[gm - 1] + gd

        var jy: Int
        if (gy > 1600) {
            jy = 979
            gy -= 1600
        } else {
            jy = 0
            gy -= 621
        }
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 - 80 + gDays

        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
        val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
        return JDate(jy, jm, jd)
    }

    /** True when [day] (month*100+day) falls inside window [from, to], wrapping across the new year. */
    fun inWindow(from: Int, to: Int, day: Int): Boolean =
        if (from <= to) day in from..to else day >= from || day <= to
}
