package com.erfanbagheri.tahdig.util

/**
 * Full nutrition label math (#111): %DV bars, Nutri-Score and NOVA.
 *
 * All pure — the UI only renders what this returns, so the scoring can be
 * tested against the published thresholds instead of eyeballed.
 */
object NutriLabel {

    /** Reference daily values (FDA, 2000 kcal diet). */
    object DV {
        const val FAT_G = 78.0
        const val SAT_FAT_G = 20.0
        const val CARB_G = 275.0
        const val FIBER_G = 28.0
        const val PROTEIN_G = 50.0
        const val SODIUM_MG = 2300.0
    }

    /** Percent of daily value, clamped to 0..999 so the bar can't overflow. */
    fun percentDv(amount: Double, reference: Double): Int {
        if (reference <= 0.0 || amount <= 0.0) return 0
        return (amount / reference * 100).toInt().coerceIn(0, 999)
    }

    enum class Score(val letter: String) { A("A"), B("B"), C("C"), D("D"), E("E") }

    /** Nutri-Score inputs, per 100 g of the dish. */
    data class Facts(
        val energyKj: Double,
        val sugarsG: Double,
        val saturatedFatG: Double,
        /** Salt, not sodium: salt = sodium × 2.5. */
        val saltG: Double,
        val fiberG: Double,
        val proteinG: Double,
        /** Fruits / vegetables / legumes share, 0..100. */
        val fruitVegPercent: Int = 0,
    )

    /**
     * Nutri-Score 2023 (general foods, solid). Negative points come from
     * energy/sugars/sat-fat/salt; positive from fruit-veg/fibre/protein.
     * Protein is NOT counted when negative points reach 11 — otherwise a
     * fatty cured meat would score on its protein alone.
     */
    fun nutriScore(f: Facts): Score {
        val negative = points(f.energyKj, ENERGY) +
            points(f.sugarsG, SUGARS) +
            points(f.saturatedFatG, SAT_FAT) +
            points(f.saltG, SALT)

        var positive = points(f.fruitVegPercent.toDouble(), FRUIT_VEG) +
            points(f.fiberG, FIBER)
        if (negative < 11) positive += points(f.proteinG, PROTEIN)

        val score = negative - positive
        return when {
            score <= 0 -> Score.A
            score <= 2 -> Score.B
            score <= 10 -> Score.C
            score <= 18 -> Score.D
            else -> Score.E
        }
    }

    /** Highest threshold strictly below [value], i.e. the standard 0..n point bands. */
    private fun points(value: Double, thresholds: DoubleArray): Int {
        var p = 0
        for (t in thresholds) if (value > t) p++ else break
        return p
    }

    // Ascending upper bounds of each point band (index 0 = the "0 points" band).
    private val ENERGY = doubleArrayOf(335.0, 670.0, 1005.0, 1340.0, 1675.0, 2010.0, 2345.0, 2680.0, 3015.0, 3350.0)
    private val SUGARS = doubleArrayOf(3.4, 6.8, 10.0, 14.0, 17.0, 20.0, 24.0, 27.0, 31.0, 34.0)
    private val SAT_FAT = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
    private val SALT = doubleArrayOf(0.2, 0.4, 0.6, 0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0)
    private val FIBER = doubleArrayOf(0.9, 1.9, 2.8, 3.7, 4.7)
    private val PROTEIN = doubleArrayOf(1.6, 3.2, 4.8, 6.4, 8.0)
    private val FRUIT_VEG = doubleArrayOf(40.0, 60.0, 80.0, 100.0)

    /**
     * NOVA group 1..4. The classifier is deliberately marker-based rather than
     * ingredient-count-based: it names the additive classes that actually make
     * a food ultra-processed, and errs toward the LOWER group on no evidence.
     */
    fun nova(ingredientText: String): Int {
        val text = ingredientText.lowercase()
        if (text.isBlank()) return 1

        // Group 4 markers: cosmetic additives and industrial textures.
        val ultra = listOf(
            "emulsifier", "emulsifier", "stabiliser", "stabilizer", "thickener",
            "flavour enhancer", "flavor enhancer", "artificial", "hydrogenated",
            "high fructose", "hfcs", "invert sugar", "glucose syrup",
            "modified starch", "maltodextrin", "dextrose", "aspartame",
            "sucralose", "msg", "monosodium", "colour", "color", "preservative",
            "e1", "e4", "جوهر", "اسانس", "امولسیفایر", "پایدارکننده",
        )
        if (ultra.any { text.contains(it) }) return 4

        // Group 3 markers: preserved or fermented from group-1 foods.
        val processed = listOf(
            "canned", "tin", "smoked", "cured", "salted", "brine", "pickled",
            "کنسرو", "دودی", "نمک‌سود", "ترشی", "شور",
        )
        if (processed.any { text.contains(it) }) return 3

        // Group 2 markers: culinary ingredients used at home, not eaten alone.
        val culinary = listOf(
            "salt", "sugar", "oil", "butter", "flour", "honey", "vinegar",
            "نمک", "شکر", "روغن", "کره", "آرد", "عسل", "سرکه",
        )
        val hasCulinary = culinary.any { text.contains(it) }

        // A single whole ingredient or a list of whole foods is group 1;
        // anything carrying a culinary ingredient alongside is group 2.
        return if (hasCulinary) 2 else 1
    }

    /** Farsi subtitle for a NOVA group, as the issue specifies. */
    fun novaLabel(group: Int): String = when (group) {
        1 -> "کم‌فراوری"
        2 -> "مادهٔ آشپزی"
        3 -> "فراوری‌شده"
        else -> "فراوری‌شدهٔ زیاد"
    }

    /**
     * Which Nutri-Score grades a filter chip keeps. A-B is the "better half"
     * the issue asks for, so the caller never has to know the enum order.
     */
    fun passesFilter(score: Score, keepAB: Boolean): Boolean =
        !keepAB || score == Score.A || score == Score.B
}
