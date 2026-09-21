package com.erfanbagheri.tahdig.util

import android.content.res.AssetManager

/**
 * Per-ingredient nutrition from Open Food Facts, loaded once at seed time from
 * assets/seed/nutrition.json. Keyed by Farsi ingredient name, matching the seed
 * format exactly.
 *
 * ponytail: serving-size approximation — OFF gives per-100g, so per-dish requires
 * knowing how many grams of each ingredient go into the dish. The seed only has
 * free-text quantities ("۲ پیمانه"), not grams, so the sum is rough. A proper
 * solution needs a weights table or user-entered grams.
 */
object NutritionDB {
    data class Entry(val calories: Int, val protein: Double, val fat: Double, val carbs: Double)

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
                    )
                }
            }
        } catch (_: Exception) { emptyMap() }
    }

    fun get(ingredient: String): Entry? = data[ingredient]

    fun has(ingredient: String): Boolean = data.containsKey(ingredient)

    /** Number of ingredients with OFF data, for diagnostics. */
    fun coverage(): Int = data.size
}
