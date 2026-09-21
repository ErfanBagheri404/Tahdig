package com.erfanbagheri.tahdig.util

/**
 * Dietary filter presets mapped onto the Farsi tag strings stored in `FoodEntity.tags`.
 *
 * Tags in the seed data are comma-separated Farsi words (e.g. "گوشتی,خورش,سنتی").
 * Matching goes through [PersianText.normalize] so ZWNJ and Arabic letter variants line up.
 */
enum class DietFilter(val label: String, private val tag: String) {
    VEGETARIAN("گیاهی", "گیاهی"),
    VEGAN("وگن", "وگن"),
    NO_GLUTEN("بدون گلوتن", "بدون_گلوتن"),
    LOW_CAL("کم‌کالری", "کم‌کالری"),
    ;

    /** True when [tags] contains this filter's tag. */
    fun matches(tags: String): Boolean {
        val hay = PersianText.normalize(tags.replace('_', ' '))
        return hay.contains(PersianText.normalize(tag.replace('_', ' ')))
    }
}
