package com.erfanbagheri.tahdig.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
data class Occasion(
    val key: String,
    /** Farsi title — «شب یلدا». */
    val title: String,
    /** Shelf phrasing — «شب یلدا نزدیکه». */
    val shelf: String,
    val rule: OccasionRule,
    /** Curated dish ids from the seed. */
    val dishes: List<Long> = emptyList(),
)

@Serializable
data class OccasionRule(
    /** jalali = Jalali month/day window; hijri = Hijri month/day window (Ramadan, Muharram). */
    val type: String,
    /** [month, day] inclusive. from > to wraps across the year's boundary. */
    val from: List<Int>,
    val to: List<Int>,
)

/**
 * Bundled occasion windows (#88): active entirely offline from java.time —
 * Jalali via [JalaliDate], Hijri via [java.time.chrono.HijrahChronology].
 * File order = priority: a fixed window (یلدا) must beat the broad season behind it.
 */
object OccasionRegistry {
    private val json = Json { ignoreUnknownKeys = true }
    private var table: List<Occasion> = emptyList()

    /** App-start / test load; never throws — an empty library means no shelves ever show. */
    fun load(text: String) {
        table = runCatching { json.decodeFromString<List<Occasion>>(text) }
            .getOrDefault(emptyList())
    }

    val all: List<Occasion> get() = table

    /** The occasion in effect on [today], or null outside every window. */
    fun activeOn(today: LocalDate): Occasion? {
        val j = runCatching { JalaliDate.toJalali(today) }.getOrNull() ?: return null
        val hij = hijriMonthDay(today)
        return table.firstOrNull { active(it.rule, j, hij) }
    }

    private fun active(rule: OccasionRule, j: JalaliDate.JDate, hij: Pair<Int, Int>?): Boolean {
        val from = key(rule.from)
        val to = key(rule.to)
        return when (rule.type) {
            "jalali" -> JalaliDate.inWindow(from, to, j.key)
            "hijri" -> hij != null && JalaliDate.inWindow(from, to, hij.first * 100 + hij.second)
            else -> false
        }
    }

    private fun hijriMonthDay(today: LocalDate): Pair<Int, Int>? = runCatching {
        val h: java.time.chrono.ChronoLocalDate =
            java.time.chrono.HijrahChronology.INSTANCE.date(today)
        val month = h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
        val day = h.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
        month to day
    }.getOrNull()

    private fun key(v: List<Int>): Int = (v.getOrElse(0) { 0 }) * 100 + v.getOrElse(1) { 0 }
}
