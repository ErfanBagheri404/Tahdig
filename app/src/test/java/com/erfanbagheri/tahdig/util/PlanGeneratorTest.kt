package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanGeneratorTest {

    private fun food(
        id: Long,
        mealTime: String = "LUNCH,DINNER",
        prep: Int = 30,
        tags: String = "",
        blocked: Boolean = false,
    ) = FoodEntity(
        id = id,
        name = "غذا$id",
        nameEn = "food$id",
        categoryId = 1,
        mealTime = mealTime,
        cuisine = "IRANI",
        difficulty = "EASY",
        prepTimeMin = prep,
        ingredients = "پیاز",
        tags = tags,
        isBlocked = blocked,
    )

    /** A catalog big enough to fill a week without forced reuse. */
    private fun bigCatalog(n: Int = 40, prep: Int = 30) =
        (1L..n).map { food(it, prep = prep) }

    @Test
    fun `fills all 21 slots when the catalog is large enough`() {
        val plan = PlanGenerator.generate(bigCatalog(40), seed = 1L)
        assertEquals(21, plan.size)
        assertTrue(plan.keys.all { it.first in 0..6 })
        assertTrue(plan.keys.all { it.second in PlanGenerator.SLOTS })
    }

    @Test
    fun `never repeats a dish within one week`() {
        val plan = PlanGenerator.generate(bigCatalog(40), seed = 7L)
        assertEquals(plan.size, plan.values.toSet().size)
    }

    @Test
    fun `same seed produces the same plan`() {
        val a = PlanGenerator.generate(bigCatalog(40), seed = 42L)
        val b = PlanGenerator.generate(bigCatalog(40), seed = 42L)
        assertEquals(a, b)
    }

    @Test
    fun `different seeds produce different plans`() {
        val a = PlanGenerator.generate(bigCatalog(40), seed = 1L)
        val b = PlanGenerator.generate(bigCatalog(40), seed = 2L)
        assertTrue(a != b)
    }

    @Test
    fun `blocked dishes are never planned`() {
        val catalog = bigCatalog(30) + food(99, blocked = true) + food(98, blocked = true)
        val plan = PlanGenerator.generate(catalog, seed = 3L)
        assertTrue(99L !in plan.values)
        assertTrue(98L !in plan.values)
    }

    @Test
    fun `time budget excludes dishes over the ceiling`() {
        val catalog = (1L..40L).map { food(it, prep = if (it % 2 == 0L) 20 else 90) }
        val plan = PlanGenerator.generate(catalog, budget = PlanGenerator.TimeBudget.WEEKDAY_FAST, seed = 4L)
        val slow = catalog.filter { it.prepTimeMin > 30 }.map { it.id }.toSet()
        assertTrue(plan.values.none { it in slow })
    }

    @Test
    fun `impossible budget yields an empty plan instead of a partial lie`() {
        val catalog = (1L..10L).map { food(it, prep = 120) }
        val plan = PlanGenerator.generate(catalog, budget = PlanGenerator.TimeBudget.WEEKDAY_FAST, seed = 5L)
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `diet filters exclude non-matching dishes`() {
        val catalog = (1L..40L).map { food(it, tags = if (it % 2 == 0L) "وگن" else "") }
        val plan = PlanGenerator.generate(catalog, diets = listOf(DietFilter.VEGAN), seed = 6L)
        val nonVegan = catalog.filter { !it.tags.contains("وگن") }.map { it.id }.toSet()
        assertTrue(plan.values.none { it in nonVegan })
    }

    @Test
    fun `kept slots are left untouched`() {
        val keep = setOf(0 to "صبحانه", 3 to "شام")
        val plan = PlanGenerator.generate(bigCatalog(40), keepSlots = keep, seed = 8L)
        assertTrue(plan.keys.none { it in keep })
    }

    @Test
    fun `recently cooked dishes are avoided when alternatives exist`() {
        val catalog = bigCatalog(40)
        val recent = (1L..15L).toList()
        val plan = PlanGenerator.generate(catalog, recentIds = recent, seed = 9L)
        val avoid = recent.toSet()
        val plannedRecent = plan.values.count { it in avoid }
        // 40 dishes, 21 slots, only 15 banned — the ladder should keep reuse of recent ones minimal
        assertTrue("planned $plannedRecent recent dishes", plannedRecent <= 3)
    }

    @Test
    fun `favourites appear more often than chance would suggest`() {
        val catalog = bigCatalog(40)
        val favourites = setOf(1L, 2L, 3L) // 3 of 40 = 7.5% of the catalog
        val plan = PlanGenerator.generate(catalog, favoritesIds = favourites, seed = 11L)
        val favPlanned = plan.values.count { it in favourites }
        // expectation is ~2 slots if uniform; the 60% weighting should beat that
        assertTrue("favourites only got $favPlanned slots", favPlanned >= 3)
    }

    @Test
    fun `small catalog repeats rather than leaving slots empty`() {
        val plan = PlanGenerator.generate(bigCatalog(5), seed = 12L)
        assertEquals(21, plan.size)
        assertTrue(plan.values.toSet().size <= 5)
    }

    @Test
    fun `empty catalog yields an empty plan`() {
        assertTrue(PlanGenerator.generate(emptyList(), seed = 13L).isEmpty())
    }

    @Test
    fun `breakfast slot prefers breakfast dishes when present`() {
        val catalog = listOf(
            food(1, mealTime = "BREAKFAST"),
            food(2, mealTime = "BREAKFAST"),
        ) + (3L..30L).map { food(it, mealTime = "DINNER") }
        val plan = PlanGenerator.generate(catalog, seed = 14L)
        val breakfastIds = setOf(1L, 2L)
        val breakfastPicks = plan.filterKeys { it.second == "صبحانه" }.values
        assertEquals(7, breakfastPicks.size)
        assertTrue(breakfastPicks.all { it in breakfastIds })
    }

    @Test
    fun `slot labels are the Farsi labels the plan screen stores`() {
        assertEquals(listOf("صبحانه", "ناهار", "شام"), PlanGenerator.SLOTS)
    }

    @Test
    fun `plan values are all real dish ids from the catalog`() {
        val catalog = bigCatalog(30)
        val ids = catalog.map { it.id }.toSet()
        val plan = PlanGenerator.generate(catalog, seed = 15L)
        assertTrue(plan.values.all { it in ids })
        assertNotNull(plan)
    }
}
