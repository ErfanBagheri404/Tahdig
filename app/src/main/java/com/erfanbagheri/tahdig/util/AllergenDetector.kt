package com.erfanbagheri.tahdig.util

/**
 * Ingredient-level allergen detection (#112).
 *
 * Two sources, both conservative — an unknown ingredient produces NO warning:
 * 1. The seed's per-ingredient `allergen` field, reached through
 *    [IngredientRegistry.resolve] on the parsed item (quantity stripped).
 * 2. A small derivative map for free-text lines the registry doesn't know
 *    (مالت، دکسترین → گلوتن), because those are exactly the hidden cases.
 *
 * Profile options mirror the seed's own taxonomy — no invented categories.
 */
object AllergenDetector {

    /** The seed's allergen values, in display order. */
    val PROFILE_OPTIONS = listOf(
        "لبنیات", "گلوتن", "مغزها", "تخم‌مرغ", "ماهی", "صدف", "سویا", "کنجد",
    )

    /**
     * Free-text derivatives → canonical allergen. Matched as normalized
     * substrings against the whole ingredient line; only cases where the
     * word IS the allergen (never a cross-contamination maybe).
     */
    private val DERIVATIVES: List<Pair<String, List<String>>> = listOf(
        "گلوتن" to listOf("مالت", "دکسترین", "آرد گندم", "گلوتن", "سمولینا"),
        "لبنیات" to listOf("کازئین", "شیرخشک", "لکتوز", "whey"),
        "تخم‌مرغ" to listOf("آلبومین تخم‌مرغ", "زرده و سفیده"),
        "سویا" to listOf("لسیتین سویا", "پروتئین سویا"),
        "مغزها" to listOf("کره بادام‌زمینی", "کره گردو"),
    )

    /** Allergens detected in one dish's ingredient text. Empty = no data. */
    fun detect(ingredients: String): Set<String> {
        if (ingredients.isBlank()) return emptySet()
        val hits = mutableSetOf<String>()
        for (line in ingredients.split(',', '،')) {
            val item = IngredientParser.parse(line).item
            if (item.isBlank()) continue
            resolveItem(item)?.allergen?.let { hits += it }
            // Seed lines sometimes join items with «و» («نمک و فلفل») — each
            // side may carry its own allergen, so resolve them apart too.
            if (item.contains(" و ")) {
                for (part in item.split(" و ")) {
                    resolveItem(part)?.allergen?.let { hits += it }
                }
            }
        }
        val norm = PersianText.normalize(ingredients)
        for ((allergen, words) in DERIVATIVES) {
            if (words.any { norm.contains(PersianText.normalize(it)) }) hits += allergen
        }
        return hits
    }

    /** Amounts glued to the value, e.g. "400ml double cream" → "double cream". */
    private val AMOUNT = Regex("""^\s*[0-9\u0660-\u0669\u06F0-\u06F9\u0660-\u0669]+(?:\.[0-9]+)?\s*[A-Za-z]*\s*""")

    /**
     * Resolve with one retry after stripping a leading amount. Imported
     * TheMealDB dishes store English lines ("400ml double cream") where the
     * number is glued to the unit, which neither the Persian parser nor an
     * exact alias match would ever see — and they carry the real allergens.
     * A failure stays a miss: conservative by design.
     */
    private fun resolveItem(item: String): IngredientRegistry.Ingredient? {
        IngredientRegistry.resolve(item)?.let { return it }
        val stripped = PersianText.toAsciiDigits(item).replaceFirst(AMOUNT, "").trim()
        if (stripped.isEmpty() || stripped == item) return null
        return IngredientRegistry.resolve(stripped)
    }

    /** Profile conflicts with the dish — drives the detail warning band. */
    fun conflicts(profile: Set<String>, dishAllergens: Set<String>): Boolean =
        profile.isNotEmpty() && dishAllergens.any { it in profile }

    /** True when the dish must be hidden search-wide (hide toggle on). */
    fun shouldHide(hideEnabled: Boolean, profile: Set<String>, dishAllergens: Set<String>): Boolean =
        hideEnabled && conflicts(profile, dishAllergens)
}
