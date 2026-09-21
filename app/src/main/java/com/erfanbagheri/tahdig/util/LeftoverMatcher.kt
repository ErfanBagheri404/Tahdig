package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.FoodEntity

/**
 * Finds dishes that overlap with a cooked dish's ingredients — for the "what
 * can I do with the leftovers?" prompt.
 *
 * A candidate shares an ingredient when its normalized, whitespace-stripped text
 * contains (or is contained by) the cooked dish's ingredient. That is the same
 * rule as [PantryMatcher], intentionally so — both reason about Persian
 * ingredient strings the same way.
 */
object LeftoverMatcher {

    private fun key(raw: String): String =
        PersianText.normalize(raw).filterNot { it.isWhitespace() }

    private fun keys(ingredients: String): Set<String> =
        ingredients.split(',', '،').map { key(it) }.filter { it.isNotEmpty() }.toSet()

    /** Number of [a]'s ingredients that [b] also uses. */
    fun countShared(a: FoodEntity, b: FoodEntity): Int {
        val aKeys = keys(a.ingredients)
        val bKeys = keys(b.ingredients)
        return aKeys.count { c -> bKeys.any { t -> c.contains(t) || t.contains(c) } }
    }

    /**
     * Dishes sharing at least [minShared] ingredients with [cooked], most overlap
     * first. Scans the full table in memory — 543 rows and ~2000 ingredient tokens
     * stays comfortably fast without an index.
     */
    fun findLeftovers(
        cooked: FoodEntity,
        allFoods: List<FoodEntity>,
        minShared: Int = 2,
        limit: Int = 10,
    ): List<FoodEntity> {
        if (keys(cooked.ingredients).size < minShared) return emptyList()
        return allFoods
            .filter { it.id != cooked.id }
            .map { it to countShared(cooked, it) }
            .filter { it.second >= minShared }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}
