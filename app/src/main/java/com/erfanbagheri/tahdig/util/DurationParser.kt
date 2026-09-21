package com.erfanbagheri.tahdig.util

/**
 * Finds cooking durations in a step's text so step mode can offer an inline timer.
 *
 * Handles the forms a recipe actually writes: "۴۵ دقیقه", "۱ ساعت", "۱ ساعت و ۳۰ دقیقه",
 * "۲ ساعت و نیم", ASCII or Persian digits.
 *
 * ponytail: no "تا" / "حدود" qualifiers and no bare "نیم ساعت". A range
 * ("۱۰ تا ۱۵ دقیقه") yields the number attached to the unit — 15, the upper
 * bound, which is the safe direction for a timer. The seed descriptions are
 * one-liners today; extend when real recipe text needs it.
 */
object DurationParser {

    /** A duration found in text, with the source range so callers can highlight it. */
    data class Found(
        val seconds: Long,
        val minutes: Long,
        val raw: String,
        val range: IntRange,
    )

    private const val MINUTE = 60L
    private const val HOUR = 60L * 60L

    // Persian: ۴۵ دقیقه | ۱ ساعت | ۲ ساعت و نیم
    private val FA = Regex(
        "([۰-۹0-9]+)\\s*(دقیقه|ساعت)(?:\\s*و\\s*(?:([۰-۹0-9]+)\\s*دقیقه|نیم))?"
    )

    /**
     * First duration in [text], or null when the text states none.
     * "۱ ساعت و ۳۰ دقیقه" → 90 minutes; "۲ ساعت و نیم" → 150 minutes.
     */
    fun first(text: String): Found? {
        val m = FA.find(text) ?: return null
        val amount = PersianText.toAsciiDigits(m.groupValues[1]).toLongOrNull() ?: return null
        val unit = m.groupValues[2]
        val extra = m.groupValues[3]

        var seconds = if (unit == "ساعت") amount * HOUR else amount * MINUTE
        val raw = m.value

        when {
            // "۱ ساعت و ۳۰ دقیقه"
            extra.isNotEmpty() -> {
                val add = PersianText.toAsciiDigits(extra).toLongOrNull() ?: 0L
                seconds += add * MINUTE
            }
            // "۲ ساعت و نیم"
            raw.contains("نیم") -> seconds += HOUR / 2
        }

        // A zero duration is not a timer worth offering.
        if (seconds <= 0L) return null
        return Found(seconds = seconds, minutes = seconds / MINUTE, raw = raw, range = m.range)
    }

    /** All durations in [text], in order. */
    fun all(text: String): List<Found> {
        val out = mutableListOf<Found>()
        var start = 0
        while (start < text.length) {
            val m = FA.find(text, start) ?: break
            val slice = text.substring(m.range.first, m.range.last + 1)
            first(slice)?.let { out += it }
            start = m.range.last + 1
        }
        return out
    }

    /** mm:ss for a timer display. */
    fun mmss(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return "${if (m < 10) "0$m" else "$m"}:${if (s < 10) "0$s" else "$s"}"
    }
}
