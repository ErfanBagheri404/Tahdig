package com.erfanbagheri.tahdig.util

/**
 * Equipment inference from dish text (#100): the seed may carry an explicit
 * `equipment` list, but most of the 1331 dishes don't — their steps do name the
 * tools («در فر بگذارید», «تابه را داغ کنید»), so keywords fill the gap.
 *
 * Matching is boundary-checked on Persian letters: «فر» must not fire inside
 * «فرش» or «فرایند».
 *
 * ponytail: strict boundaries mean an attached suffix («تابه‌ای» → «تابهای»)
 * is NOT matched — a missing chip, never a wrong one. Suffix-tolerant matching
 * can be added if real steps start under-reporting tools.
 */
object EquipmentInferrer {

    /**
     * keyword → Farsi tool label. Single tokens only; [PersianText.normalize]
     * strips ZWNJ, so «مخلوط‌کن» matches both spelled variants.
     */
    private val TABLE: List<Pair<String, String>> = listOf(
        "فر" to "فر",
        "تابه" to "تابه",
        "قابلمه" to "قابلمه",
        "آبکش" to "آبکش",
        "مخلوط‌کن" to "مخلوط‌کن",
        "همزن" to "همزن",
        "زودپز" to "زودپز",
        "پلوپز" to "پلوپز",
        "سینی" to "سینی",
        "گریل" to "گریل",
        "کباب‌پز" to "کباب‌پز",
        "چرخ‌گوشت" to "چرخ‌گوشت",
        "رنده" to "رنده",
        "مایکروویو" to "مایکروویو",
        "دم‌کنی" to "دم‌کنی",
        "الک" to "الک",
        "بخارپز" to "بخارپز",
        "توستر" to "توستر",
        "روغن‌گیر" to "روغن‌گیر",
    )

    /** All tool labels this inferrer can ever produce — the seed bake validates against it. */
    val knownLabels: List<String> = TABLE.map { it.second }

    /**
     * Equipment named by [texts] (steps + ingredients), in table order, deduped.
     * Pass every free-text field of the dish; explicit seed equipment should be
     * used by the caller FIRST, this is the fallback.
     */
    fun infer(vararg texts: String): List<String> {
        val hay = PersianText.normalize(texts.joinToString(" "))
        return TABLE.filter { (needle, _) -> containsWord(hay, PersianText.normalize(needle)) }
            .map { it.second }
            .distinct()
    }

    /**
     * Seed-first resolution: explicit comma-separated equipment when present,
     * keyword inference otherwise. One entry point so detail and cook mode
     * can never disagree about a dish's tools.
     */
    fun forDish(explicitEquipment: String, vararg texts: String): List<String> {
        val explicit = explicitEquipment.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return explicit.ifEmpty { infer(*texts) }
    }

    /** Word-boundary search: neighbors of a hit must not be Persian letters. */
    private fun containsWord(hay: String, needle: String): Boolean {
        if (needle.isEmpty()) return false
        var i = hay.indexOf(needle)
        while (i >= 0) {
            val before = if (i == 0) null else hay[i - 1]
            val end = i + needle.length
            val after = if (end >= hay.length) null else hay[end]
            if (!isFaLetter(before) && !isFaLetter(after)) return true
            i = hay.indexOf(needle, i + 1)
        }
        return false
    }

    private fun isFaLetter(c: Char?): Boolean =
        c != null && Character.isLetter(c)
}
