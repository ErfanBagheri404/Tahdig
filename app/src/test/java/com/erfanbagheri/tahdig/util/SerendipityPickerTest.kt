package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SerendipityPickerTest {

    private val DAY = 86_400_000L
    private val now = 1_000L * DAY // arbitrary fixed clock

    private fun food(id: Long, blocked: Boolean = false) = FoodEntity(
        id = id,
        name = "غذا$id",
        nameEn = "food$id",
        categoryId = 1,
        mealTime = "LUNCH",
        cuisine = "IRANI",
        difficulty = "EASY",
        prepTimeMin = 30,
        ingredients = "پیاز",
        isBlocked = blocked,
    )

    @Test
    fun `untouched dishes are preferred over forgotten ones`() {
        val foods = listOf(food(1), food(2), food(3))
        // 1 was cooked long ago, 2 was favourited, 3 is untouched
        val pool = SerendipityPicker.pool(
            allFoods = foods,
            cookedAt = mapOf(1L to now - 200 * DAY),
            favoritedIds = setOf(2L),
            now = now,
        )
        assertEquals(listOf(3L), pool.map { it.id })
    }

    @Test
    fun `favourited dishes are excluded from the untouched pool`() {
        val foods = listOf(food(1), food(2))
        val pool = SerendipityPicker.pool(
            allFoods = foods,
            cookedAt = emptyMap(),
            favoritedIds = setOf(1L, 2L),
            now = now,
        )
        assertTrue(pool.isEmpty())
    }

    @Test
    fun `falls back to stale dishes when nothing is untouched`() {
        val foods = listOf(food(1), food(2), food(3))
        // 1 stale (200d), 2 fresh (5d), 3 stale (100d)
        val pool = SerendipityPicker.pool(
            allFoods = foods,
            cookedAt = mapOf(1L to now - 200 * DAY, 2L to now - 5 * DAY, 3L to now - 100 * DAY),
            favoritedIds = emptySet(),
            now = now,
        )
        // stalest first, fresh dish excluded
        assertEquals(listOf(1L, 3L), pool.map { it.id })
    }

    @Test
    fun `a dish exactly at the stale boundary counts as forgotten`() {
        val pool = SerendipityPicker.pool(
            allFoods = listOf(food(1)),
            cookedAt = mapOf(1L to now - SerendipityPicker.STALE_DAYS * DAY),
            favoritedIds = emptySet(),
            now = now,
        )
        assertEquals(listOf(1L), pool.map { it.id })
    }

    @Test
    fun `blocked dishes never enter the pool`() {
        val foods = listOf(food(1, blocked = true), food(2, blocked = true))
        val untouched = SerendipityPicker.pool(foods, emptyMap(), emptySet(), now)
        assertTrue(untouched.isEmpty())

        val stale = SerendipityPicker.pool(
            foods,
            mapOf(1L to now - 500 * DAY, 2L to now - 500 * DAY),
            emptySet(),
            now,
        )
        assertTrue(stale.isEmpty())
    }

    @Test
    fun `pick is stable for the same day and rotates across days`() {
        val pool = listOf(food(1), food(2), food(3))
        assertEquals(SerendipityPicker.pickForDay(pool, 10)!!.id, SerendipityPicker.pickForDay(pool, 10)!!.id)
        assertEquals(1L, SerendipityPicker.pickForDay(pool, 0)!!.id)
        assertEquals(2L, SerendipityPicker.pickForDay(pool, 1)!!.id)
        assertEquals(3L, SerendipityPicker.pickForDay(pool, 2)!!.id)
        // dayOfYear can exceed pool size — wraps without throwing
        assertEquals(1L, SerendipityPicker.pickForDay(pool, 3)!!.id)
    }

    @Test
    fun `pick on an empty pool is null`() {
        assertNull(SerendipityPicker.pickForDay(emptyList(), 42))
    }

    @Test
    fun `reason says never tried when the dish has no cook timestamp`() {
        assertEquals("هرگز امتحانش نکرده‌ای", SerendipityPicker.reason(food(1), emptyMap(), now))
    }

    @Test
    fun `reason reports whole days since the last cook`() {
        val cooked = mapOf(1L to now - 90 * DAY)
        assertEquals("90 روزه پختش ندادی", SerendipityPicker.reason(food(1), cooked, now))
    }

    @Test
    fun `reason handles a dish cooked today`() {
        assertEquals("امروز پختیش", SerendipityPicker.reason(food(1), mapOf(1L to now), now))
    }
}
