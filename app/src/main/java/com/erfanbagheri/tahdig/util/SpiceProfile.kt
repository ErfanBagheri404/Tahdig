package com.erfanbagheri.tahdig.util

/**
 * Derives a spice level (0-3) from a dish's tags/ingredients text.
 * ponytail: keyword heuristic, no per-dish DB column — add `spice_level` when curated data exists.
 */
object SpiceProfile {
    /** Words that mean "this dish is spicy". Note: شور (salty) is a taste, not spiciness. */
    private val HOT = listOf("فلفل", "تند", "چلی", "هریسا", "پیازچه")

    /** Words that mean "definitely not spicy" (sweets). */
    private val MILD = listOf("دسر", "شیرینی", "حلوا", "فرنی", "شله", "بستنی")

    /** 0=not spicy, 1=mild, 2=medium, 3=hot */
    fun level(tags: String, ingredients: String, name: String): Int {
        val text = "$tags $ingredients $name"

        // Sweets first: a dessert listing فلفل as a garnish is still not a spicy dish,
        // and this keeps MILD from being dead code.
        if (MILD.any { text.contains(it) }) return 0

        if (HOT.any { text.contains(it) }) {
            return when {
                text.contains("فلفل قرمز") || text.contains("تند") || text.contains("هریسا") -> 3
                text.contains("فلفل") || text.contains("چلی") -> 2
                else -> 1
            }
        }
        return 1
    }

    fun label(level: Int): String = when (level) {
        0 -> "بدون تندی 🍼"
        1 -> "کم‌تندی 🌶"
        2 -> "متوسط 🌶🌶"
        else -> "تند 🌶🌶🌶"
    }
}
