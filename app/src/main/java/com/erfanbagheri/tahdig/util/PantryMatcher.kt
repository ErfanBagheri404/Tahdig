package com.erfanbagheri.tahdig.util

/**
 * Scores a dish against the ingredients the user already has at home (pantry).
 *
 * A dish is "cookable now" when every one of its ingredients is covered by the
 * pantry. Coverage is substring-based on normalized text (same rule as the search
 * ingredient filter) so "پیاز" covers "پیاز داغ" and ZWNJ/Kaf variants match.
 *
 * Canonical ids are preferred when the glossary knows both sides, and an ingredient
 * the user lacks still counts as covered when the pantry holds a registered substitute
 * — having ماست satisfies a need for ماست چکیده. That expansion is what makes the
 * coverage number credible instead of demanding an exact shopping match.
 *
 * ponytail: substring matching plus the one-hop swap table, no synonym graph. Add
 * transitive swaps only if second-order misses get reported.
 */
object PantryMatcher {

    /**
     * Comparison key: normalized with ALL whitespace removed.
     *
     * [PersianText.normalize] strips ZWNJ but keeps the space it often stands for,
     * so "قورمه‌سبزی" normalizes to one word while "قورمه سبزی" stays two. Dropping
     * spaces entirely makes those compare equal, which is what an ingredient match
     * needs ("لوبیا سبز" vs "لوبیاسبز").
     */
    private fun key(raw: String): String = PersianText.normalize(raw).filterNot { it.isWhitespace() }

    /** Canonical ids the pantry literally holds, tolerating display rows with quantities. */
    private fun pantryIds(pantry: Collection<String>): Set<String> =
        pantry.flatMap { it.split(',', '،') }
            .map { IngredientParser.parse(it.trim()).item }
            .filter { it.isNotBlank() }
            .mapNotNull { IngredientRegistry.resolve(it)?.id }
            .toSet()

    /**
     * Fraction of [ingredients] covered by [pantry], 0.0..1.0.
     * A dish with no listed ingredients counts as fully covered.
     */
    fun coverage(ingredients: String, pantry: Collection<String>): Float {
        val parts = ingredients.split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return 1f
        val have = pantry.flatMap { it.split(',', '،') }.map { key(it) }.filter { it.isNotEmpty() }
        if (have.isEmpty()) return 0f

        val heldIds = pantryIds(pantry)
        val covered = parts.count { part ->
            val ing = IngredientRegistry.resolve(part)
            when {
                ing != null && ing.id in heldIds -> true
                // The pantry holds something we can substitute for what's missing.
                ing != null && heldIds.any { held ->
                    SubstitutionRegistry.swapsFor(ing.id).any { it.toId == held }
                } -> true
                else -> {
                    val n = key(part)
                    have.any { h -> n.contains(h) || h.contains(n) }
                }
            }
        }
        return covered.toFloat() / parts.size
    }

    fun isCookable(ingredients: String, pantry: Collection<String>): Boolean =
        coverage(ingredients, pantry) >= 1f

    /** Ingredients the pantry cannot cover, in list order. Empty means fully cookable. */
    fun missing(ingredients: String, pantry: Collection<String>): List<String> =
        MissingDiff.diff(ingredients, pantry).missing.map { it.display }
}
