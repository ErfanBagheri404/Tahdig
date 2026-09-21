package com.erfanbagheri.tahdig.util

/**
 * Scores a dish against the ingredients the user already has at home (pantry).
 *
 * A dish is "cookable now" when every one of its ingredients is covered by the
 * pantry. Coverage is substring-based on normalized text (same rule as the search
 * ingredient filter) so "پیاز" covers "پیاز داغ" and ZWNJ/Kaf variants match.
 *
 * ponytail: substring matching, no synonym table, so "گوشت" does not cover
 * "گوشت گوسفندی" — the score just drops. Add a synonym map if users report
 * near-misses.
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

    /**
     * Fraction of [ingredients] covered by [pantry], 0.0..1.0.
     * A dish with no listed ingredients counts as fully covered.
     */
    fun coverage(ingredients: String, pantry: Collection<String>): Float {
        val need = ingredients.split(',', '،').map { key(it) }.filter { it.isNotEmpty() }
        if (need.isEmpty()) return 1f
        val have = pantry.flatMap { it.split(',', '،') }.map { key(it) }.filter { it.isNotEmpty() }
        if (have.isEmpty()) return 0f
        val covered = need.count { n -> have.any { h -> n.contains(h) || h.contains(n) } }
        return covered.toFloat() / need.size
    }

    fun isCookable(ingredients: String, pantry: Collection<String>): Boolean =
        coverage(ingredients, pantry) >= 1f
}
