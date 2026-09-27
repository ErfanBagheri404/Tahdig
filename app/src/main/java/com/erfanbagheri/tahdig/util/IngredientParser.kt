package com.erfanbagheri.tahdig.util

/**
 * Parses a seed ingredient line into (quantity, unit, item) and merges duplicates.
 *
 * The seed stores ingredients as free text ("۲ پیمانه آرد", "پیاز", "۳ عدد تخم‌مرغ"),
 * so shopping-list entries inherit that mess: adding two dishes that both need onion
 * used to produce two separate "پیاز" rows.
 *
 * ponytail: handles the quantity forms the seed actually uses (leading integer,
 * Persian or ASCII digits, optional unit word). No fractions, ranges or
 * parenthesised notes — those pass through as the item text unchanged, which is
 * the safe direction: an unparsed line still lands on the list, just unmerged.
 * Add fraction/range parsing if real recipes start needing it.
 */
object IngredientParser {

    /** Units that may follow a quantity. Longest first so "قاشق غذاخوری" wins over "قاشق". */
    private val UNITS = listOf(
        "قاشق غذاخوری", "قاشق چای‌خوری", "قاشق مرباخوری", "قاشق",
        "پیمانه", "فنجان", "لیوان",
        "کیلوگرم", "کیلو", "گرم",
        "عدد", "حبه", "برگ", "شاخه", "دانه", "بسته", "قوطی", "ورق", "تکه",
    )

    /**
     * Shopping-list grouping buckets, matched against the item text.
     * First match wins, so order matters: more specific buckets first.
     */
    private val CATEGORIES: List<Pair<String, List<String>>> = listOf(
        "پروتئین" to listOf("گوشت", "مرغ", "ماهی", "تخم", "میگو", "کباب", "سوسیس", "کالباس", "جوجه"),
        "لبنیات" to listOf("شیر", "ماست", "پنیر", "کره", "خامه", "دوغ", "کشک", "سرشیر"),
        "سبزیجات" to listOf("پیاز", "سیر", "سیب‌زمینی", "گوجه", "خیار", "بادمجان", "کدو", "هویج", "فلفل دلمه", "قارچ", "کلم", "اسفناج", "سبزی", "نعنا", "جعفری", "شوید", "ریحان", "تره", "شنبلیله", "لیمو"),
        "حبوبات" to listOf("لوبیا", "نخود", "عدس", "ماش", "باقلا", "لپه"),
        "غلات" to listOf("برنج", "آرد", "نان", "ماکارونی", "رشته", "جو", "بلغور", "سمولینا"),
        "ادویه" to listOf("نمک", "زردچوبه", "زعفران", "دارچین", "زنجبیل", "فلفل", "سماق", "هل", "زیره", "آویشن", "ادویه", "رب"),
        "روغن و سس" to listOf("روغن", "سرکه", "سس", "آبلیمو", "عسل", "شکر", "قند"),
    )

    private const val OTHER = "سایر"

    /** One parsed ingredient line. `item` keeps its original spelling for display. */
    data class Parsed(
        val quantity: Double?,
        val unit: String?,
        val item: String,
    ) {
        /** Display form, e.g. "۳ پیمانه آرد" — quantity omitted when unknown. */
        fun display(): String {
            val q = quantity ?: return item
            val qty = if (q == q.toLong().toDouble()) q.toLong().toString() else q.toString()
            return listOf(PersianText.toPersianDigits(qty), unit.orEmpty(), item)
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }
    }

    /**
     * Match key: normalized with all whitespace removed.
     *
     * [PersianText.normalize] strips ZWNJ but keeps the space it often stands for,
     * so "سیب‌زمینی" becomes one word and "سیب زمینی" two — they must still merge.
     */
    private fun key(raw: String): String =
        PersianText.normalize(raw).filterNot { it.isWhitespace() }

    /** Bucket name for grouping in the UI. Prefers the canonical aisle when known. */
    fun categoryOf(item: String): String {
        val k = PersianText.normalize(item)
        return CATEGORIES.firstOrNull { (_, words) -> words.any { k.contains(PersianText.normalize(it)) } }
            ?.first
            ?: OTHER
    }

    /**
     * Parse one line. A leading number (ASCII or Persian) becomes the quantity; a
     * following known unit word is split off; everything else is the item.
     *
     * The item is returned in its ORIGINAL spelling — normalization is only for
     * matching, and would otherwise turn "آرد" into "ارد" on screen.
     */
    fun parse(raw: String): Parsed {
        val text = raw.trim()
        if (text.isEmpty()) return Parsed(null, null, "")

        val tokens = text.split(' ').filter { it.isNotBlank() }
        val firstAscii = PersianText.toAsciiDigits(tokens.firstOrNull().orEmpty())
        val qty = firstAscii.toDoubleOrNull()
        if (qty == null) return Parsed(null, null, text)

        val rest = tokens.drop(1)
        // Longest unit match wins, so "قاشق غذاخوری" is not truncated to "قاشق".
        val unit = UNITS.firstOrNull { u ->
            val uTokens = u.split(' ')
            rest.size >= uTokens.size &&
                rest.take(uTokens.size).map { key(it) } == uTokens.map { key(it) }
        }
        val itemTokens = if (unit != null) rest.drop(unit.split(' ').size) else rest
        val item = itemTokens.joinToString(" ").trim()
        // "۲" alone is a quantity with no item — keep the original text instead of
        // producing an empty row.
        return if (item.isEmpty()) Parsed(null, null, text) else Parsed(qty, unit, item)
    }

    /** Identity of a merged entry: unit + item, normalized. Used to match existing rows. */
    fun mergeKey(unit: String?, item: String): String = key(unit.orEmpty()) + "|" + key(item)

    /**
     * Merge duplicates across every input line, summing quantities of the same
     * (unit, item). Lines that parsed to no quantity still dedupe by item text.
     *
     * Identity goes through [IngredientRegistry] so alias variants of one ingredient
     * («پیاز» / «پیاز قرمز» / "Red Onion") fold into a single row. Unknown items fall
     * back to the raw normalized key and behave exactly as before.
     *
     * Rows only combine when their units are compatible — equal, or one side stating
     * none. "۲ پیمانه آرد" and "۱۰۰ گرم آرد" are the same ingredient but not a summable
     * quantity, so they stay two rows rather than becoming an invented "۱۰۲ پیمانه".
     */
    fun merge(lines: List<String>): List<Parsed> {
        val parsed = lines.map { parse(it) }.filter { it.item.isNotBlank() }
        val result = mutableListOf<Parsed>()
        // Canonical group -> indices of the rows already emitted for it, so a later
        // line can find a compatible row to fold into.
        val groups = LinkedHashMap<String, MutableList<Int>>()

        for (p in parsed) {
            val group = IngredientRegistry.resolve(p.item)?.let { "id:" + it.id }
                ?: key(p.unit.orEmpty()) + "|" + key(p.item)
            val slot = groups[group]?.firstOrNull { idx ->
                val row = result[idx]
                row.unit == null || p.unit == null || key(row.unit) == key(p.unit)
            }
            if (slot == null) {
                result += p
                groups.getOrPut(group) { mutableListOf() } += result.lastIndex
                continue
            }
            val row = result[slot]
            // Only sum when both sides actually carry a quantity; otherwise the sum
            // would invent numbers the source never stated.
            val sum = if (row.quantity != null && p.quantity != null)
                row.quantity + p.quantity else row.quantity
            // First-seen unit and spelling win; they are the ones already on screen.
            result[slot] = row.copy(quantity = sum, unit = row.unit ?: p.unit)
        }
        return result
    }

    /** Group merged entries into display buckets, preserving insertion order within each. */
    fun group(entries: List<Parsed>): List<Pair<String, List<Parsed>>> {
        val buckets = linkedMapOf<String, MutableList<Parsed>>()
        for (e in entries) {
            buckets.getOrPut(categoryOf(e.item)) { mutableListOf() }.add(e)
        }
        // "سایر" always last so the meaningful buckets lead.
        return buckets.entries
            .sortedBy { if (it.key == OTHER) 1 else 0 }
            .map { it.key to it.value.toList() }
    }
}
