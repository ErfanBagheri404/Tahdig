package com.erfanbagheri.tahdig.util

/**
 * Persian/Arabic text normalization for search + de-duplication.
 *
 * SQLite has no Persian collation, so the app stores a normalized copy of every
 * searchable field and normalizes the query the same way. Without this, searching
 * "قورمه سبزی" would miss "قورمه‌سبزی" (ZWNJ) and "ك" would never match "ک".
 */
object PersianText {

    /** ZWNJ — نیم‌فاصله. Zero-width non-joiner, U+200C. */
    const val ZWNJ = '\u200C'

    /**
     * Normalize for matching. NOT for display.
     *
     * - Arabic Yeh ي (U+064A) / Alef Maksura ى (U+0649) → Persian Yeh ی (U+06CC)
     * - Arabic Kaf ك (U+0643) → Persian Kaf ک (U+06A9)
     * - Arabic Heh ة (U+0629) → Heh ه (U+0647)
     * - strips ZWNJ, diacritics (harakat), tatweel
     * - collapses whitespace
     */
    fun normalize(input: String): String = buildString(input.length) {
        var lastWasSpace = false
        for (ch in input) {
            val mapped = when (ch) {
                '\u064A', '\u0649', '\u06D2' -> '\u06CC'   // ي ى ے → ی
                '\u0643'                     -> '\u06A9'   // ك → ک
                '\u0629'                     -> '\u0647'   // ة → ه
                '\u0623', '\u0625', '\u0622' -> '\u0627'   // أ إ آ → ا
                '\u0624'                     -> '\u0648'   // ؤ → و
                '\u0626'                     -> '\u06CC'   // ئ → ی
                '\u0640'                     -> null        // tatweel ـ
                ZWNJ, '\u200B', '\u200E', '\u200F', '\uFEFF' -> null
                in '\u064B'..'\u0652'        -> null        // harakat
                else                         -> ch
            } ?: continue

            val c = if (mapped == '\u0649') '\u06CC' else mapped
            if (c == ' ' || c == '\t' || c == '\n' || c == '\u00A0') {
                if (!lastWasSpace && isNotEmpty()) { append(' '); lastWasSpace = true }
            } else {
                append(c)
                lastWasSpace = false
            }
        }
    }.trim()

    private const val FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
    private const val AR_DIGITS = "٠١٢٣٤٥٦٧٨٩"

    /** Convert ASCII digits in a string to Persian digits for display. */
    fun toPersianDigits(value: String): String = buildString(value.length) {
        for (ch in value) {
            append(if (ch in '0'..'9') FA_DIGITS[ch - '0'] else ch)
        }
    }

    fun toPersianDigits(value: Int): String = toPersianDigits(value.toString())

    /** Convert Persian or Arabic-Indic digits back to ASCII (for input parsing). */
    fun toAsciiDigits(value: String): String = buildString(value.length) {
        for (ch in value) {
            val fa = FA_DIGITS.indexOf(ch)
            val ar = AR_DIGITS.indexOf(ch)
            append(
                when {
                    fa >= 0 -> '0' + fa
                    ar >= 0 -> '0' + ar
                    else   -> ch
                }
            )
        }
    }

    /** True when the string is safe to render RTL without bidi surprises (no lone Latin run). */
    fun isPersian(input: String): Boolean = input.any { it in '\u0600'..'\u06FF' }
}
