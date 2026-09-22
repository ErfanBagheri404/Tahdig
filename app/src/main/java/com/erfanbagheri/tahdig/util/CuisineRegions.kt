package com.erfanbagheri.tahdig.util

/**
 * Cuisine-map registry (#90): the region rows in browse order, and the coverage
 * computation the «۷ از ۴۲ غذا را امتحان کردی» meter reads.
 *
 * Keys match `FoodEntity.cuisine` (seed + scripts/bake_regions.py output).
 * A cuisine missing from here still renders via [labelFor]'s fallback — new seed
 * data must never blank a row.
 */
data class Region(
    val key: String,
    val label: String,
    val emoji: String,
    /** Persian regions first, then the world section — order IS the row order. */
    val section: String,
)

/** Cooked/total for one region. total 0 = empty region (shown as 0, never a crash). */
data class Coverage(val cooked: Int, val total: Int) {
    /** 0.0..1.0 for the meter; 0.0 when total is 0. */
    val fraction: Float get() = if (total == 0) 0f else cooked.toFloat() / total
    val isComplete: Boolean get() = total > 0 && cooked >= total
}

object CuisineRegions {

    const val SECTION_IRAN = "ایران"
    const val SECTION_WORLD = "جهان"

    private val REGIONS = listOf(
        // ── ایران ────────────────────────────────────────────────────────
        Region("IRANI", "ایرانی (سنتی)", "🍲", SECTION_IRAN),
        Region("GILAKI", "گیلان", "🐟", SECTION_IRAN),
        Region("AZERBAIJANI", "آذربایجان", "🍢", SECTION_IRAN),
        Region("ISFAHANI", "اصفهان", "🏛", SECTION_IRAN),
        Region("SHIRAZI", "شیراز", "🌹", SECTION_IRAN),
        Region("YAZDI", "یزد", "🌾", SECTION_IRAN),
        Region("KURDISH", "کردستان", "⛰", SECTION_IRAN),
        Region("KHUZESTANI", "خوزستان", "🌴", SECTION_IRAN),
        Region("KHORASANI", "خراسان", "🕌", SECTION_IRAN),
        Region("MAZANDRANI", "مازندران", "🌲", SECTION_IRAN),
        Region("LURISTAN", "لرستان", "🏔", SECTION_IRAN),
        Region("BALUCHI", "بلوچستان", "🐪", SECTION_IRAN),
        // ── جهان ────────────────────────────────────────────────────────
        Region("INTERNATIONAL", "بین‌المللی", "🌍", SECTION_WORLD),
        Region("ITALIAN", "ایتالیا", "🇮🇹", SECTION_WORLD),
        Region("FRENCH", "فرانسه", "🇫🇷", SECTION_WORLD),
        Region("TURKISH", "ترکیه", "🇹🇷", SECTION_WORLD),
        Region("ARABIC", "عربی", "🕌", SECTION_WORLD),
        Region("GREEK", "یونان", "🇬🇷", SECTION_WORLD),
        Region("INDIAN", "هند", "🇮🇳", SECTION_WORLD),
        Region("CHINESE", "چین", "🇨🇳", SECTION_WORLD),
        Region("JAPANESE", "ژاپن", "🇯🇵", SECTION_WORLD),
        Region("KOREAN", "کره", "🇰🇷", SECTION_WORLD),
        Region("THAI", "تایلند", "🇹🇭", SECTION_WORLD),
        Region("MEXICAN", "مکزیک", "🇲🇽", SECTION_WORLD),
        Region("VIETNAMESE", "ویتنام", "🇻🇳", SECTION_WORLD),
        Region("AMERICAN", "آمریکا", "🇺🇸", SECTION_WORLD),
        Region("BRITISH", "بریتانیا", "🇬🇧", SECTION_WORLD),
        Region("SPANISH", "اسپانیا", "🇪🇸", SECTION_WORLD),
        Region("POLISH", "لهستان", "🇵🇱", SECTION_WORLD),
        Region("HUNGARIAN", "مجارستان", "🇭🇺", SECTION_WORLD),
        Region("RUSSIAN", "روسیه", "🇷🇺", SECTION_WORLD),
    )

    private val BY_KEY = REGIONS.associateBy { it.key }

    /** Registered rows in display order (caller filters to rows with dishes). */
    val all: List<Region> get() = REGIONS

    /** Farsi label for a cuisine key; unknown keys render as themselves, never blank. */
    fun labelFor(key: String): String = BY_KEY[key]?.label ?: key

    fun regionFor(key: String): Region? = BY_KEY[key]

    /**
     * Coverage from history rows (#90 AC): how many of this region's dishes appear
     * in the cooked-id set. Pure set intersection — an untried region yields 0/total,
     * a total of 0 yields 0/0 (fraction 0.0, no division by zero).
     */
    fun coverage(regionDishIds: Set<Long>, cookedIds: Set<Long>): Coverage =
        Coverage(
            cooked = regionDishIds.intersect(cookedIds).size,
            total = regionDishIds.size,
        )
}
