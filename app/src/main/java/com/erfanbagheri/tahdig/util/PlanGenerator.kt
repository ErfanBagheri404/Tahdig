package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.FoodEntity

/**
 * Fills a week's meal plan from the catalog — the "tweak a generated plan" flow that
 * beats staring at an empty 21-slot grid.
 *
 * Pure and seeded, so a given (week, seed) always produces the same plan: the generator
 * is testable, and a regenerate that the user dislikes can be reproduced.
 */
object PlanGenerator {

    /** The three slots the plan screen shows, in display order — matches MealPlanScreen's MEALS. */
    val SLOTS = listOf("صبحانه", "ناهار", "شام")

    /** Display slot -> the English mealTime bucket stored on FoodEntity. */
    private val SLOT_BUCKET = mapOf(
        "صبحانه" to "BREAKFAST",
        "ناهار" to "LUNCH",
        "شام" to "DINNER",
    )

    const val DAYS = 7

    /** Max dishes cooked recently that we actively steer away from. */
    private const val RECENT_AVOID = 15

    /** Time budget for the generated week. */
    enum class TimeBudget(val label: String, val maxMinutes: Int?) {
        ANY("هر زمانی", null),
        WEEKDAY_FAST("شب‌های سریع", 30),
        QUICK_ALL("همه سریع", 45);

        fun allows(prepTimeMin: Int): Boolean = maxMinutes == null || prepTimeMin <= maxMinutes
    }

    /**
     * @param foods        full catalog (blocked dishes are excluded here)
     * @param favoritesIds dishes to favour — mirrors the home recommender's weighting
     * @param recentIds    recently cooked dishes — avoided when alternatives exist
     * @param budget       prep-time ceiling
     * @param diets        dietary filters that must all match (reuses [DietFilter])
     * @param keepSlots    slots the caller wants left alone, as dayIndex to slot
     * @param seed         deterministic seed — use the week number so a week is stable
     */
    fun generate(
        foods: List<FoodEntity>,
        favoritesIds: Set<Long> = emptySet(),
        recentIds: List<Long> = emptyList(),
        budget: TimeBudget = TimeBudget.ANY,
        diets: List<DietFilter> = emptyList(),
        keepSlots: Set<Pair<Int, String>> = emptySet(),
        seed: Long = 0L,
    ): Map<Pair<Int, String>, Long> {
        val avoid = recentIds.take(RECENT_AVOID).toSet()
        val eligible = foods.filter { food ->
            !food.isBlocked &&
                budget.allows(food.prepTimeMin) &&
                diets.all( { it.matches(food.tags) })
        }
        if (eligible.isEmpty()) return emptyMap()

        val rnd = java.util.Random(seed)
        val used = mutableSetOf<Long>()
        val plan = LinkedHashMap<Pair<Int, String>, Long>()

        // Favourites first so the weighting is visible in the output, but never a hard rule.
        val favouritePool = eligible.filter { it.id in favoritesIds }
        val restPool = eligible.filter { it.id !in favoritesIds }

        for (day in 0 until DAYS) {
            for (slot in SLOTS) {
                if (day to slot in keepSlots) continue

                val pool = slotPool(eligible, slot, budget)
                if (pool.isEmpty()) continue

                // Preference ladder: unused + not recent → unused → anything not recent → anything.
                val pick = pool.filter { it.id !in used && it.id !in avoid }.let { candidates ->
                    when {
                        candidates.isNotEmpty() -> weightedPick(candidates, favouritePool, rnd)
                        else -> null
                    }
                } ?: pool.filter { it.id !in used }.let { candidates ->
                    if (candidates.isNotEmpty()) weightedPick(candidates, favouritePool, rnd) else null
                } ?: pool.filter { it.id !in avoid }.randomOrNull(rnd)
                    ?: pool.randomOrNull(rnd)
                    ?: continue

                plan[day to slot] = pick.id
                used += pick.id
            }
        }
        return plan
    }

    /**
     * Slot-specific pool. FoodEntity.mealTime carries English buckets while the plan
     * screen stores Farsi slot labels, so the two meet here. When a slot has no dishes
     * of its own we fall back to the whole eligible set rather than leaving it empty.
     */
    private fun slotPool(
        eligible: List<FoodEntity>,
        slot: String,
        budget: TimeBudget,
    ): List<FoodEntity> {
        val bucket = SLOT_BUCKET[slot] ?: return eligible
        val matching = eligible.filter {
            it.mealTime.split(',').any { part -> part.trim().equals(bucket, ignoreCase = true) }
        }
        return matching.ifEmpty { eligible }
    }

    /** Favourites win 60% of the time, so the plan feels personal without being a loop. */
    private fun weightedPick(
        candidates: List<FoodEntity>,
        favouritePool: List<FoodEntity>,
        rnd: java.util.Random,
    ): FoodEntity? {
        val favCandidates = candidates.filter { c -> favouritePool.any { it.id == c.id } }
        return if (favCandidates.isNotEmpty() && rnd.nextDouble() < 0.60) {
            favCandidates[rnd.nextInt(favCandidates.size)]
        } else {
            candidates[rnd.nextInt(candidates.size)]
        }
    }

    private fun List<FoodEntity>.randomOrNull(rnd: java.util.Random): FoodEntity? =
        if (isEmpty()) null else this[rnd.nextInt(size)]
}
