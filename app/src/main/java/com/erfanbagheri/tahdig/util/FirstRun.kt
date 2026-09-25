package com.erfanbagheri.tahdig.util

/**
 * First-run experience (#126) — the pure decision layer.
 *
 * Three small pieces, all testable without Compose:
 * - [Tip]: one-line contextual hints, dismissed permanently via a prefs set.
 * - [starterChips]: bundled search suggestions for a history-less install.
 * - [relaxed]: given a query that matched nothing, what to offer instead.
 */
object FirstRun {

    /** Hint ids are stable strings — renaming one resurrects a dismissed tip. */
    object Tip {
        const val HOME_REROLL = "home_reroll"
        const val SEARCH_CHIPS = "search_chips"
        const val PANTRY = "pantry_what_i_have"
        const val PLAN = "plan_generator"
        const val JOURNAL = "journal_capture"
        const val SHOPPING_MERGE = "shopping_merge"
    }

    val ALL_TIPS = listOf(
        Tip.HOME_REROLL,
        Tip.SEARCH_CHIPS,
        Tip.PANTRY,
        Tip.PLAN,
        Tip.JOURNAL,
        Tip.SHOPPING_MERGE,
    )

    /**
     * Popular starter searches. Bundled, not fetched — the app is offline-first
     * and a brand-new install has no history to draw from.
     *
     * Every chip is verified against the shipped seed as a raw substring, the
     * same way SQLite's `LIKE` matches: `چلو مرغ` (with a space) hits nothing
     * because the catalogue spells it `چلو`/`مرغ` separately, and `زرشک پلو`
     * misses `زرشک‌پلو با مرغ` because of the ZWNJ. A starter that returns zero
     * rows is the exact dead end this feature exists to remove.
     */
    val starterChips = listOf(
        "قورمه‌سبزی",  // 1
        "زرشک‌پلو",    // 1
        "کباب",        // 24
        "پاستا",       // 7
        "املت",        // 9
        "سالاد",       // 26
    )

    /**
     * Relaxed suggestions for a zero-result query, longest first.
     *
     * Every trailing truncation, so «کباب زعفرانی مخصوص» offers «کباب زعفرانی»
     * and then «کباب». Returning only one candidate is not enough: the user's
     * extra words are exactly what made the search fail, so a single drop can
     * still land on zero rows and leave the promised "شاید این‌ها" list empty.
     * The caller tries them in order and takes the first that returns rows.
     */
    fun relaxedCandidates(query: String): List<String> {
        val tokens = query.trim().split(' ', '‌').filter { it.isNotBlank() }
        if (tokens.size < 2) return emptyList()
        // dropLast then dropLast(2) … down to a single token.
        return (1 until tokens.size).map { drop -> tokens.dropLast(drop).joinToString(" ") }
    }

    /**
     * The single best relaxation, kept for callers that only need a label
     * (never to decide whether a suggestion exists).
     */
    fun relaxed(query: String): String? = relaxedCandidates(query).firstOrNull()

    /**
     * True when [relaxedQuery] is worth offering: it must differ from the
     * original, otherwise the UI would show a button that re-runs the same
     * no-result search.
     */
    fun shouldOfferRelaxed(query: String, resultCount: Int): Boolean =
        resultCount == 0 && relaxed(query) != null

    /**
     * Onboarding hands off into a REAL action, not a dead last page (#126).
     *
     * Picks one of [SAMPLE_PICKS] — seeded ids, never random. A random pick
     * would be re-rolled on every recomposition, so the «نمونه» row under the
     * user would flicker; deterministic also keeps the same dish on a retry.
     */
    fun samplePick(variation: Int = 0): Long = SAMPLE_PICKS[variation.mod(SAMPLE_PICKS.size)]

    /**
     * Stable dish ids present in `assets/seed/foods/`. Hard-coded on purpose:
     * a baked catalogue is a shipped contract, and a lookup by name would
     * break the moment a seed is renamed.
     */
    private val SAMPLE_PICKS = longArrayOf(1L, 90L, 143L, 3L)
}
