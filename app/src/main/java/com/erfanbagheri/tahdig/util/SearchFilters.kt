package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.FoodEntity

/** Prep-time ceiling filter. ANY = no constraint. */
enum class TimeBucket(val label: String, val maxMinutes: Int?) {
    ANY("زمان", null),
    UNDER_15("≤ ۱۵ دقیقه", 15),
    UNDER_30("≤ ۳۰ دقیقه", 30),
    UNDER_60("≤ ۶۰ دقیقه", 60);

    fun matches(prepTimeMin: Int): Boolean = maxMinutes == null || prepTimeMin <= maxMinutes
}

/** Skill filter. ANY = no constraint. */
enum class DifficultyFilter(val label: String, val value: String?) {
    ANY("سختی", null),
    EASY("آسان", "EASY"),
    MEDIUM("متوسط", "MEDIUM"),
    HARD("سخت", "HARD");

    fun matches(difficulty: String): Boolean =
        value == null || difficulty.equals(value, ignoreCase = true)
}

/** Result ordering. */
enum class SortOrder(val label: String) {
    SUGGESTED("پیشنهادی"),
    TIME_ASC("کوتاه‌ترین زمان"),
    RATING_DESC("بهترین امتیاز"),
    NEWEST("جدیدترین"),
    ALPHABETICAL("الفبا"),
}

/**
 * Filter + sort for search results. All pure so the whole surface is unit-testable
 * without a database or a Compose runtime.
 *
 * @param ratings foodId -> stars (1-5). Missing = unrated, sorts last.
 */
object SearchFilters {

    fun apply(
        foods: List<FoodEntity>,
        time: TimeBucket = TimeBucket.ANY,
        difficulty: DifficultyFilter = DifficultyFilter.ANY,
        cuisine: String? = null,
        sort: SortOrder = SortOrder.SUGGESTED,
        ratings: Map<Long, Int> = emptyMap(),
    ): List<FoodEntity> {
        val filtered = foods.filter { food ->
            time.matches(food.prepTimeMin) &&
                difficulty.matches(food.difficulty) &&
                (cuisine == null || food.cuisine.equals(cuisine, ignoreCase = true))
        }
        return when (sort) {
            // Keeps the ranking the DB already produced (meal-time/recommendation order).
            SortOrder.SUGGESTED -> filtered
            SortOrder.TIME_ASC -> filtered.sortedBy { it.prepTimeMin }
            // Unrated dishes sink below rated ones; ties break on the existing order.
            SortOrder.RATING_DESC -> filtered.sortedByDescending { ratings[it.id] ?: 0 }
            SortOrder.NEWEST -> filtered.sortedByDescending { it.id }
            SortOrder.ALPHABETICAL -> filtered.sortedBy { PersianText.normalize(it.name) }
        }
    }

    /** True when any filter (not sort) is active — drives the "clear all" affordance. */
    fun hasActiveFilters(
        time: TimeBucket,
        difficulty: DifficultyFilter,
        cuisine: String?,
    ): Boolean = time != TimeBucket.ANY || difficulty != DifficultyFilter.ANY || cuisine != null
}
