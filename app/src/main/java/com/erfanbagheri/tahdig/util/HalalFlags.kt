package com.erfanbagheri.tahdig.util

/**
 * Halal-style ingredient flags (#118).
 *
 * Deliberately NOT a halal certification and deliberately conservative. There
 * is no certification dataset anywhere in this app, so this object answers one
 * narrow question: does this ingredient NAME positively name something a user
 * avoiding pork/alcohol/animal gelatin would want to know about?
 *
 * Two rules hold everywhere in this file:
 *  1. Only a POSITIVE word match flags. An unknown or ambiguous ingredient
 *     returns nothing — a false positive here makes a safe dish look unsafe,
 *     which is worse than saying nothing. The acceptance criterion spells this
 *     out: suspect/unknown text produces no flag.
 *  2. The display wording says «بررسی کن» (check this) and never claims a dish
 *     is halal or permitted. A name match is a reason to look, not a verdict.
 *
 * Alcohol is matched even when the dish is cooked — cooking does not reliably
 * remove alcohol from a wine sauce, and the user asked to see it anyway.
 */
object HalalFlags {

    /** Matching keys, Persian-first then English. */
    private val PORK: List<String> = listOf(
        "گوشت خوک", "ژامبون خوک", "ژامبون", "بیکن", "سوسیس خوک",
        "کالباس خوک", "خوک", "پپرونی", "سالامی",
        "pork", "ham", "bacon", "prosciutto", "salami", "pepperoni",
        "pancetta", "chorizo", "gammon", "lard", "lardons",
    )

    private val GELATIN: List<String> = listOf(
        "ژلاتین", "ژله", "ژلاتین حیوانی", "ژلاتین خوک", "ژلاتین گاو",
        "gelatin", "gelatine", "animal gelatin", "hide glue",
        "e441", "e 441", "e442", "e 442", "e450", "e 450",
    )

    private val ALCOHOL: List<String> = listOf(
        "شراب", "الکل", "لیکور", "نوشیدنی الکلی", "آبجو",
        "wine", "beer", "brandy", "rum", "vodka",
        "whisky", "whiskey", "liqueur", "sherry", "cognac",
        "kirsch", "champagne", "mead",
    )

    /**
     * The flags this dish's ingredient text positively names, as Farsi display
     * labels. Empty means no flag — the caller renders nothing at all.
     *
     * Matching runs on a normalized key that KEEPS whitespace and splits ZWNJ
     * (see [key]), so «شراب سفید» and «ژلاتین‌دار» both match while «ژلی»
     * inside an unrelated word does not. Each comma/semicolon line is checked
     * alone, and a line joining two items with «و» is split apart first, so one
     * flagged item cannot smear its flag onto its neighbour.
     */
    fun flags(ingredients: String): List<String> {
        if (ingredients.isBlank()) return emptyList()
        val out = mutableListOf<String>()
        for (line in ingredients.split(',', '،', ';', '؛')) {
            val item = IngredientParser.parse(line).item
            if (item.isBlank()) continue
            val candidates = if (item.contains(" و ")) item.split(" و ") else listOf(item)
            for (candidate in candidates) {
                val key = key(candidate)
                if (key.isEmpty()) continue
                if (PORK.any { containsWord(key, key(it)) }) {
                    if ("گوشت خوک" !in out) out += "گوشت خوک"
                }
                if (GELATIN.any { containsWord(key, key(it)) }) {
                    if ("ژلاتین حیوانی" !in out) out += "ژلاتین حیوانی"
                }
                if (ALCOHOL.any { containsWord(key, key(it)) }) {
                    if ("الکل" !in out) out += "الکل"
                }
            }
        }
        return out
    }

    /**
     * Normalized comparison key. Whitespace is KEPT because it is the boundary
     * the matcher needs: in «شراب سفید» the word شراب is a token, and only a
     * token boundary stops «آب» matching inside it.
     *
     * ZWNJ is turned into a space first. Persian compounds write
     * «ژلاتین‌دار» as one token, so without the split the boundary test would
     * reject the very word the user needs to see. PersianText.normalize then
     * folds the letter variants (ك->ک, ي->ی, آ->ا) and collapses the spaces.
     */
    private fun key(s: String): String =
        PersianText.normalize(s.replace(PersianText.ZWNJ, ' ')).lowercase()

    /**
     * Whole-word containment on stripped keys. The boundary test is a real
     * letter/digit check, not a substring: «ham» inside «sham» fails the
     * before-boundary, «rum» inside «ramen» fails the after-boundary.
     */
    private fun containsWord(haystack: String, needle: String): Boolean {
        if (needle.isEmpty() || needle.length > haystack.length) return false
        var i = haystack.indexOf(needle)
        while (i >= 0) {
            val before = if (i == 0) null else haystack[i - 1]
            val after = if (i + needle.length >= haystack.length) null
                else haystack[i + needle.length]
            val okBefore = before == null || !before.isLetterOrDigit()
            val okAfter = after == null || !after.isLetterOrDigit()
            if (okBefore && okAfter) return true
            i = haystack.indexOf(needle, i + 1)
        }
        return false
    }

    /**
     * The detail-screen row. Returns null when nothing is flagged, so the
     * caller renders nothing at all — the AC says an ambiguous ingredient
     * produces no row.
     */
    fun warningText(flags: List<String>): String? =
        if (flags.isEmpty()) null
        else "بررسی کن: " + flags.joinToString("، ")

    /** True when the dish must be hidden search-wide (strict toggle on). */
    fun shouldHide(strict: Boolean, flags: List<String>): Boolean =
        strict && flags.isNotEmpty()
}
