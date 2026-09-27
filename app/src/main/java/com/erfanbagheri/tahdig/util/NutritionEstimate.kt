package com.erfanbagheri.tahdig.util

/**
 * Estimated nutritional info for a dish. Two tiers:
 *
 * 1. REAL — the dish's ingredients are covered by [NutritionDB] (USDA SR Legacy,
 *    per-100g). Summed over the dish's ingredient list. Badge: "واقعی".
 * 2. Fallback — the per-category heuristic in [estimateHeuristic]. Badge: "تخمینی".
 *
 * ponytail: the source gives per-100g but the seed has no gram amounts per ingredient,
 * so the "real" figure assumes equal 100g shares of every listed ingredient.
 * A gram-weights table would make this exact; add one when the data exists.
 */
object NutritionEstimate {
    data class Info(val calories: Int, val protein: String, val fat: String, val carb: String)

    /** (calories, protein ratio, fat ratio) — carb ratio is the remainder. */
    private data class Macro(val cal: Int, val protein: Double, val fat: Double)

    private val MACROS = mapOf(
        "کباب" to Macro(320, 0.30, 0.45),
        "خورشت" to Macro(220, 0.15, 0.40),
        "پلو" to Macro(280, 0.08, 0.20),
        "آش" to Macro(180, 0.15, 0.25),
        "سالاد" to Macro(90, 0.10, 0.40),
        "سوپ" to Macro(120, 0.15, 0.30),
        "دسر" to Macro(350, 0.05, 0.40),
        "شیرینی" to Macro(400, 0.05, 0.45),
        "نان" to Macro(250, 0.12, 0.15),
        "ماهی" to Macro(200, 0.45, 0.25),
        "املت" to Macro(220, 0.20, 0.55),
        "کوکو" to Macro(250, 0.20, 0.50),
        "کتلت" to Macro(280, 0.22, 0.45),
        "سبزیجات" to Macro(100, 0.15, 0.15),
    )
    private val FALLBACK = Macro(250, 0.15, 0.35)

    fun estimate(name: String, tags: String): Info {
        // Name wins over tags: "سالاد شیرازی" tagged سبزیجات is still a 90 kcal salad.
        val macro = bestMatch(name) ?: bestMatch(tags) ?: FALLBACK

        // 4 kcal/g protein & carbs, 9 kcal/g fat.
        val protein = macro.cal * macro.protein / 4
        val fat = macro.cal * macro.fat / 9
        val carb = (macro.cal * (1 - macro.protein - macro.fat) / 4).coerceAtLeast(0.0)

        return Info(
            calories = macro.cal,
            protein = "${protein.toInt()}g",
            fat = "${fat.toInt()}g",
            carb = "${carb.toInt()}g",
        )
    }

    data class Real(val info: Info, val covered: Int, val total: Int)

    /**
     * Sum real macros over the dish's ingredient list (each as its 100g share).
     * Returns null when fewer than [minCovered] ingredients have real data —
     * then the caller falls back to the heuristic with the "تخمینی" badge.
     */
    fun estimateFromIngredients(ingredients: String, minCovered: Int = 2): Real? {
        val parts = ingredients.split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }
        val covered = parts.mapNotNull { NutritionDB.get(it) }
        if (covered.size < minCovered) return null

        var cal = 0.0; var pro = 0.0; var fat = 0.0; var carb = 0.0
        for (e in covered) {
            cal += e.calories; pro += e.protein; fat += e.fat; carb += e.carbs
        }
        return Real(
            Info(
                calories = cal.toInt(),
                protein = "${pro.toInt()}g",
                fat = "${fat.toInt()}g",
                carb = "${carb.toInt()}g",
            ),
            covered = covered.size,
            total = covered.size,
        )
    }

    /** Longest matching keyword wins, so a specific term can't be shadowed by a generic one. */
    private fun bestMatch(text: String): Macro? =
        MACROS.entries
            .filter { text.contains(it.key) }
            .maxByOrNull { it.key.length }
            ?.value
}
