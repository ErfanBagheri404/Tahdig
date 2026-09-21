package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PantryMatcherTest {

    @Test
    fun `empty pantry covers nothing`() {
        assertEquals(0f, PantryMatcher.coverage("پیاز، برنج", emptyList()), 0.001f)
    }

    @Test
    fun `dish with no ingredients counts as fully covered`() {
        assertEquals(1f, PantryMatcher.coverage("", listOf("پیاز")), 0.001f)
    }

    @Test
    fun `all ingredients present is full coverage`() {
        val c = PantryMatcher.coverage("پیاز، برنج، لوبیا قرمز", listOf("پیاز", "برنج", "لوبیا قرمز"))
        assertEquals(1f, c, 0.001f)
        assertTrue(PantryMatcher.isCookable("پیاز، برنج، لوبیا قرمز", listOf("پیاز", "برنج", "لوبیا قرمز")))
    }

    @Test
    fun `partial coverage is fractional`() {
        val c = PantryMatcher.coverage("پیاز، برنج، لوبیا قرمز، گوشت", listOf("پیاز", "برنج"))
        assertEquals(0.5f, c, 0.001f)
        assertFalse(PantryMatcher.isCookable("پیاز، برنج، لوبیا قرمز، گوشت", listOf("پیاز", "برنج")))
    }

    @Test
    fun `substring match accepts the longer pantry term`() {
        // "پیاز داغ" in the recipe is satisfied by plain "پیاز" on hand.
        assertTrue(PantryMatcher.isCookable("پیاز داغ، برنج", listOf("پیاز", "برنج")))
    }

    @Test
    fun `farsi comma separates ingredients`() {
        assertEquals(1f, PantryMatcher.coverage("پیاز،برنج", listOf("پیاز", "برنج")), 0.001f)
    }

    @Test
    fun `ZWNJ variants match`() {
        // "قورمهسبزی" vs "قورمه سبزی" — normalization must collapse the difference.
        assertEquals(1f, PantryMatcher.coverage("قورمهسبزی", listOf("قورمه سبزی")), 0.001f)
    }

    @Test
    fun `multi-word pantry entry splits into usable terms`() {
        val c = PantryMatcher.coverage("پیاز، برنج", listOf("پیاز، برنج"))
        assertEquals(1f, c, 0.001f)
    }
}
