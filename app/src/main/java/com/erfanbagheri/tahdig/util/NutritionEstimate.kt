package com.erfanbagheri.tahdig.util

/**
 * Estimated nutritional info derived from dish name/tags (no DB column).
 * ponytail: rough heuristic per category; add real nutrition table when curated data exists.
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
        val text = "$name $tags"
        // Longest keyword first so "فلفل قرمز"-style compounds can't shadow a more specific dish.
        val macro = MACROS.entries
            .filter { text.contains(it.key) }
            .maxByOrNull { it.key.length }
            ?.value ?: FALLBACK

        // 4 kcal/g protein & carbs, 9 kcal/g fat. Ratios always sum to <= 1.
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
}
