package com.erfanbagheri.tahdig.util

/** Simple volume↔weight converters for common cooking units. */
object UnitConverter {
    data class Conversion(val value: Double, val unit: String)

    /** 1 cup ≈ 240ml ≈ ~120g (flour) / 200g (sugar) / 230g (rice). */
    private val GRAMS_PER_CUP = mapOf(
        "آرد" to 120.0, "شکر" to 200.0, "برنج" to 185.0, "نمک" to 280.0,
        "شیر" to 240.0, "روغن" to 220.0, "کره" to 230.0,
    )
    private const val DEFAULT_GRAMS_PER_CUP = 150.0
    private const val TBSP_PER_CUP = 16.0
    private const val TSP_PER_CUP = 48.0

    /** Density-aware: teaspoons/tablespoons scale from the ingredient's g-per-cup. */
    private fun gramsPerCup(ingredient: String): Double {
        val key = GRAMS_PER_CUP.keys.find { ingredient.contains(it) }
        return key?.let { GRAMS_PER_CUP[it] } ?: DEFAULT_GRAMS_PER_CUP
    }

    fun gramsToTablespoons(g: Double, ingredient: String = ""): Conversion =
        Conversion(g / (gramsPerCup(ingredient) / TBSP_PER_CUP), "قاشق غذاخوری")

    fun gramsToTeaspoons(g: Double, ingredient: String = ""): Conversion =
        Conversion(g / (gramsPerCup(ingredient) / TSP_PER_CUP), "قاشق چایخوری")

    fun cupsToGrams(cups: Double, ingredient: String = ""): Conversion {
        return Conversion(cups * gramsPerCup(ingredient), "گرم")
    }

    fun cupsToTablespoons(cups: Double): Conversion = Conversion(cups * TBSP_PER_CUP, "قاشق غذاخوری")

    fun tablespoonsToGrams(tb: Double, ingredient: String = ""): Conversion =
        Conversion(tb * (gramsPerCup(ingredient) / TBSP_PER_CUP), "گرم")

    fun teaspoonsToGrams(tsp: Double, ingredient: String = ""): Conversion =
        Conversion(tsp * (gramsPerCup(ingredient) / TSP_PER_CUP), "گرم")

    // ── Persian kitchen units (#103) ────────────────────────────────
    /** Calibration table: Persian unit → milliliters. AC: لیوان→ml exact per THIS. */
    val MILLILITERS: Map<String, Double> = mapOf(
        "لیوان" to 240.0,
        "نیم‌لیوان" to 120.0,
        "پیمانه" to 180.0,
        "قاشق غذاخوری" to 15.0,
        "قاشق چایخوری" to 5.0,
    )

    /** One converter-sheet row: `amount` × [from] expressed in [to]. */
    data class SheetRow(val amount: Double, val from: String, val to: String) {
        /** Stable identity across the metric⇄ایرانی toggle, for the favorite. */
        val key: String get() = "$from|$to"
    }

    /**
     * Convert [amount] between [from] and [to] through milliliters (both units
     * in the table), identity when equal, null when the pair is unknown — the
     * caller hides unknown rows rather than showing a wrong number.
     */
    fun convert(amount: Double, from: String, to: String): Double? = when {
        from == to -> amount
        to == "میلی‌لیتر" -> MILLILITERS[from]?.let { amount * it }
        from == "میلی‌لیتر" -> MILLILITERS[to]?.let { amount / it }
        else -> {
            val ml = MILLILITERS[from]
            val target = MILLILITERS[to]
            if (ml == null || target == null) null else amount * ml / target
        }
    }

    /** Metric display: kitchen unit → ml (and flour/sugar density rows in grams). */
    fun metricRows(): List<SheetRow> = listOf(
        SheetRow(1.0, "لیوان", "میلی‌لیتر"),
        SheetRow(1.0, "نیم‌لیوان", "میلی‌لیتر"),
        SheetRow(1.0, "پیمانه", "میلی‌لیتر"),
        SheetRow(1.0, "قاشق غذاخوری", "میلی‌لیتر"),
        SheetRow(1.0, "قاشق چایخوری", "میلی‌لیتر"),
        SheetRow(1.0, "لیوان آرد", "گرم"),
        SheetRow(1.0, "لیوان شکر", "گرم"),
    )

    /** ایرانی display: the same pairs read backwards — same rows, `to` swapped. */
    fun iranianRows(): List<SheetRow> = listOf(
        SheetRow(240.0, "میلی‌لیتر", "لیوان"),
        SheetRow(120.0, "میلی‌لیتر", "نیم‌لیوان"),
        SheetRow(180.0, "میلی‌لیتر", "پیمانه"),
        SheetRow(15.0, "میلی‌لیتر", "قاشق غذاخوری"),
        SheetRow(5.0, "میلی‌لیتر", "قاشق چایخوری"),
        SheetRow(120.0, "گرم", "لیوان آرد"),
        SheetRow(200.0, "گرم", "لیوان شکر"),
    )

    /** Resolved display value of a row: number in the row's `to` unit, null = hide. */
    fun valueOf(row: SheetRow): Double? = when {
        row.to == "گرم" -> {
            val cup = row.from.removePrefix("لیوان ").let { gramsPerCup(it) }
            row.amount * cup
        }
        row.from == "گرم" -> {
            val cup = row.to.removePrefix("لیوان ").let { gramsPerCup(it) }
            row.amount / cup
        }
        else -> convert(row.amount, row.from, row.to)
    }
}
