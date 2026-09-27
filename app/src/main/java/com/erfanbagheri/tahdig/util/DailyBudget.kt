package com.erfanbagheri.tahdig.util

/**
 * Daily calorie budget + macro split from a minimal profile (#110), pure so
 * the AC's BMR/TDEE and ring-percentage tests run without prefs or a DB.
 *
 * Mifflin-St Jeor (the standard for adults), then activity as a TDEE factor,
 * then goal as a delta on top. Everything Int — calories are never fractional
 * in a daily budget, and fake precision is worse than rounding.
 */
object DailyBudget {

    /** Activity multipliers (light -> athlete). Ordinal indexes the store key. */
    val ACTIVITY = listOf(1.2, 1.375, 1.55, 1.725, 1.9)

    /** Goal deltas in kcal: cut / maintain / gain. */
    val GOALS = listOf(-400, 0, 400)

    /** Profile for one household member. 0 = "no goal set" (totals only). */
    @kotlinx.serialization.Serializable
    data class Profile(
        val age: Int = 30,
        val weightKg: Double = 70.0,
        val heightCm: Double = 170.0,
        /** Index into [ACTIVITY]; defaults to lightly active. */
        val activity: Int = 1,
        /** Index into [GOALS]; defaults to maintain. */
        val goal: Int = 1,
        /** false = «بدون هدف» mode — the card shows totals only. */
        val hasGoal: Boolean = false,
    )

    /**
     * Mifflin-St Jeor BMR with the male constant (+5).
     *
     * ponytail: the issue's profile has no sex field, so the female variant
     * (-161) is unreachable. Add a `sex` flag and switch the constant when the
     * profile grows one; until then one formula beats a fudged average.
     */
    fun bmr(p: Profile): Int =
        (10.0 * p.weightKg + 6.25 * p.heightCm - 5.0 * p.age + 5).toInt()

    /** TDEE = BMR x activity factor. */
    fun tdee(p: Profile): Int = (bmr(p) * ACTIVITY[p.activity.coerceIn(0, ACTIVITY.lastIndex)]).toInt()

    /** Daily budget after the goal delta. */
    fun budget(p: Profile): Int =
        (tdee(p) + GOALS[p.goal.coerceIn(0, GOALS.lastIndex)]).coerceAtLeast(1200)

    /**
     * Macro split in grams for a calorie budget: 4/4/9 kcal per gram of
     * protein/carb/fat, split 30/40/30 — the issue's default split.
     */
    fun macroSplit(budgetCal: Int): Triple<Int, Int, Int> {
        val protein = (budgetCal * 0.30 / 4).toInt()
        val carb = (budgetCal * 0.40 / 4).toInt()
        val fat = (budgetCal * 0.30 / 9).toInt()
        return Triple(protein, carb, fat)
    }

    /**
     * Ring fraction 0..1 for consumed vs target. Never negative, never >1 —
     * the arc clamps so an over-eaten day draws a full circle, not an
     * inverted one. [target] <= 0 (no-goal mode) returns -1 so the caller
     * knows to draw a totals-only ring instead of dividing by zero.
     */
    fun ringFraction(consumed: Int, target: Int): Double =
        if (target <= 0) -1.0 else (consumed.toDouble() / target).coerceIn(0.0, 1.0)
}
