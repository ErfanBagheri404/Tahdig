package com.erfanbagheri.tahdig.util

/**
 * Estimated nutritional info derived from dish name/tags (no DB column).
 * ponytail: rough heuristic per category; add real nutrition table when curated data exists.
 */
object NutritionEstimate {
    data class Info(val calories: Int, val protein: String, val fat: String, val carb: String)

    private val CALORIE_MAP = mapOf(
        "کباب" to 320, "خورشت" to 220, "پلو" to 280, "آش" to 180, "سالاد" to 90,
        "سوپ" to 120, "دسر" to 350, "شیرینی" to 400, "نان" to 250, "ماهی" to 200,
        "املت" to 220, "کوکو" to 250, "کتلت" to 280, "سبزیجات" to 100,
    )

    fun estimate(name: String, tags: String): Info {
        val text = "$name $tags"
        val cal = CALORIE_MAP.entries.find { text.contains(it.key) }?.value ?: 250
        val ratio = when {
            text.contains("سبزی") || text.contains("سالاد") -> 0.1
            text.contains("گوشت") || text.contains("کباب") -> 0.3
            text.contains("دسر") || text.contains("شیرینی") -> 0.05
            else -> 0.15
        }
        return Info(
            calories = cal,
            protein = "${(cal * ratio / 4).toInt()}g",
            fat = "${(cal * 0.35 / 9).toInt()}g",
            carb = "${(cal * (1 - ratio - 0.35) / 4).toInt()}g",
        )
    }
}
