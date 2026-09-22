package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Preset serialization round-trip (#87 acceptance: save → change → apply restores).
 */
class FilterPresetCodecTest {

    private val full = FilterPresetPayload(
        categoryId = 7L,
        diet = DietFilter.NO_GLUTEN.name,
        ingredients = "پیاز,رب",
        excluded = "فلفل",
        time = TimeBucket.UNDER_30.name,
        difficulty = DifficultyFilter.EASY.name,
        cuisine = "GILAKI",
        sort = SortOrder.RATING_DESC.name,
    )

    @Test
    fun `round-trip returns the identical payload`() {
        val decoded = FilterPresetCodec.decode(FilterPresetCodec.encode(full))
        assertEquals(full, decoded)
    }

    @Test
    fun `all filter axes survive the trip`() {
        val p = FilterPresetCodec.decode(FilterPresetCodec.encode(full))!!
        val r = FilterPresetCodec.resolve(p)
        assertEquals(7L, r.categoryId)
        assertEquals(DietFilter.NO_GLUTEN, r.diet)
        assertEquals("پیاز,رب", r.ingredients)
        assertEquals("فلفل", r.excluded)
        assertEquals(TimeBucket.UNDER_30, r.time)
        assertEquals(DifficultyFilter.EASY, r.difficulty)
        assertEquals("GILAKI", r.cuisine)
        assertEquals(SortOrder.RATING_DESC, r.sort)
    }

    @Test
    fun `defaults payload is all-unconstrained`() {
        val r = FilterPresetCodec.resolve(FilterPresetPayload())
        assertNull(r.categoryId); assertNull(r.diet); assertNull(r.cuisine)
        assertEquals("", r.ingredients); assertEquals("", r.excluded)
        assertEquals(TimeBucket.ANY, r.time)
        assertEquals(DifficultyFilter.ANY, r.difficulty)
        assertEquals(SortOrder.SUGGESTED, r.sort)
    }

    @Test
    fun `unknown enum name from a future build degrades to no-constraint instead of crashing`() {
        val r = FilterPresetCodec.resolve(
            FilterPresetPayload(time = "UNDER_900", difficulty = "NIGHTMARE", sort = "MAGIC", diet = "KETO"),
        )
        assertEquals(TimeBucket.ANY, r.time)
        assertEquals(DifficultyFilter.ANY, r.difficulty)
        assertEquals(SortOrder.SUGGESTED, r.sort)
        assertNull(r.diet)
    }

    @Test
    fun `unknown extra json keys are ignored`() {
        val p = FilterPresetCodec.decode("""{"time":"UNDER_15","newAxis":{"x":1}}""")
        assertEquals(TimeBucket.UNDER_15, FilterPresetCodec.resolve(p!!).time)
    }

    @Test
    fun `corrupt payload returns null instead of throwing`() {
        assertNull(FilterPresetCodec.decode("not json"))
        assertNull(FilterPresetCodec.decode(""))
    }

    @Test
    fun `encode output is stable json the dao can store as text`() {
        val s = FilterPresetCodec.encode(FilterPresetPayload(time = TimeBucket.UNDER_30.name))
        assertTrue(s.contains("\"time\":\"UNDER_30\""))
        assertEquals(s, FilterPresetCodec.encode(FilterPresetCodec.decode(s)!!))
    }
}
