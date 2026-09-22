package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Taste-tagging rules (#89). Fixtures are the acceptance cases: the ترش filter
 * must match sour dishes, sweets must never land in ترش, and the tagger is pure.
 */
class FlavorTaggerTest {

    @Test
    fun `sour keywords tag a dish ترش`() {
        val sour = FlavorTagger.tag(
            ingredients = "گوشت گوسفند، سماق، پیاز، زردچوبه",
            tags = "گوشتی,خورش",
            name = "خورش قیمه سماق",
        )
        assertTrue(Flavor.TURSH in sour)
    }

    @Test
    fun `lemon sour dish is tagged ترش`() {
        val sour = FlavorTagger.tag(ingredients = "غوره، گوشت، پیاز", name = "خورش غوره گوشت")
        assertTrue(Flavor.TURSH in sour)
    }

    @Test
    fun `sweets are never tagged ترش even with a sour note`() {
        // لیمو روی کیک — sweet dish wins; this is the AC's «excludes sweets».
        val cake = FlavorTagger.tag(
            ingredients = "آرد، شکر، تخم‌مرغ، لیمو",
            name = "کیک لیمویی",
            categoryName = "شیرینی",
        )
        assertFalse(Flavor.TURSH in cake)
        assertTrue(Flavor.SHIRIN in cake)
    }

    @Test
    fun `savory dish with a pinch of sugar is not swept into sweets`() {
        // زرشک پلو often takes a spoon of شکر, but لیمو/انار keeps it off the sweet path.
        val polo = FlavorTagger.tag(
            ingredients = "زرشک، شکر، برنج، مرغ، لیمو",
            name = "زرشک پلو",
            categoryName = "پلو و چلو",
        )
        assertFalse(Flavor.SHIRIN in polo)
        assertTrue(Flavor.TURSH in polo)
    }

    @Test
    fun `sweet category is a sweet regardless of ingredient text`() {
        // Category default (AC «category defaults»): id 19/20/21 dishes start شیرین.
        val halva = FlavorTagger.tag(ingredients = "آرد، روغن، گلاب", categoryName = "حلوا و دسر")
        assertEquals(setOf(Flavor.SHIRIN), halva)
    }

    @Test
    fun `hot spice level tags تند by delegating to SpiceProfile`() {
        val kebab = FlavorTagger.tag(ingredients = "گوشت، فلفل قرمز، پیاز", name = "کباب تند")
        assertTrue(Flavor.TOND in kebab)
    }

    @Test
    fun `grease markers tag چرب`() {
        val fried = FlavorTagger.tag(ingredients = "مرغ، روغن، سوخاری", name = "مرغ سوخاری")
        assertTrue(Flavor.CHORB in fried)
    }

    @Test
    fun `plain dish gets no taste tags`() {
        val plain = FlavorTagger.tag(ingredients = "برنج، نمک، زردچوبه", name = "برنج ساده")
        assertEquals(emptySet<Flavor>(), plain)
    }

    @Test
    fun `tagger is pure — same input yields the same set`() {
        val args = Triple("گوشت، سماق، پیاز", "گوشتی", "خورش قیمه سماق")
        val a = FlavorTagger.tag(args.first, args.second, args.third)
        val b = FlavorTagger.tag(args.first, args.second, args.third)
        assertEquals(a, b)
    }

    @Test
    fun `every flavor renders a Farsi label`() {
        for (f in Flavor.entries) {
            assertTrue("label blank for $f", f.label.isNotBlank())
        }
    }
}
