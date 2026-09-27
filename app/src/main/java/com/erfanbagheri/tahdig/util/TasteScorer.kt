package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

/**
 * «سلیقه» — the implicit score behind «the more you cook, the smarter it gets»
 * (#92). Replaces the flat 30%-favorited term in the home roll.
 *
 * Pure and static on purpose: the same shape as [StreakMath] and
 * [LeftoverRanker], so every weight is unit-testable without a database, and
 * the profile is only *read* here (reset lives in [SettingsStore]).
 *
 * Signals, in the order they are allowed to dominate:
 *
 *  1. **explicit rating** — the strongest thing the user ever told us, and the
 *     only one they were asked for. Full weight, half-life ~8 weeks so a
 *     three-month-old verdict still counts.
 *  2. **cooked count** — cooked it more, offer it more. Logarithmic: the 2nd
 *     time you cook something says far more than the 20th.
 *  3. **favorite** — a weaker cousin of the rating, which is why it is capped
 *     below the rating's own reach.
 *  4. **explicit dislike** — a hidden/blocked dish is a hard veto, not a
 *     penalty: hiding something and still having it suggested is the bug this
 *     line exists to prevent.
 *
 * Every term is a *multiplier* on a 0..1 base rather than a sum, so no single
 * signal can be drowned out by a long tail of trivial ones — the alternative
 * (adding raw counts) made cooked-count eventually outweigh a 1-star rating.
 */
object TasteScorer {

    /** Half-life of a rating, in days. ~8 weeks: opinions fade, they do not vanish. */
    const val RATING_HALF_LIFE_DAYS = 56.0

    /** How much a full 5-star rating can lift a dish, as a multiplier. */
    const val RATING_WEIGHT = 2.0

    /** Ceiling for the cooked-count term; reached after many cooks. */
    const val COOKED_WEIGHT = 0.6

    /** Cooks at which the cooked term is ~63% of the way to [COOKED_WEIGHT]. */
    const val COOKED_HALF_COOKS = 4.0

    /** A favorite's lift — deliberately below what a top rating can do. */
    const val FAVORITE_WEIGHT = 0.35

    /**
     * Multiplier applied to a dish the user hid.
     *
     * Small, not zero: [weightedPick] still can return it as a last resort, and
     * this keeps that outcome vanishingly unlikely. The absolute "never
     * suggest a hidden dish" rule is enforced where the pool is built
     * ([pool]), because a scorer multiplier cannot make something impossible.
     */
    const val DISLIKED_MULTIPLIER = 0.15

    /**
     * How much a loved *category* lifts its dishes, as a multiplier.
     *
     * The issue's acceptance says rating 3 dishes 5★ should raise «they and
     * their category» — a per-dish score alone cannot do that, and predicting a
     * dish from a sibling is the behavior that makes the feed feel like it
     * learns. Capped below [RATING_WEIGHT] so one loved category never buries
     * the rest of the catalog.
     */
    const val CATEGORY_WEIGHT = 0.5

    /**
     * Lift for a category ticked on the first-run picker (#92).
     *
     * Below [CATEGORY_WEIGHT] and well below the rating weight: this is a guess
     * the user made before tasting anything through the app, so the first real
     * signal must be able to outvote it.
     */
    const val PREFERRED_CATEGORY_WEIGHT = 0.3

    private const val DAY_MS = 86_400_000.0

    /** Per-dish signals the scorer needs. Every field optional — a dish with
     *  no signals scores 1.0, i.e. no change to today's behavior. */
    data class Signals(
        /** 1..5, or null when never rated. */
        val stars: Int? = null,
        /** Epoch ms of the most recent rating, null when never rated. */
        val ratedAt: Long? = null,
        /** How many times cooked. */
        val cookedCount: Int = 0,
        val favorited: Boolean = false,
        /** Hidden or blocked by the user. */
        val disliked: Boolean = false,
        /**
         * Mean star rating of the dish's *category*, 1..5, or null when the
         * user has never rated anything in it. Positive only: a category the
         * user dislikes is expressed by rating those dishes low, which already
         * reads through the per-dish term — doubling the signal here would let
         * one bad night suppress a whole cuisine.
         */
        val categoryStars: Float? = null,
        /**
         * The user ticked this dish's category on the first-run chip picker
         * (#92). A cold-start seed, before any rating exists — deliberately
         * weak so one bad dinner outweighs neither a rating nor a real habit.
         */
        val preferredCategory: Boolean = false,
    )

    /**
     * Multiplier for one dish, in (0, 2.x]. 1.0 means "no opinion yet".
     *
     * Time is injected as `now` rather than read from the clock so the decay is
     * testable and so a caller batching a whole feed passes one timestamp.
     */
    fun score(signals: Signals, now: Long): Double {
        if (signals.disliked) return DISLIKED_MULTIPLIER
        var s = 1.0
        s *= ratingTerm(signals, now)
        s *= cookedTerm(signals.cookedCount)
        s *= categoryTerm(signals.categoryStars)
        if (signals.preferredCategory) s += PREFERRED_CATEGORY_WEIGHT
        if (signals.favorited) s += FAVORITE_WEIGHT
        return max(s, 0.0)
    }

    /** Only above-neutral category ratings lift, by design (see [Signals]). */
    private fun categoryTerm(categoryStars: Float?): Double {
        val stars = categoryStars ?: return 1.0
        if (stars <= 3f) return 1.0
        val strength = ((stars - 3f) / 2f).coerceIn(0f, 1f)
        return 1.0 + strength * CATEGORY_WEIGHT
    }

    /**
     * The pool the feed may draw from: hidden dishes are removed outright.
     *
     * Separate from [score] because the issue's acceptance is absolute
     * («hide a dish -> never suggested»), and a multiplier — however small —
     * is a thing that can still happen. Filtering here makes the rule true by
     * construction instead of by weight.
     */
    fun pool(disliked: Boolean): Boolean = !disliked

    /**
     * Exponential decay toward 1.0: a 5★ is worth [RATING_WEIGHT] now and
     * approaches "no signal" as the rating ages, never crossing it.
     *
     * A rating with no timestamp is treated as fresh rather than ancient —
     * a missing timestamp means the feature predates the column, and
     * decaying it to nothing would silently erase real opinions.
     */
    private fun ratingTerm(signals: Signals, now: Long): Double {
        val stars = signals.stars ?: return 1.0
        if (stars !in 1..5) return 1.0
        // No timestamp => decay 1.0, i.e. treated as fresh. Returning 1.0 here
        // instead would silently discard a real rating, which is precisely the
        // data this scorer exists to use.
        val ageDays = signals.ratedAt
            ?.let { ((now - it) / DAY_MS).coerceAtLeast(0.0) }
            ?: 0.0
        val decay = exp(-ln(2.0) * ageDays / RATING_HALF_LIFE_DAYS)
        // stars 1..5 -> -1..+1, so a 3★ is neutral and never fights the base.
        val sentiment = (stars - 3) / 2.0
        return 1.0 + sentiment * RATING_WEIGHT * decay
    }

    /** Logarithmic cook count: 1 cook ≈ 0.35 of the term, 8 cooks ≈ 0.52. */
    private fun cookedTerm(count: Int): Double {
        if (count <= 0) return 1.0
        val x = ln(1.0 + count.toDouble()) / ln(1.0 + COOKED_HALF_COOKS)
        return 1.0 + minOf(x, 1.0) * COOKED_WEIGHT
    }

    /**
     * Weighted sample over a dish pool, replacing the old `if (random() < 0.30)`
     * coin flip.
     *
     * Scored selection *and* randomness: an earlier attempt ranked the pool and
     * took the top, which made the same dish appear for days. Here every dish
     * keeps a chance proportional to its score, so a 5★ is far likelier without
     * being a lock-in.
     */
    fun <T> weightedPick(
        items: List<T>,
        scoreOf: (T) -> Double,
    ): T? {
        if (items.isEmpty()) return null
        val scores = items.map { max(scoreOf(it), 0.0) }
        val total = scores.sum()
        // Every score zero (all disliked): fall back to uniform rather than
        // dividing by zero, so a heavily-disliked feed still returns something.
        if (total <= 0.0) return items[abs(items.hashCode()) % items.size]
        var roll = Math.random() * total
        items.indices.forEach { i ->
            roll -= scores[i]
            if (roll <= 0.0) return items[i]
        }
        return items.last()
    }

    /**
     * Blend a taste multiplier into an existing base weight.
     *
     * The base (meal-time fit, priority) is what *this moment* wants; the taste
     * score is what the user *usually* wants. Multiplying keeps both — a
     * perfect dinner dish the user dislikes never outranks its score.
     */
    fun blend(base: Double, taste: Double): Double = max(base, 0.0) * max(taste, 0.0)
}
