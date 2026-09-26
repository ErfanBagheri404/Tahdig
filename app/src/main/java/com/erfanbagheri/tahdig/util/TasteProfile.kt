package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity

/**
 * Builds the [TasteScorer] view of the user from what is already in the
 * database (#92). No new tables: ratings, history and favorites already record
 * everything the score needs, and a separate "taste" store would be a second
 * copy of the same facts — free to drift, expensive to keep true.
 *
 * Read once per roll and held in memory: a roll happens on a button press, the
 * pool is small, and re-querying per candidate would be five queries per dish.
 */
object TasteProfile {

    /** Signals for one dish, plus its category's mean rating. */
    data class Snapshot(
        val byFoodId: Map<Long, TasteScorer.Signals>,
        val categoryMean: Map<Long, Float>,
        val preferredCategories: Set<Long> = emptySet(),
    ) {
        fun scoreOf(food: FoodEntity, now: Long): Double {
            // A dish with no signals row still gets its category's mean and the
            // picker's tick — that is the whole point of scoring a dish the user
            // has never touched.
            val known = byFoodId[food.id]
            return TasteScorer.score(
                signals = (known ?: TasteScorer.Signals()).copy(
                    categoryStars = categoryMean[food.categoryId],
                    preferredCategory = food.categoryId in preferredCategories,
                ),
                now = now,
            )
        }

        /**
         * Hidden dishes, by id. A dish with no signals row is by definition not
         * blocked — the id set only ever holds dishes something was recorded
         * against, so a missing row means "no opinion", not "hidden".
         */
        fun isDisliked(foodId: Long): Boolean = byFoodId[foodId]?.disliked == true

        companion object {
            val EMPTY = Snapshot(emptyMap(), emptyMap(), emptySet())
        }
    }

    /**
     * Read every signal in four queries, regardless of pool size.
     *
     * Ratings are keyed by food id; a dish with no row gets `stars = null` and
     * therefore a neutral score, which is exactly the pre-#92 behavior for a
     * user who has never rated anything.
     */
    suspend fun snapshot(
        db: TahdigDatabase,
        resetAt: Long = 0L,
        preferredCategories: Set<Long> = emptySet(),
    ): Snapshot {
        // Ratings written before a «بازنشانی سلیقه» are filtered out rather than
        // deleted: the user asked for the feed to forget, not for their notes
        // and stars to disappear (#92).
        val allRatings = db.ratingDao().allRatings()
        // Reset suppresses *dish-level* signals only. Category means still use
        // the pre-reset rows: a category the user has always loved is a fact
        // about their palate that «forget my taste» should not rewrite.
        val ratings = allRatings.filter { it.ratedAt == 0L || it.ratedAt >= resetAt }
        // History has no reset semantics of its own — a cook is a fact about
        // what happened, so the reset does not rewrite it.
        val cooked = db.historyDao().cookCounts()
        val favorites = db.favoriteDao().favoritedIds()
        val disliked = db.favoriteDao().blockedIds()

        val categoryMean = allRatings
            .filter { it.stars > 0 }
            .groupBy { r -> r.categoryId }
            .mapValues { (_, rows) -> rows.map { it.stars }.average().toFloat() }

        val ratingsByFood = ratings.associateBy { it.foodId }
        val cookedByFood = cooked.associate { it.foodId to it.cooks }
        val favoriteIds = favorites.toSet()
        val dislikedIds = disliked.toSet()

        // Union of every id any signal knows about: a dish with only a cook
        // count must still get a Signals row, or its history would be invisible.
        val ids = ratingsByFood.keys + cookedByFood.keys + favoriteIds + dislikedIds
        val byFoodId = ids.associateWith { id ->
            val rating = ratingsByFood[id]
            TasteScorer.Signals(
                stars = rating?.stars?.takeIf { it > 0 },
                ratedAt = rating?.ratedAt?.takeIf { it > 0 },
                cookedCount = cookedByFood[id] ?: 0,
                favorited = id in favoriteIds,
                disliked = id in dislikedIds,
                categoryStars = rating?.categoryId?.let { categoryMean[it] },
            )
        }
        return Snapshot(byFoodId, categoryMean, preferredCategories)
    }
}
