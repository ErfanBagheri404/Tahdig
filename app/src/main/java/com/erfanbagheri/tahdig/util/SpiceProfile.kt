package com.erfanbagheri.tahdig.util

/**
 * Derives a spice level (0-3) from a dish's tags/ingredients text.
 * ponytail: keyword heuristic, no per-dish DB column — add `spice_level` when curated data exists.
 */
object SpiceProfile {
    private val HOT = listOf("فلفل", "تند", "چلی", "فلفل قرمز", "شور", "پیازچه")
    private val MILD = listOf("دسر", "شیرین", "حلوا", "فرنی", "شله", "کرم", "بستنی")

    /** 0=not spicy, 1=mild, 2=medium, 3=hot */
    fun level(tags: String, ingredients: String, name: String): Int {
        val text = "$tags $ingredients $name"
        if (HOT.any { text.contains(it) }) {
            return when {
                text.contains("فلفل قرمز") || text.contains("تند") -> 3
                text.contains("فلفل") -> 2
                else -> 1
            }
        }
        if (text.contains("دسر") || text.contains("شیرینی")) return 0
        return 1
    }

    fun label(level: Int): String = when (level) {
        0 -> "بدون تندی 🍼"
        1 -> "کم‌تندی 🌶"
        2 -> "متوسط 🌶🌶"
        else -> "تند 🌶🌶🌶"
    }
}
