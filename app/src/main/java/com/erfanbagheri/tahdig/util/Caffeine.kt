package com.erfanbagheri.tahdig.util

import java.time.LocalDate

/**
 * Caffeine math (#119). Pure — the DAO supplies [Entry] rows, this file
 * decides totals, caps, and warnings. No clock: callers pass [today].
 *
 * Caps follow the common public guidance: 400 mg/day for healthy adults
 * (EFSA 2015), 200 mg/day in pregnancy (ACOG 2020). The pregnancy cap
 * wins whenever pregnancy mode is on — mode beats the manual slider.
 *
 * Food-safety follows the same conservative pattern as HalalFlags (#118)
 * and AllergenDetector (#112): an unknown ingredient never warns, a
 * positive whole-word match does, and the wording says «avoid» only
 * about named risks, never certifying a dish as safe.
 */
object Caffeine {

    /** Evidence text for the safety card's «چرا؟» line. */
    data class SafetyFlag(val category: String, val why: String)

    data class Entry(val date: LocalDate, val mg: Int)

    /**
     * Quick-log chips in display order. Each pairs a Farsi label with its
     * typical mg — 40 for tea matches the AC's worked example, and the
     * 3-coffee fixture (3 × 95 = 285) is asserted in CaffeineTest.
     */
    val PRESETS = listOf(
        "چای" to 40,
        "قهوه" to 95,
        "نسکافه" to 65,
    )

    /** Healthy-adult default cap (EFSA 2015). */
    const val DEFAULT_CAP_MG = 400

    /** Pregnancy/breastfeeding cap (ACOG 2020). */
    const val PREGNANCY_CAP_MG = 200

    /**
     * The cap in force. Pregnancy mode always wins over the manual slider —
     * a user who set 600 mg and then turns the mode on gets 200, not 600.
     */
    fun effectiveCap(customMg: Int?, pregnancyMode: Boolean): Int =
        if (pregnancyMode) {
            PREGNANCY_CAP_MG
        } else {
            customMg?.coerceIn(0, 2000) ?: DEFAULT_CAP_MG
        }

    /** Milligrams logged on [today]. */
    fun todayTotal(entries: List<Entry>, today: LocalDate): Int =
        entries.filter { it.date == today }.sumOf { it.mg }

    /** True when [total] has passed the cap — at exactly the cap is fine. */
    fun overCap(total: Int, capMg: Int): Boolean = total > capMg

    /**
     * Persian progress line for the accumulation bar, e.g.
     * «۹۵ از ۴۰۰ میلی‌گرم» — plus the carried note when pregnancy mode
     * tightened the cap, so the 200 line under a 600 slider is not a
     * surprise.
     */
    fun progressLine(total: Int, capMg: Int, pregnancyMode: Boolean): String {
        val line = PersianText.toPersianDigits(total.toDouble()) + " از " +
            PersianText.toPersianDigits(capMg.toDouble()) + " میلی‌گرم"
        return if (pregnancyMode) "$line · حالت بارداری" else line
    }

    // --- food-safety keywords ------------------------------------------------

    private val RAW_EGG = listOf("تخم‌مرغ خام", "تخم مرغ خام", "زرده خام", "raw egg")
    private val HIGH_MERCURY = listOf("ماهی تن", "تن ماهی", "tuna", "کوسه", "اره‌ماهی", "شمشیرماهی", "ماهي خال‌مخالی بزرگ", "king mackerel")
    private val UNDERCOOKED_MEAT = listOf("گوشت نیم‌پز", "نیم‌پز", "استیک ریر", "rare steak", "medium rare")
    private val UNPASTEURIZED = listOf("پنیر خام", "شیر خام", "unpasteurized")
    private val HIGH_CAFFEINE_FOOD = listOf("انرژی‌زا", "نوشابه انرژی", "energy drink")
    private val LIVER = listOf("جگر", "liver")

    /**
     * Flags for a pregnancy-mode user reading [ingredients]. Whole-word
     * matching, same as HalalFlags: «ماهی» alone never trips the tuna line.
     */
    fun safetyFlags(ingredients: String): Set<SafetyFlag> {
        val out = mutableSetOf<SafetyFlag>()
        // Multi-word phrases match as substrings of the raw text («جگر
        // گوسفند» still trips the liver line); single words need a
        // whole-word match so «ماهی» alone never trips the tuna line.
        fun hit(vararg phrases: String): Boolean =
            phrases.any { phrase ->
                if (phrase.contains(' ')) ingredients.contains(phrase) else
                    ingredients.split(Regex("[^\\p{L}\\p{N}]+"))
                        .any { it.equals(phrase, ignoreCase = true) }
            }
        if (hit(*RAW_EGG.toTypedArray())) {
            out += SafetyFlag("تخم‌مرغ خام", "خطر سالمونلا — باید کاملاً پخته شود")
        }
        if (hit(*HIGH_MERCURY.toTypedArray())) {
            out += SafetyFlag("ماهی پرجیوه", "ماهی‌های بزرگ جیوه بالا دارند — حداکثر هفته‌ای یک وعده کوچک")
        }
        if (hit(*UNDERCOOKED_MEAT.toTypedArray())) {
            out += SafetyFlag("گوشت نیم‌پز", "گوشت باید کاملاً پخته شود — بدون قسمت صورتی")
        }
        if (hit(*UNPASTEURIZED.toTypedArray())) {
            out += SafetyFlag("لبنیات غیرپاستوریزه", "خطر لیستریا — فقط پاستوریزه")
        }
        if (hit(*HIGH_CAFFEINE_FOOD.toTypedArray())) {
            out += SafetyFlag("کافئین پنهان", "در سقف روزانه حساب می‌شود")
        }
        if (hit(*LIVER.toTypedArray())) {
            out += SafetyFlag("جگر", "ویتامین A بالا — در بارداری محدود شود")
        }
        return out
    }

    fun warningText(flags: Set<SafetyFlag>): String? {
        if (flags.isEmpty()) return null
        return "احتیاط در بارداری: " + flags.joinToString("، ") { it.category }
    }
}
