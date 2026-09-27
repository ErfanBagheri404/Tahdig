package com.erfanbagheri.tahdig.util

import android.content.res.AssetManager

/**
 * Per-ingredient nutrition from USDA SR Legacy (public domain), loaded once at
 * seed time from assets/seed/nutrition.json. Keyed by ingredient name, with
 * English aliases alongside the Farsi ones so the English-titled dishes resolve
 * too.
 *
 * ponytail: serving-size approximation — the source is per-100g, so per-dish
 * requires knowing how many grams of each ingredient go into the dish. The seed
 * only has free-text quantities ("۲ پیمانه"), not grams, so the sum is rough. A
 * proper solution needs a weights table or user-entered grams.
 */
object NutritionDB {
    data class Entry(
        val calories: Int,
        val protein: Double,
        val fat: Double,
        val carbs: Double,
        /** Per-100g fields the full label (#111) needs; 0 when the source lacks them. */
        val sugars: Double = 0.0,
        val saturatedFat: Double = 0.0,
        val fiber: Double = 0.0,
        val salt: Double = 0.0,
        /** kJ per 100g — Nutri-Score's energy input, never derived from kcal. */
        val energyKj: Double = 0.0,
    )

    private var data: Map<String, Entry> = emptyMap()
    private var loaded = false

    /** Must be called once at app start — fast, <1ms for ~40 entries. */
    fun load(assets: AssetManager) {
        if (loaded) return
        loaded = true
        data = try {
            val raw = assets.open("seed/nutrition.json").bufferedReader().readText()
            org.json.JSONObject(raw).let { obj ->
                obj.keys().asSequence().associateWith { key ->
                    val e = obj.getJSONObject(key)
                    Entry(
                        calories = e.optInt("calories", 0),
                        protein = e.optDouble("protein", 0.0),
                        fat = e.optDouble("fat", 0.0),
                        carbs = e.optDouble("carbs", 0.0),
                        sugars = e.optDouble("sugars", 0.0),
                        saturatedFat = e.optDouble("sat_fat", 0.0),
                        fiber = e.optDouble("fiber", 0.0),
                        salt = e.optDouble("salt", 0.0),
                        // Older rows have no kJ; fall back to the kcal conversion
                        // so a partially-baked entry still scores.
                        energyKj = if (e.has("kj")) e.optDouble("kj", 0.0)
                        else e.optDouble("calories", 0.0) * 4.184,
                    )
                }
            }
        } catch (_: Exception) { emptyMap() }
    }

    /**
     * Leading quantity/unit noise in the seed text: "400ml double cream",
     * "۲ پیمانه آرد", "1/2 cup sugar". Stripped before lookup so the alias
     * itself never has to encode amounts.
     */
    private val LEADING = Regex(
        "^\\s*[0-9\\u06F0-\\u06F9\\u0660-\\u0669/.,\\u066B\\u066C-]+\\s*" +
            // Word-bounded unit, so the litre unit cannot swallow the "l" of
            // "large onion" and leave "arge onion" behind.
            "((?:g|gr|grams?|kg|ml|litres?|liters?|tbsp|tbs|tblsp|tablespoons?|" +
            "teaspoons?|tsp|cups?|oz|lb|lbs|cloves?|cans?|slices?|pinch(?:es)?|" +
            "bunch(?:es)?|handfuls?|sprigs?|sticks?|pieces?|packets?|packs?|" +
            "پیمانه|قاشق|گرم|کیلو|لیتر|عدد|حبه|ورق|برگ|بسته|دانه|سر)" +
            // Unicode lookahead, not \\b: Java's word boundary is ASCII-only, so
            // it never fires after a Persian unit like «پیمانه».
            "(?![\\p{L}\\p{N}]))?\\s*",
        RegexOption.IGNORE_CASE,
    )

    /** Trailing qualifiers that never change the nutrition row. */
    private val TRAILING = Regex(
        "\\s*(to taste|chopped|sliced|diced|minced|crushed|peeled|grated|" +
            "beaten|melted|softened|finely|roughly|fresh|frozen|tinned|canned|" +
            "cooked|uncooked|raw|large|small|medium|whole|extra|virgin|plain|" +
            "unsalted|salted|optional|for garnish|plus extra)\\s*",
        RegexOption.IGNORE_CASE,
    )

    /** Pure normalisation — no lookups, so the tests can pin it directly. */
    fun normalize(raw: String): String {
        var s = raw.trim().lowercase()
        s = LEADING.replace(s, "")
        s = TRAILING.replace(s, " ")
        s = s.replace(Regex("\\s+"), " ").trim()
        return s
    }

    /**
     * Resolve one ingredient. Exact, then normalised, then the longest alias
     * that appears on a word boundary — longest first so «گوشت گاو» beats
     * «گوشت», and word-bounded so "cream" never matches "creamed coconut".
     */
    fun get(ingredient: String): Entry? {
        val raw = ingredient.trim().lowercase()
        if (raw.isEmpty()) return null
        data[raw]?.let { return it }

        val norm = normalize(ingredient)
        if (norm.isNotEmpty()) data[norm]?.let { return it }

        for (probe in listOf(norm, raw)) {
            if (probe.isEmpty()) continue
            var best: Pair<String, Entry>? = null
            for ((key, entry) in data) {
                if (key.length < 3) continue
                if (!Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(key) + "(?![\\p{L}\\p{N}])")
                        .containsMatchIn(probe)
                ) continue
                if (best == null || key.length > best!!.first.length) best = key to entry
            }
            if (best != null) return best!!.second
        }
        return null
    }

    fun has(ingredient: String): Boolean = get(ingredient) != null

    /** Number of ingredients with real data, for diagnostics. */
    fun coverage(): Int = data.size

    // ── test seams ─────────────────────────────────────────────────
    // The label's estimate/real split is decided entirely by what this
    // registry holds, so the tests need to install a known fixture.

    /**
     * Replace the table. Test-only, and deliberately typed rather than
     * JSON-parsed: `org.json` is a stub in unit tests, so a JSON seam would
     * silently install nothing.
     */
    fun installEntries(entries: Map<String, Entry>) {
        data = entries
        loaded = true
    }

    /** Empty the table and clear the loaded flag. Test-only. */
    fun reset() {
        data = emptyMap()
        loaded = false
    }
}
