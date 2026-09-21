package com.erfanbagheri.tahdig.util

/** Simple volume↔weight converters for common cooking units. */
object UnitConverter {
    data class Conversion(val value: Double, val unit: String)

    /** 1 cup ≈ 240ml ≈ ~120g (flour) / 200g (sugar) / 230g (rice). */
    private val GRAMS_PER_CUP = mapOf(
        "آرد" to 120.0, "شکر" to 200.0, "برنج" to 185.0, "نمک" to 280.0,
        "شیر" to 240.0, "روغن" to 220.0, "کره" to 230.0,
    )
    private val GRAMS_PER_TBSP = 15.0 // ~15g per tablespoon for most dry goods
    private val GRAMS_PER_TSP = 5.0

    fun gramsToTablespoons(g: Double, ingredient: String = ""): Conversion = Conversion(g / GRAMS_PER_TBSP, "قاشق غذاخوری")
    fun gramsToTeaspoons(g: Double, ingredient: String = ""): Conversion = Conversion(g / GRAMS_PER_TSP, "قاشق چایخوری")

    fun cupsToGrams(cups: Double, ingredient: String = ""): Conversion {
        val key = GRAMS_PER_CUP.keys.find { ingredient.contains(it) }
        val gPerCup = key?.let { GRAMS_PER_CUP[it] } ?: 150.0
        return Conversion(cups * gPerCup, "گرم")
    }

    fun cupsToTablespoons(cups: Double): Conversion = Conversion(cups * 16.0, "قاشق غذاخوری")
    fun tablespoonsToGrams(tb: Double): Conversion = Conversion(tb * GRAMS_PER_TBSP, "گرم")
}
