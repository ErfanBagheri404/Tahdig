package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFiltersTest {

    private fun food(
        id: Long,
        name: String = "غذا$id",
        prep: Int = 30,
        difficulty: String = "MEDIUM",
        cuisine: String = "IRANI",
    ) = FoodEntity(
        id = id,
        name = name,
        nameEn = "food$id",
        categoryId = 1,
        mealTime = "LUNCH",
        cuisine = cuisine,
        difficulty = difficulty,
        prepTimeMin = prep,
        ingredients = "پیاز، نمک",
    )

    private val sample = listOf(
        food(1, "قورمه‌سبزی", prep = 120, difficulty = "HARD", cuisine = "IRANI"),
        food(2, "املت", prep = 10, difficulty = "EASY", cuisine = "IRANI"),
        food(3, "پاستا", prep = 25, difficulty = "EASY", cuisine = "ITALIAN"),
        food(4, "آش رشته", prep = 45, difficulty = "MEDIUM", cuisine = "IRANI"),
        food(5, "سوشی", prep = 60, difficulty = "HARD", cuisine = "JAPANESE"),
    )

    // ── time buckets ────────────────────────────────────────────────

    @Test
    fun `time bucket boundaries are inclusive`() {
        assertTrue(TimeBucket.UNDER_15.matches(15))
        assertFalse(TimeBucket.UNDER_15.matches(16))
        assertTrue(TimeBucket.UNDER_30.matches(30))
        assertFalse(TimeBucket.UNDER_30.matches(31))
        assertTrue(TimeBucket.UNDER_60.matches(60))
        assertFalse(TimeBucket.UNDER_60.matches(61))
    }

    @Test
    fun `ANY time bucket matches every prep time`() {
        assertTrue(TimeBucket.ANY.matches(0))
        assertTrue(TimeBucket.ANY.matches(999))
    }

    @Test
    fun `under 30 keeps only dishes at or under 30 minutes`() {
        val out = SearchFilters.apply(sample, time = TimeBucket.UNDER_30)
        assertEquals(listOf(2L, 3L), out.map { it.id })
    }

    // ── difficulty + cuisine ────────────────────────────────────────

    @Test
    fun `difficulty filter is case insensitive`() {
        val out = SearchFilters.apply(sample, difficulty = DifficultyFilter.EASY)
        assertEquals(listOf(2L, 3L), out.map { it.id })
        assertTrue(DifficultyFilter.EASY.matches("easy"))
        assertTrue(DifficultyFilter.EASY.matches("EASY"))
    }

    @Test
    fun `cuisine filter is case insensitive and exact`() {
        val out = SearchFilters.apply(sample, cuisine = "italian")
        assertEquals(listOf(3L), out.map { it.id })
    }

    @Test
    fun `filters compose as AND`() {
        val out = SearchFilters.apply(
            sample,
            time = TimeBucket.UNDER_60,
            difficulty = DifficultyFilter.EASY,
            cuisine = "IRANI",
        )
        assertEquals(listOf(2L), out.map { it.id })
    }

    // ── sorting ─────────────────────────────────────────────────────

    @Test
    fun `time ascending sorts by prep time`() {
        val out = SearchFilters.apply(sample, sort = SortOrder.TIME_ASC)
        assertEquals(listOf(2L, 3L, 4L, 5L, 1L), out.map { it.id })
    }

    @Test
    fun `rating descending puts unrated dishes last`() {
        val ratings = mapOf(1L to 3, 2L to 5, 4L to 4)
        val out = SearchFilters.apply(sample, sort = SortOrder.RATING_DESC, ratings = ratings)
        assertEquals(listOf(2L, 4L, 1L, 3L, 5L), out.map { it.id })
    }

    @Test
    fun `newest sorts by descending id`() {
        val out = SearchFilters.apply(sample, sort = SortOrder.NEWEST)
        assertEquals(listOf(5L, 4L, 3L, 2L, 1L), out.map { it.id })
    }

    @Test
    fun `alphabetical sorts on normalized name`() {
        val out = SearchFilters.apply(sample, sort = SortOrder.ALPHABETICAL)
        val names = out.map { it.name }
        assertEquals(names.sortedBy { PersianText.normalize(it) }, names)
    }

    @Test
    fun `suggested preserves incoming order`() {
        val out = SearchFilters.apply(sample, sort = SortOrder.SUGGESTED)
        assertEquals(sample.map { it.id }, out.map { it.id })
    }

    // ── active-filter detection ─────────────────────────────────────

    @Test
    fun `hasActiveFilters is false for defaults only`() {
        assertFalse(SearchFilters.hasActiveFilters(TimeBucket.ANY, DifficultyFilter.ANY, null))
        assertTrue(SearchFilters.hasActiveFilters(TimeBucket.UNDER_15, DifficultyFilter.ANY, null))
        assertTrue(SearchFilters.hasActiveFilters(TimeBucket.ANY, DifficultyFilter.HARD, null))
        assertTrue(SearchFilters.hasActiveFilters(TimeBucket.ANY, DifficultyFilter.ANY, "IRANI"))
    }

    @Test
    fun `empty input yields empty output for every sort`() {
        SortOrder.values().forEach { sort ->
            assertTrue(SearchFilters.apply(emptyList(), sort = sort).isEmpty())
        }
    }
}
