package com.erfanbagheri.tahdig.ui.screen

import com.erfanbagheri.tahdig.util.NutriLabel
import com.erfanbagheri.tahdig.util.NutritionDB
import com.erfanbagheri.tahdig.util.NutritionEstimate

/**
 * Everything the label panel renders (#111), already resolved.
 *
 * The estimate/real split lives HERE, not in the composable: `score` and
 * `nova` stay null unless every Nutri-Score input came from real per-100g
 * data, which is what keeps an estimate from displaying a grade it hasn't
 * earned. Pure — testable without a device.
 */
data class NutritionLabelData(
    val calories: Int,
    val proteinG: Double,
    val fatG: Double,
    val carbG: Double,
    val sugarG: Double,
    val satFatG: Double,
    val fiberG: Double,
    val saltG: Double,
    /** Nutri-Score grade, or null when the data can't support one. */
    val score: NutriLabel.Score?,
    /** NOVA group, or null on an estimate. */
    val nova: Int?,
    val estimated: Boolean,
) {
    companion object {

        /** Ingredients that must resolve before a grade is honest. */
        private const val MIN_COVERED = 2

        /**
         * Build from a dish. Real per-100g data when enough ingredients are
         * covered — and only then are Nutri-Score and NOVA computed. Otherwise
         * the per-category heuristic fills the table with `estimated = true`
         * and no badges.
         */
        fun of(name: String, tags: String, ingredients: String): NutritionLabelData {
            val parts = ingredients.split(',', '،')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val entries = parts.mapNotNull { NutritionDB.get(it) }

            if (entries.size >= MIN_COVERED) {
                // Equal 100g shares — the seed has no gram amounts, so this is
                // the same documented approximation NutritionEstimate makes.
                var cal = 0.0; var pro = 0.0; var fat = 0.0; var carb = 0.0
                var sugar = 0.0; var satFat = 0.0; var fiber = 0.0; var salt = 0.0
                var kj = 0.0
                for (e in entries) {
                    cal += e.calories; pro += e.protein; fat += e.fat; carb += e.carbs
                    sugar += e.sugars; satFat += e.saturatedFat
                    fiber += e.fiber; salt += e.salt; kj += e.energyKj
                }

                // A grade needs the inputs to actually be present. All-zero
                // sugars/sat-fat/salt across every ingredient means the source
                // lacked those columns — scoring then would read as "A" for
                // anything, which is a lie dressed as a badge.
                val hasScoreInputs = entries.any {
                    it.sugars > 0.0 || it.saturatedFat > 0.0 || it.salt > 0.0
                }
                val score = if (hasScoreInputs) {
                    NutriLabel.nutriScore(
                        NutriLabel.Facts(
                            energyKj = kj,
                            sugarsG = sugar,
                            saturatedFatG = satFat,
                            saltG = salt,
                            fiberG = fiber,
                            proteinG = pro,
                        ),
                    )
                } else null

                return NutritionLabelData(
                    calories = cal.toInt(),
                    proteinG = pro, fatG = fat, carbG = carb,
                    sugarG = sugar, satFatG = satFat, fiberG = fiber, saltG = salt,
                    score = score,
                    // NOVA is a property of the ingredient LIST, so it is
                    // available whenever the ingredient text is.
                    nova = NutriLabel.nova(ingredients),
                    estimated = false,
                )
            }

            val est = NutritionEstimate.estimate(name, tags)
            return NutritionLabelData(
                calories = est.calories,
                proteinG = est.protein.removeSuffix("g").toDoubleOrNull() ?: 0.0,
                fatG = est.fat.removeSuffix("g").toDoubleOrNull() ?: 0.0,
                carbG = est.carb.removeSuffix("g").toDoubleOrNull() ?: 0.0,
                sugarG = 0.0, satFatG = 0.0, fiberG = 0.0, saltG = 0.0,
                score = null,
                nova = null,
                estimated = true,
            )
        }
    }
}
