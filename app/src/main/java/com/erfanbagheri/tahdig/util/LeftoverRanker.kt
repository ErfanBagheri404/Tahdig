package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.FoodEntity

/**
 * «غذای مونده دارم» — ranks dishes that consume what is already cooked (#107).
 *
 * Distinct from [PantryMatcher]: the pantry is a persisted staple list, this is a
 * single-shot session over *cooked components* (برنج، مرغ سرخ‌شده) whose whole point
 * is to be eaten soon. Hence the three-part score, in this strict order:
 *
 * 1. **coverage** — how many of the entered leftovers the dish uses (weight 100)
 * 2. **minimal extra** — how few additional items it needs (weight 10 each)
 * 3. **urgency** — how close a *used* leftover is to its consume-by date (0..9)
 *
 * The weights are spaced so a step-1 win can never be overturned by step-2 or
 * step-3 (100 > any sum of 10s within one dish), which is what the issue's
 * "scored by (a), (b), (c)" ordering means in practice.
 *
 * ponytail: no quantities — the seed stores free-text amounts with no gram basis,
 * so "uses the rice you have" is presence, not amount (same ceiling as [MissingDiff]).
 */
object LeftoverRanker {

    /** Default consume-by window: a cooked component is best used within 2 days. */
    const val CONSUME_BY_DAYS = 2L

    private const val DAY_MS = 86_400_000L
    private const val W_COVERAGE = 100.0
    private const val W_MISSING = 10.0
    private const val W_URGENCY_MAX = 9.0

    /** One entered component, with when it was cooked (for urgency). */
    data class Leftover(val text: String, val cookedAtMs: Long? = null)

    /** A ranked dish plus the diff the result row shows. */
    data class Scored(
        val food: FoodEntity,
        /** Entered components this dish consumes — the «از اینا استفاده کن» hits. */
        val used: List<String>,
        /** Items the dish still needs (swap-aware, straight from [MissingDiff]). */
        val missing: List<String>,
        /** How many recipe items the leftovers cover — score term (a). */
        val covered: Int,
        val score: Double,
    ) {
        val extraCount: Int get() = missing.size

        /** The AC's diff line: «با ۱ ماده دیگه درست میشه». */
        fun headline(): String = if (missing.isEmpty()) "با همین مواد درست میشه"
        else "با ${PersianText.toPersianDigits(missing.size)} ماده دیگه درست میشه"
    }

    /**
     * Rank [allFoods] against [leftovers], best first.
     *
     * Coverage and the missing list both come from [MissingDiff] so this screen
     * and the detail screen can never disagree about what is covered; the local
     * overlap only attributes WHICH entered component was used (for the row's
     * «از اینا استفاده کن» list) and drives the urgency term.
     *
     * @param now current time in ms — passed in so the ranking is deterministic in tests.
     */
    fun rank(
        leftovers: List<Leftover>,
        allFoods: List<FoodEntity>,
        now: Long,
        limit: Int = 20,
    ): List<Scored> {
        val entered = leftovers
            .map { it to key(it.text) }
            .filter { it.second.isNotEmpty() }
        if (entered.isEmpty()) return emptyList()
        val texts = entered.map { it.first.text }

        return allFoods
            .mapNotNull { food -> score(food, entered, texts, now) }
            .sortedWith(compareByDescending<Scored> { it.score }.thenBy { it.food.name })
            .take(limit)
    }

    private fun score(
        food: FoodEntity,
        entered: List<Pair<Leftover, String>>,
        texts: List<String>,
        now: Long,
    ): Scored? {
        val diff = MissingDiff.diff(food.ingredients, texts)
        val covered = diff.covered.size
        if (covered == 0) return null

        val items = MissingDiff.itemsOf(food.ingredients)
        val used = mutableListOf<String>()
        var urgency = 0.0
        for ((leftover, leftKey) in entered) {
            if (items.any { matches(leftKey, key(it)) }) {
                used += leftover.text
                urgency += urgencyOf(leftover, now)
            }
        }

        val score = covered * W_COVERAGE -
            diff.missing.size * W_MISSING +
            urgency
        return Scored(food, used, diff.missing.map { it.display }, covered, score)
    }

    /** 0 for a fresh component, rising to [W_URGENCY_MAX] at the consume-by date. */
    private fun urgencyOf(leftover: Leftover, now: Long): Double {
        val cookedAt = leftover.cookedAtMs ?: return 0.0
        val ageDays = (now - cookedAt).toDouble() / DAY_MS
        val frac = (ageDays / CONSUME_BY_DAYS).coerceIn(0.0, 1.0)
        return frac * W_URGENCY_MAX
    }

    /** Same overlap rule as [LeftoverMatcher] and [PantryMatcher] — deliberately. */
    private fun matches(a: String, b: String): Boolean =
        a.isNotEmpty() && b.isNotEmpty() && (a.contains(b) || b.contains(a))

    private fun key(raw: String): String =
        PersianText.normalize(raw).filterNot { it.isWhitespace() }
}
