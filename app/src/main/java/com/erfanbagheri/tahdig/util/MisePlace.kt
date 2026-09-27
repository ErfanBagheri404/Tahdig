package com.erfanbagheri.tahdig.util

/**
 * Mise-en-place checklist identity (#99).
 *
 * A check must survive serving changes: scaling «۲ پیمانه آرد» to 6 servings changes
 * the quantity, never the ingredient — so the hash is built from the alias-aware
 * canonical id (or normalized text when the glossary doesn't know the item) plus the
 * unit, with the quantity deliberately excluded.
 */
object MisePlace {

    /**
     * Stable identity of one ingredient row: canonical/normalized item + unit.
     * Quantity is NOT part of the hash — scaled servings keep their checked state.
     */
    fun hashOf(item: String, unit: String?): String {
        val base = IngredientRegistry.resolve(item)?.id
            ?: PersianText.normalize(item).filterNot { it.isWhitespace() }
        val u = PersianText.normalize(unit.orEmpty()).filterNot { it.isWhitespace() }
        return "$base#$u"
    }

    /**
     * Split a dish's ingredient blob into parsed rows, original order and spelling.
     * Same splitting as [MissingDiff.itemsOf] — the two never disagree about what a
     * row is, but this keeps quantity/unit for display scaling.
     */
    fun rowsOf(ingredients: String): List<IngredientParser.Parsed> =
        ingredients.split(',', '،')
            .map { IngredientParser.parse(it.trim()) }
            .filter { it.item.isNotBlank() }

    /** One checklist row ready for the UI: display text at [servings], stable [hash]. */
    data class Row(val display: String, val hash: String)

    /** Build UI rows for [parsed] scaled by [factor] — hashes never change with scale (#103). */
    fun rowsFor(parsed: List<IngredientParser.Parsed>, factor: Double): List<Row> =
        parsed.map { p ->
            val base = p.display()
            val display = if (factor == 1.0) base
            else faQty(ServingScaler.scale(base, factor))
            Row(display = display, hash = hashOf(p.item, p.unit))
        }

    /**
     * Scaled quantity display: Persian digits, and ٫ as the decimal separator
     * only when the dot belongs to the LEADING number — a dot inside an item
     * name («گوجه.فرنگی») is text, not a decimal (#103 AC: ۱٫۵ پیمانه).
     */
    private fun faQty(s: String): String {
        val fa = PersianText.toPersianDigits(s)
        val dot = fa.indexOf('.')
        val space = fa.indexOf(' ')
        return if (dot < 0 || (space in 0 until dot)) fa
        else fa.replaceRange(dot, dot + 1, "٫")
    }
}
