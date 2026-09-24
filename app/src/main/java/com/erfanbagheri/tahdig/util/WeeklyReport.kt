package com.erfanbagheri.tahdig.util

import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Weekly report aggregation (#114). Pure — days come in keyed by date.
 *
 * The honesty rule that shapes most of this: averages are over the days
 * that were actually LOGGED, not over all seven. A user who logged three
 * days has an average of three days, and the report says so ("میانگین ۳ روز
 * ثبت‌شده") — averaging seven days and silently counting four zeros would
 * report starvation the user never logged.
 */
object WeeklyReport {

    data class DayTotals(
        val date: LocalDate,
        val calories: Int,
        val protein: Int,
        val fat: Int,
        val carbs: Int,
    ) {
        /** A day with no food at all. Never counted as a logged day. */
        val logged: Boolean get() = calories > 0 || protein > 0 || fat > 0 || carbs > 0
    }

    data class Summary(
        val avgCalories: Int,
        val avgProtein: Int,
        val avgFat: Int,
        val avgCarbs: Int,
        val loggedDays: Int,
        val totalCalories: Int,
    )

    /** Per-day totals from flat log rows. */
    fun totalsByDay(
        rows: List<Pair<LocalDate, NutritionLogEntityView>>,
    ): List<DayTotals> =
        rows.groupBy { it.first }
            .map { (date, group) ->
                DayTotals(
                    date = date,
                    calories = group.sumOf { it.second.calories },
                    protein = group.sumOf { it.second.protein },
                    fat = group.sumOf { it.second.fat },
                    carbs = group.sumOf { it.second.carbs },
                )
            }
            .sortedBy { it.date }

    /**
     * Averages over the LOGGED days only. [loggedDays] travels with the
     * result so the UI can say how many days it actually averaged.
     */
    fun summarize(days: List<DayTotals>): Summary? {
        val logged = days.filter { it.logged }
        if (logged.isEmpty()) return null
        val n = logged.size
        return Summary(
            avgCalories = (logged.sumOf { it.calories }.toDouble() / n).roundToInt(),
            avgProtein = (logged.sumOf { it.protein }.toDouble() / n).roundToInt(),
            avgFat = (logged.sumOf { it.fat }.toDouble() / n).roundToInt(),
            avgCarbs = (logged.sumOf { it.carbs }.toDouble() / n).roundToInt(),
            loggedDays = n,
            totalCalories = logged.sumOf { it.calories },
        )
    }

    /**
     * The three most-logged dishes. Counts LOG ROWS, not days, so a dish
     * cooked twice on Sunday genuinely outranks one cooked once — and ties
     * break alphabetically so the list is stable between recompositions
     * instead of shuffling on a HashMap's iteration order.
     */
    fun topDishes(counts: List<Pair<String, Int>>): List<Pair<String, Int>> =
        counts.sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
            .take(3)
            .filter { it.second > 0 }

    /**
     * Logging streak: consecutive days ending [today] (or yesterday, if today
     * is not logged yet — otherwise opening the app before breakfast resets a
     * 30-day streak to zero).
     */
    fun loggingStreak(days: List<DayTotals>, today: LocalDate): Int {
        val logged = days.filter { it.logged }.map { it.date }.toSet()
        if (logged.isEmpty()) return 0
        var cursor = if (logged.contains(today)) today else today.minusDays(1)
        if (!logged.contains(cursor)) return 0
        var streak = 0
        while (logged.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /**
     * Unspent calories rolling into tomorrow. Negative surplus is clamped to
     * zero: a day over budget does not get to REDUCE tomorrow's allowance
     * below zero and then read as a healthy-looking negative number.
     */
    fun carryOver(todayTarget: Int, todayConsumed: Int, enabled: Boolean): Int {
        if (!enabled || todayTarget <= 0) return 0
        return (todayTarget - todayConsumed).coerceAtLeast(0)
    }
}

/** The four numbers a log row contributes — keeps the util free of Room. */
data class NutritionLogEntityView(
    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int,
)
