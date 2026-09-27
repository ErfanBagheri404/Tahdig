package com.erfanbagheri.tahdig.util

/**
 * Micronutrient %DV table and threshold badges (#117).
 *
 * %DV references are the standard adult Daily Values (FDA-style), so a
 * percentage the user has seen elsewhere reads the same here. Threshold
 * badges are bundled rules, not medical claims.
 */
object MicroNutrients {

    /**
     * One row in the micro table. [amount] is null when the source had no
     * data — the row still renders, showing «—». Dropping it would hide the
     * gap; zero would invent a value.
     */
    data class Row(
        val label: String,
        val amount: Double?,
        val unit: String,
        val dvPercent: Int?,
    )

    /** Badge derived from thresholds — surfaced as filter chips. */
    enum class Badge(val label: String) {
        HIGH_FIBER("فیبر بالا"),
        LOW_SODIUM("کم‌سدیم"),
        HIGH_IRON("آهن بالا"),
    }

    // ── Daily Values (mg unless noted) ─────────────────────────────
    private const val DV_FIBER = 28.0
    private const val DV_SODIUM = 2300.0
    private const val DV_POTASSIUM = 4700.0
    private const val DV_CALCIUM = 1300.0
    private const val DV_IRON = 18.0
    private const val DV_VITD = 20.0   // µg
    private const val DV_B12 = 2.4     // µg

    /**
     * %DV with one rule that keeps it honest: a missing DV is null, not 0 —
     * a zero bar would read as "none of this nutrient is good for you".
     */
    fun dvPercent(amount: Double, dv: Double?): Int? {
        if (dv == null || dv <= 0.0) return null
        return ((amount / dv) * 100).toInt()
    }

    /**
     * Build every micro row. A nutrient the source lacked keeps its row with a
     * null amount, which the UI renders as «—» — the gap stays visible and
     * nothing is imputed. Estimates are filtered upstream.
     */
    fun rows(
        fiberG: Double?,
        sodiumMg: Double?,
        potassiumMg: Double?,
        calciumMg: Double?,
        ironMg: Double?,
        vitDUg: Double?,
        b12Ug: Double?,
    ): List<Row> = listOf(
        Row("فیبر", fiberG, "گرم", fiberG?.let { dvPercent(it, DV_FIBER) }),
        Row("سدیم", sodiumMg, "میلی‌گرم", sodiumMg?.let { dvPercent(it, DV_SODIUM) }),
        Row("پتاسیم", potassiumMg, "میلی‌گرم", potassiumMg?.let { dvPercent(it, DV_POTASSIUM) }),
        Row("کلسیم", calciumMg, "میلی‌گرم", calciumMg?.let { dvPercent(it, DV_CALCIUM) }),
        Row("آهن", ironMg, "میلی‌گرم", ironMg?.let { dvPercent(it, DV_IRON) }),
        Row("ویتامین D", vitDUg, "میکروگرم", vitDUg?.let { dvPercent(it, DV_VITD) }),
        Row("ویتامین B12", b12Ug, "میکروگرم", b12Ug?.let { dvPercent(it, DV_B12) }),
    )

    // ── Threshold badges ────────────────────────────────────────────

    /** High fiber: ≥ 6 g per serving (20% of DV is the "good source" floor). */
    private const val HIGH_FIBER_AT = 6.0

    /** Low sodium: ≤ 140 mg per serving (FDA "low sodium" claim threshold). */
    private const val LOW_SODIUM_AT = 140.0

    /** High iron: ≥ 3.6 mg per serving (20% of DV). */
    private const val HIGH_IRON_AT = 3.6

    /**
     * Accumulate a tracked nutrient across today's logged meals and evaluate it
     * against the cap (#117). Meals with no data for the nutrient are SKIPPED
     * rather than counted as zero — otherwise a day of estimates would look
     * like a healthy low-sodium day.
     *
     * [mealAmounts] is one map per logged meal; a null value inside a map means
     * that meal had no data for the nutrient.
     */
    fun dailyTotal(
        mealAmounts: List<Map<NutrientCaps.Nutrient, Double?>>,
        nutrient: NutrientCaps.Nutrient,
    ): Double {
        var total = 0.0
        var counted = 0
        for (meal in mealAmounts) {
            val v = meal[nutrient]
            if (v != null) {
                total += v
                counted++
            }
        }
        // No meal had the data — return the count so the caller can say
        // "unknown" rather than showing a confident zero.
        return if (counted == 0) -1.0 else total
    }

    /** Status for a daily total; null when nothing was measurable. */
    fun dailyStatus(total: Double, cap: Double): NutrientCaps.Status? =
        if (total < 0.0) null else NutrientCaps.evaluate(total, cap)

    /** Threshold boundaries are inclusive on the healthy side. */
    fun badges(
        fiberG: Double?,
        sodiumMg: Double?,
        ironMg: Double?,
    ): Set<Badge> = buildSet {
        if (fiberG != null && fiberG >= HIGH_FIBER_AT) add(Badge.HIGH_FIBER)
        if (sodiumMg != null && sodiumMg <= LOW_SODIUM_AT) add(Badge.LOW_SODIUM)
        if (ironMg != null && ironMg >= HIGH_IRON_AT) add(Badge.HIGH_IRON)
    }
}
