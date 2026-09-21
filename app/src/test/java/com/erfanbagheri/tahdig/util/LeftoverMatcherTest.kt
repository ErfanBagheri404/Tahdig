package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeftoverMatcherTest {

    private fun food(id: Long, name: String, ingredients: String) = FoodEntity(
        id = id,
        name = name,
        nameEn = "",
        categoryId = 1,
        mealTime = "LUNCH",
        cuisine = "IRANI",
        difficulty = "EASY",
        prepTimeMin = 30,
        ingredients = ingredients,
        tags = "",
        description = "",
        imageUrl = null,
        isBlocked = false,
    )

    private val all = listOf(
        food(1, "قورمه‌سبزی", "سبزی خورشتی، لوبیا قرمز، گوشت گوسفند، لیمو عمانی، پیاز، زردچوبه"),
        food(2, "قیمه", "گوشت گوسفند، لپه، پیاز، رب انار، زردچوبه"),
        food(3, "کباب کوبیده", "گوشت گوسفند، پیاز، زعفران"),
        food(4, "سالاد فصل", "گوجه، خیار، کاهو"),
        food(5, "فسنجان", "گردو، رب انار، مرغ، پیاز، شکر"),
    )

    @Test
    fun `finds dishes sharing two or more ingredients`() {
        val cooked = all[0] // قورمه‌سبزی
        val result = LeftoverMatcher.findLeftovers(cooked, all)
        val ids = result.map { it.id }
        // قیمه shares گوشت+پیاز+زردچوبه → 3
        assertTrue(ids.contains(2))
        // کباب کوبیده shares گوشت+پیاز → 2
        assertTrue(ids.contains(3))
        // سالاد shares 0 → not in list
        assertTrue(!ids.contains(4))
    }

    @Test
    fun `excludes the cooked dish itself`() {
        val cooked = all[0]
        val result = LeftoverMatcher.findLeftovers(cooked, all)
        assertTrue(result.none { it.id == cooked.id })
    }

    @Test
    fun `sorted by overlap count descending`() {
        val cooked = all[0]
        val result = LeftoverMatcher.findLeftovers(cooked, all)
        assertTrue(result.size >= 2)
        // First should have more shared than second
        val first = result[0]
        val firstShared = LeftoverMatcher.countShared(cooked, first)
        val secondShared = LeftoverMatcher.countShared(cooked, result[1])
        assertTrue(firstShared >= secondShared)
    }

    @Test
    fun `returns empty when fewer than minShared ingredients`() {
        val sparse = food(10, "ساده", "نمک")
        val result = LeftoverMatcher.findLeftovers(sparse, all, minShared = 2)
        assertTrue(result.isEmpty())
    }
}
