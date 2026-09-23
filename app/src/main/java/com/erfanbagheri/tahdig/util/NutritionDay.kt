package com.erfanbagheri.tahdig.util

/**
 * What the «امروز» card renders (#110): consumed totals, the target they are
 * measured against, and the macro targets. [targetCal] == 0 means «بدون هدف» —
 * the card shows totals only and no ring percentages (see
 * [DailyBudget.ringFraction], which returns -1 for that case).
 */
data class NutritionDay(
    val consumedCal: Int = 0,
    val consumedProtein: Int = 0,
    val consumedFat: Int = 0,
    val consumedCarbs: Int = 0,
    val targetCal: Int = 0,
    /** protein/carb/fat gram targets; null in no-goal mode. */
    val macroTarget: Triple<Int, Int, Int>? = null,
) {
    val hasGoal: Boolean get() = targetCal > 0

    /** Remaining calories; negative when the day is over budget (shown as-is). */
    val remainingCal: Int get() = targetCal - consumedCal

    /** Ring fractions for calories/protein/carb/fat; -1 when there is no goal. */
    val rings: List<Double>
        get() {
            val m = macroTarget ?: return listOf(-1.0, -1.0, -1.0, -1.0)
            return listOf(
                DailyBudget.ringFraction(consumedCal, targetCal),
                DailyBudget.ringFraction(consumedProtein, m.first),
                DailyBudget.ringFraction(consumedCarbs, m.second),
                DailyBudget.ringFraction(consumedFat, m.third),
            )
        }
}
