package com.erfanbagheri.tahdig.util

/**
 * Personal nutrient caps and health-condition presets (#113).
 *
 * A cap is a ceiling on one nutrient. Presets are just named bundles of caps,
 * so "custom caps compose with preset" falls out for free: a user cap for the
 * same nutrient overrides the preset's, and caps for other nutrients stack.
 *
 * Pure — no DB, no Android — so the tri-state boundaries are testable.
 */
object NutrientCaps {

    /** Nutrients a cap can be expressed on, with the unit shown in the UI. */
    enum class Nutrient(val label: String, val unit: String) {
        SODIUM("سدیم", "میلی‌گرم"),
        SAT_FAT("چربی اشباع", "گرم"),
        SUGAR("قند", "گرم"),
        CARBS("کربوهیدرات", "گرم"),
        POTASSIUM("پتاسیم", "میلی‌گرم"),
        PHOSPHORUS("فسفر", "میلی‌گرم"),
        PROTEIN("پروتئین", "گرم"),
    }

    /**
     * Health-condition presets. Thresholds are the widely published general
     * guidance for the condition, per serving — deliberately conservative
     * rather than clinical, and each is presented as a personal limit the user
     * can override, never as medical advice.
     */
    enum class Preset(val label: String, val caps: Map<Nutrient, Double>) {
        DIABETES(
            "دیابت",
            mapOf(Nutrient.CARBS to 45.0, Nutrient.SUGAR to 10.0),
        ),
        HYPERTENSION(
            "فشار خون",
            mapOf(Nutrient.SODIUM to 400.0),
        ),
        KIDNEY(
            "کلیه",
            mapOf(
                Nutrient.POTASSIUM to 500.0,
                Nutrient.PHOSPHORUS to 300.0,
                Nutrient.PROTEIN to 20.0,
            ),
        ),
        HEART(
            "قلب",
            mapOf(
                Nutrient.SAT_FAT to 5.0,
                Nutrient.SODIUM to 400.0,
                Nutrient.SUGAR to 10.0,
            ),
        ),
    }

    /** Pass/warn/fail, expressed as a symbol too so colour isn't the only cue. */
    enum class Status(val symbol: String, val label: String) {
        PASS("✓", "در محدوده"),
        WARN("!", "نزدیک سقف"),
        FAIL("×", "بیش از سقف"),
    }

    /** Fraction of the cap at which a dish starts warning. */
    private const val WARN_AT = 0.8

    /**
     * Tri-state evaluation. Boundaries: exactly at the cap is still PASS
     * (a ceiling is "up to and including"), and the warn band starts at
     * [WARN_AT] of it. A zero or negative cap is treated as no cap.
     */
    fun evaluate(amount: Double, cap: Double): Status? {
        if (cap <= 0.0) return null
        val ratio = amount / cap
        return when {
            // Order matters: exactly at the cap is PASS, so the warn band has to
            // stop below 1.0 or "400 of 400" would read as a warning.
            ratio > 1.0 -> Status.FAIL
            ratio >= WARN_AT && ratio < 1.0 -> Status.WARN
            else -> Status.PASS
        }
    }

    /**
     * Merge a preset's caps with the user's own. The user's value wins for the
     * same nutrient; presets never silently overwrite a personal limit.
     */
    fun merge(
        preset: Preset?,
        custom: Map<Nutrient, Double>,
    ): Map<Nutrient, Double> {
        val out = LinkedHashMap<Nutrient, Double>()
        preset?.caps?.forEach { (n, v) -> if (v > 0.0) out[n] = v }
        custom.forEach { (n, v) -> if (v > 0.0) out[n] = v }
        return out
    }

    /**
     * One nutrient's result against the active caps, or null when that
     * nutrient isn't capped. `amount` is per serving.
     */
    data class Check(
        val nutrient: Nutrient,
        val amount: Double,
        val cap: Double,
        val status: Status,
    ) {
        /** «سدیم: ۳۲۰ میلی‌گرم از ۴۰۰ — در محدوده» */
        fun describe(): String =
            "${nutrient.label}: ${fmt(amount)} ${nutrient.unit} از ${fmt(cap)} — ${status.label}"
    }

    /** Evaluate every active cap against a dish's per-serving amounts. */
    fun check(
        caps: Map<Nutrient, Double>,
        amounts: Map<Nutrient, Double>,
    ): List<Check> = caps.mapNotNull { (n, cap) ->
        // No data for this nutrient: nothing to report. Never assume zero —
        // that would read as a comfortable pass.
        val amount = amounts[n] ?: return@mapNotNull null
        evaluate(amount, cap)?.let { Check(n, amount, cap, it) }
    }

    /** True when nothing is over its cap — the «در محدوده من» filter predicate. */
    fun allWithin(caps: Map<Nutrient, Double>, amounts: Map<Nutrient, Double>): Boolean =
        check(caps, amounts).none { it.status == Status.FAIL }

    /** Persian digits, no decimals when the value is whole. */
    private fun fmt(v: Double): String {
        val s = if (v == v.toLong().toDouble()) v.toLong().toString()
        else String.format("%.1f", v)
        return s.map { if (it.isDigit()) '۰' + (it - '0') else it }.joinToString("")
    }
}
