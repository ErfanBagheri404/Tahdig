package com.erfanbagheri.tahdig.util

/**
 * Taste axes (#89): the five «امروز چه مزه‌ای؟» filters.
 * Stored in the seed as enum names (TURSH…), rendered via [label].
 */
enum class Flavor(val label: String) {
    TURSH("ترش"),
    SHIRIN("شیرین"),
    TALKH("تلخ"),
    TOND("تند"),
    CHORB("چرب"),
}

/**
 * Deterministic taste tagging from ingredient/tag text + category (#89).
 *
 * Pure keyword rules — no I/O, same input always yields the same set.
 * The SAME tables live in `scripts/bake_flavor.py`, which bakes the per-dish
 * result into `seed/flavor.json` at authoring time (the issue wants tags baked,
 * not computed per launch); this object is the tested reference implementation
 * and the runtime fallback for dishes added without a bake (#73 custom recipes).
 * Keep the two tables in sync.
 */
object FlavorTagger {

    /** Sour markers — the ترش axis. */
    private val SOUR = listOf("لیمو", "سماق", "انار", "آبغوره", "غوره")

    /** Sweet markers. One hit on a savory dish is normal (a pinch of شکر), hence the gate below. */
    private val SWEET = listOf(
        "شیرینی", "شکر", "عسل", "گلاب", "مربا", "ژله", "بستنی",
        "حلوا", "فرنی", "کیک", "پودینگ", "دسر",
    )

    /** Bitter markers. */
    private val BITTER = listOf("قهوه", "کاکائو", "نسکافه", "کنگر", "کاسنی")

    /** Greasy/rich markers — the چرب axis. Conservative list; ordinary روغن is in every Persian dish. */
    private val GREASY = listOf("کره", "سوخاری", "سرخ کردن", "سرخ شده", "سرخ کرده", "چرب")

    /**
     * Sweet categories (ids 19 حلوا و دسر / 20 شیرینی / 21 بستنی و فالوده — by name so
     * callers that only know the label still work). A lemon tart is شیرین, never ترش.
     */
    private val SWEET_CATEGORIES = listOf("دسر", "شیرینی", "بستنی", "حلوا")

    /** Taste tags for one dish. */
    fun tag(
        ingredients: String,
        tags: String = "",
        name: String = "",
        categoryName: String = "",
    ): Set<Flavor> {
        val text = PersianText.normalize("$ingredients $tags $name")

        fun has(words: List<String>) = words.any { text.contains(PersianText.normalize(it)) }

        val categoryIsSweet = SWEET_CATEGORIES.any { categoryName.contains(it) }
        val hasSour = has(SOUR)
        val hasSweet = has(SWEET)

        // Sweets first — same precedence as SpiceProfile: a sweet dish with a sour
        // note (لیمو روی کیک) is still a sweet dish and must never be tagged ترش.
        if (categoryIsSweet || (hasSweet && !hasSour)) {
            val out = mutableSetOf(Flavor.SHIRIN)
            if (has(BITTER)) out += Flavor.TALKH
            if (has(GREASY)) out += Flavor.CHORB
            return out
        }

        val out = mutableSetOf<Flavor>()
        if (hasSour) out += Flavor.TURSH
        // Symmetric with the sweet gate above: on a savory dish a spoon of شکر does
        // not make it a sweet dish, so شیرین needs a sweet marker and NO sour one.
        if (hasSweet && !hasSour) out += Flavor.SHIRIN
        if (has(BITTER)) out += Flavor.TALKH
        // TOND delegates to the existing spice core — one spice answer, two surfaces.
        if (SpiceProfile.level(tags, ingredients, name) >= 2) out += Flavor.TOND
        if (has(GREASY)) out += Flavor.CHORB
        return out
    }
}
