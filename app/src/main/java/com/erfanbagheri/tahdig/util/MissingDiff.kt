package com.erfanbagheri.tahdig.util

/**
 * "What do I still need to buy?" — the diff between a dish's ingredients and the pantry.
 *
 * Generating a shopping list from a recipe should not ask the user to buy onion they
 * already have. This computes the gap, and it counts an approved substitute as covered:
 * if the dish wants ماست چکیده and the pantry holds ماست, the need is satisfied.
 *
 * Ingredients are matched on canonical ids via [IngredientRegistry]; anything the
 * glossary doesn't know falls back to normalized text, so an unrecognized ingredient is
 * reported as missing rather than silently dropped (the honest direction).
 *
 * ponytail: no quantities. The seed stores free-text amounts ("۲ پیمانه") with no gram
 * basis, so "you have 500g and need 300g" cannot be computed — presence is the only real
 * signal available. Add quantity-aware diffing when the glossary's density data is
 * actually consumed by a weights path.
 */
object MissingDiff {

    /** One ingredient the dish needs, and whether the pantry covers it. */
    data class Need(
        val key: String,
        val display: String,
        val canonicalId: String?,
        val covered: Boolean,
        /** When covered by a swap rather than directly: the replacement's Farsi name. */
        val viaSubstitute: String? = null,
    ) {
        val missing: Boolean get() = !covered
    }

    /** Result of a diff: everything needed, plus the missing subset for convenience. */
    data class Result(val needs: List<Need>) {
        val missing: List<Need> get() = needs.filter { it.missing }
        val covered: List<Need> get() = needs.filter { it.covered }
        val allCovered: Boolean get() = missing.isEmpty()

        /** «۲ ماده کم داری: زعفران، کشمش» — the one-line summary the detail screen shows. */
        fun summary(): String {
            if (needs.isEmpty()) return "چیزی لازم نیست"
            if (missing.isEmpty()) return "همه‌چیز را داری"
            val names = missing.take(3).joinToString("، ") { it.display }
            val more = if (missing.size > 3) " و ${missing.size - 3} مورد دیگر" else ""
            return "${PersianText.toPersianDigits(missing.size.toString())} ماده کم داری: $names$more"
        }
    }

    /**
     * Split a seed ingredient blob ("۲ پیمانه آرد، پیاز، ۱ حبه سیر") into display items.
     * Reuses the shopping parser so the two never disagree about what a line means.
     */
    fun itemsOf(ingredients: String): List<String> =
        ingredients.split(',', '،')
            .map { IngredientParser.parse(it.trim()).item.ifBlank { it.trim() } }
            .filter { it.isNotEmpty() }

    /**
     * Diff [ingredients] against [pantry]. Swaps count when the pantry holds the
     * replacement.
     *
     * @param pantry raw pantry strings, as stored.
     * @param allowSubstitutes when false the diff is strict (used by tests and by any
     *   caller that wants the literal answer).
     */
    fun diff(
        ingredients: String,
        pantry: Collection<String>,
        allowSubstitutes: Boolean = true,
    ): Result {
        val items = itemsOf(ingredients)

        // Canonical ids the pantry covers, plus its unknown-but-present text.
        val pantryIds = mutableSetOf<String>()
        val pantryText = mutableSetOf<String>()
        for (p in pantry) {
            for (part in p.split(',', '،')) {
                val raw = part.trim()
                if (raw.isEmpty()) continue
                // Pantry rows are display text ("۲ عدد پیاز قرمز") — strip quantity/unit
                // before resolving, or the digits defeat the alias lookup.
                val ing = IngredientRegistry.resolve(IngredientParser.parse(raw).item)
                if (ing != null) pantryIds += ing.id else pantryText += normKey(raw)
            }
        }

        val needs = items.map { item ->
            val ing = IngredientRegistry.resolve(item)
            when {
                ing != null && ing.id in pantryIds ->
                    Need("id:" + ing.id, ing.fa, ing.id, covered = true)

                ing != null && allowSubstitutes -> {
                    // Look for a swap the pantry actually holds. First hit wins; the
                    // tables are curated so the first entry is the best replacement.
                    val hit = swapsHeldBy(ing.id, pantryIds)
                    if (hit != null) Need("id:" + ing.id, ing.fa, ing.id, covered = true, viaSubstitute = hit)
                    else Need("id:" + ing.id, ing.fa, ing.id, covered = false)
                }

                ing != null -> Need("id:" + ing.id, ing.fa, ing.id, covered = false)

                // Unknown ingredient: match on text so a literal pantry entry still counts.
                pantryText.any { it == normKey(item) || it.contains(normKey(item)) || normKey(item).contains(it) } ->
                    Need("txt:" + normKey(item), item, null, covered = true)

                else -> Need("txt:" + normKey(item), item, null, covered = false)
            }
        }
        return Result(needs)
    }

    /** Name of the pantry-held substitute for [missingId], or null. */
    private fun swapsHeldBy(missingId: String, pantryIds: Set<String>): String? =
        SubstitutionRegistry.swapsFor(missingId)
            .firstOrNull { it.toId in pantryIds }
            ?.toName

    private fun normKey(raw: String): String =
        PersianText.normalize(raw).filterNot { it.isWhitespace() }
}
