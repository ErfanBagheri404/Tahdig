package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.FoodEntity

/**
 * Picks one "from the archive" dish — never cooked, or not cooked in a long time.
 *
 * Deliberately the inverse of the home recommender: that one favours favourites and
 * recent hits, so the same handful of dishes keep resurfacing and the rest of the
 * catalog is dead weight. This one excludes anything the user has engaged with and
 * falls back to the stalest dish once nothing untouched remains.
 */
object SerendipityPicker {

    /** Days of silence after which a dish counts as forgotten rather than recently cooked. */
    const val STALE_DAYS = 90

    private const val MILLIS_PER_DAY = 86_400_000L

    /**
     * Candidate pool, in preference order:
     *  1. dishes never cooked and never favourited — the untouched archive
     *  2. if that is empty, dishes last cooked at least [STALE_DAYS] ago
     *
     * Blocked dishes are never candidates. The result is ordered so that the pick is
     * deterministic for a given day (see [pickForDay]).
     *
     * @param cookedAt foodId -> most recent cook timestamp (epoch millis)
     * @param favoritedIds dishes the user has starred
     */
    fun pool(
        allFoods: List<FoodEntity>,
        cookedAt: Map<Long, Long>,
        favoritedIds: Set<Long>,
        now: Long = System.currentTimeMillis(),
        staleDays: Int = STALE_DAYS,
    ): List<FoodEntity> {
        val visible = allFoods.filter { !it.isBlocked }
        val untouched = visible.filter { it.id !in cookedAt && it.id !in favoritedIds }
        if (untouched.isNotEmpty()) return untouched.sortedBy { it.id }

        val cutoff = now - staleDays.toLong() * MILLIS_PER_DAY
        return visible
            .filter { (cookedAt[it.id] ?: Long.MAX_VALUE) <= cutoff }
            .sortedBy { cookedAt[it.id] ?: 0L }
    }

    /**
     * Stable pick for a calendar day — same dish all day, changes at midnight, and
     * identical across restarts and processes (no randomness to re-roll).
     * Returns null only when the pool is empty.
     */
    fun pickForDay(pool: List<FoodEntity>, dayOfYear: Int): FoodEntity? =
        if (pool.isEmpty()) null else pool[dayOfYear % pool.size]

    /** Farsi reason line shown under the dish — explains why this one surfaced. */
    fun reason(food: FoodEntity, cookedAt: Map<Long, Long>, now: Long = System.currentTimeMillis()): String {
        val last = cookedAt[food.id] ?: return "هرگز امتحانش نکرده‌ای"
        val days = ((now - last) / MILLIS_PER_DAY).toInt()
        return if (days <= 0) "امروز پختیش" else "$days روزه پختش ندادی"
    }
}
