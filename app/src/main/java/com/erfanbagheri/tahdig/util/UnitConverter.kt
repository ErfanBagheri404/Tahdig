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
}
