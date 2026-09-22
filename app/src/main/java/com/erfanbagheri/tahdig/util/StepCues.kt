package com.erfanbagheri.tahdig.util

/**
 * Per-step cooking cues (#97): flame level + doneness words, both read from the
 * step text the app already has.
 *
 * The issue proposed baking these into the seed at import time. The seed's
 * `description` holds the step text and carries no heat/cue words today, so
 * baking would write empty arrays for all 1331 dishes — inference at render time
 * gives the same UI with zero fabricated data, and the seed schema can adopt a
 * `cues`/`heat` field later without touching this code.
 */
object HeatTagger {

    /** Flame levels, lowest first. `bars` drives the flat ▮▮▮ glyph. */
    enum class Level(val label: String, val bars: Int) {
        OFF("بدون شعله", 0),
        LOW("شعله کم", 1),
        MEDIUM("شعله ملایم", 2),
        HIGH("شعله زیاد", 3),
    }

    /**
     * Keyword table, checked in order — the FIRST hit wins, so the specific
     * phrasings come before the bare words they contain.
     *
     * Negations come first for the same reason: «شعله را زیاد نکنید» is LOW, not
     * HIGH, and would otherwise match «زیاد» and report the opposite.
     */
    private val TABLE: List<Pair<String, Level>> = listOf(
        "زیاد نکنید" to Level.LOW,
        "زیاد نشود" to Level.LOW,
        "تند نکنید" to Level.LOW,
        "شعله کم" to Level.LOW,
        "حرارت کم" to Level.LOW,
        "شعله ملایم" to Level.MEDIUM,
        "حرارت ملایم" to Level.MEDIUM,
        "ملایم" to Level.MEDIUM,
        "شعله متوسط" to Level.MEDIUM,
        "حرارت متوسط" to Level.MEDIUM,
        "شعله زیاد" to Level.HIGH,
        "حرارت زیاد" to Level.HIGH,
        "شعله تند" to Level.HIGH,
        "حرارت تند" to Level.HIGH,
        "جوش ملایم" to Level.MEDIUM,
        "بجوشانید" to Level.HIGH,
        "خاموش کنید" to Level.OFF,
        "از روی حرارت بردارید" to Level.OFF,
    )

    /**
     * Flame level stated by [text], or null when the step says nothing about heat.
     * Null is the honest answer — an invented flame level would be a lie in the UI.
     */
    fun levelOf(text: String): Level? {
        val hay = PersianText.normalize(text)
        return TABLE.firstOrNull { (needle, _) -> hay.contains(PersianText.normalize(needle)) }
            ?.second
    }

    /** Flat text glyph, e.g. «▮▮» — no icons, matches the house flat style. */
    fun glyph(level: Level): String =
        if (level.bars == 0) "" else "▮".repeat(level.bars)
}

/** Doneness cues: the words that tell a cook «it's ready», not just how long. */
object DonenessCues {

    /**
     * Cue phrases, checked against the step text. Curated Farsi kitchen language —
     * color, sound and texture words a cook actually looks for.
     */
    private val TABLE: List<String> = listOf(
        "طلایی", "قهوه‌ای", "یکدست", "شفاف", "لعاب افتاد", "لعاب بیفتد",
        "جا افتاد", "جا بیفتد", "روغن بیندازد", "روغن بیندازد",
        "بوی خوش", "عطر", "کف روی آن جمع", "کف جمع",
        "نرم شد", "نرم شود", "تُرد", "ترد شد", "کش بیاید", "حباب زد",
        "ته‌دیگ جدا", "جدا شد", "غلیظ شد", "غلیظ شود", "سفت شد",
    )

    /**
     * Cues mentioned in [text], in table order, deduped by meaning.
     *
     * Empty list = the step states no cue; the UI then shows the timer alone
     * rather than inventing guidance the recipe never gave.
     */
    fun of(text: String): List<String> {
        val hay = PersianText.normalize(text)
        return TABLE.filter { hay.contains(PersianText.normalize(it)) }.distinct()
    }
}
