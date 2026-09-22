package com.erfanbagheri.tahdig.util

/**
 * Expiry math for the pantry (#106). Pure: every function takes `now`
 * explicitly so the AC's "days-to-expiry math" and the urgency multiplier are
 * unit-testable without a clock or a DB.
 *
 * Days are CALENDAR days, not 24h chunks: something expiring tonight is "today"
 * (0), and anything past its date is negative — the UI bands on that directly.
 */
object ExpiryMath {

    /** Red-band threshold: fewer than [BAND_DAYS] days left. */
    const val BAND_DAYS = 3

    /** Shelf-life defaults in days for known staples, keyed by comparison text. */
    private val SHELF_DAYS = mapOf(
        "برنج" to 365L,
        "آرد" to 180L,
        "روغن" to 365L,
        "شکر" to 365L,
        "چای" to 730L,
        "لپه" to 365L,
        "عدس" to 365L,
        "لوبیا" to 365L,
        "پیاز" to 30L,
        "سیبزمینی" to 50L,
        "سیب زمینی" to 50L,
        "نان" to 3L,
        "شیر" to 5L,
        "ماست" to 7L,
        "پنیر" to 10L,
        "کره" to 14L,
        "تخممرغ" to 21L,
        "گوشت مرغ" to 2L,
        "گوشت قرمز" to 3L,
        "ماهی" to 2L,
        "گوجه" to 7L,
        "خیار" to 7L,
        "گوجهفرنگی" to 7L,
    )

    /** Comparison key: normalized, whitespace-free — same rule as the matcher. */
    private fun key(raw: String): String =
        PersianText.normalize(raw).filterNot { it.isWhitespace() }

    /** Default shelf life in days for [item], or null when the table has no opinion. */
    fun shelfDays(item: String): Long? = SHELF_DAYS[key(item)]

    /** Expiry timestamp for a freshly stocked [item], or null (undated). */
    fun defaultExpiry(item: String, addedAtMs: Long): Long? =
        shelfDays(item)?.let { addedAtMs + it * 86_400_000L }

    /**
     * Calendar days until [expiresAt]: expiry today → 0, yesterday → -1,
     * null (undated) → null. Truncating toward the past, so a shelf life that
     * ends in 9 hours still reads 0 («امروز»), never 1.
     */
    fun daysTo(expiresAt: Long?, now: Long): Int? {
        expiresAt ?: return null
        val dayMs = 86_400_000L
        // Compare at midnight granularity in LOCAL time via zone offset of now.
        val offset = java.util.TimeZone.getDefault().getOffset(now).toLong()
        val today = (now + offset) / dayMs
        val thatDay = (expiresAt + offset) / dayMs
        return (thatDay - today).toInt()
    }

    /** Red band: fewer than 3 days left (past-due counts as urgent too). */
    fun isUrgent(days: Int?): Boolean = days != null && days < BAND_DAYS

    /**
     * Urgency multiplier for the use-it-up ranking (#106):
     * - undated / fresh (3+ days) → 1.0 (no opinion)
     * - expiring → boost, tighter dates boost harder (1.1 / 1.2 / 1.3)
     * - already past → 0.5: a spoiled staple must NOT be pushed to the top
     */
    fun urgencyBoost(days: Int?): Double = when {
        days == null || days >= BAND_DAYS -> 1.0
        days < 0 -> 0.5
        days == 2 -> 1.1
        days == 1 -> 1.2
        else -> 1.3 // today
    }

    /**
     * Coverage × strongest urgency among the dish's expiring pantry items.
     * The MAX is deliberate: one expiring ingredient should lift the dish, two
     * fresh ones must not dilute it.
     */
    fun boostedScore(coverage: Float, boosts: Collection<Double>): Float {
        val boost = boosts.maxOrNull() ?: 1.0
        return coverage * boost.toFloat()
    }

    /** True when the dish's top score was lifted by something expiring (the badge). */
    fun hasExpiryBadge(boosts: Collection<Double>): Boolean = (boosts.maxOrNull() ?: 1.0) > 1.0

    /**
     * Morning summary: «۳ قلم تا ۲ روز آینده: برنج، ماست، پیاز».
     * Empty input → "" (the card hides itself); more than 3 names → +N more.
     */
    fun summary(names: List<String>): String {
        val clean = names.filter { it.isNotBlank() }
        if (clean.isEmpty()) return ""
        val count = PersianText.toPersianDigits(clean.size.toString())
        val shown = clean.take(3).joinToString("، ")
        val more = if (clean.size > 3) " و ${PersianText.toPersianDigits(clean.size - 3)} مورد دیگر" else ""
        return "$count قلم تا ${PersianText.toPersianDigits(BAND_DAYS.toString())} روز آینده: $shown$more"
    }
}
