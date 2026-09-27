package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * Drives the REAL bundled tables (glossary + substitutions) — a swap entry pointing at
 * an id that doesn't exist would never apply, and that is invisible without real data.
 */
class SubstitutionRegistryTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadRealTables() {
            val seed = File("src/main/assets/seed")
            IngredientRegistry.installFromJson(
                File(seed, "ingredients.json").readText()
            )
            SubstitutionRegistry.installFromJson(
                File(seed, "substitutions.json").readText()
            )
        }
    }

    @Test
    fun `tables load with a usable size`() {
        assertTrue("glossary empty", IngredientRegistry.size > 200)
        assertTrue("no substitutions", SubstitutionRegistry.size > 100)
    }

    @Test
    fun `strained yogurt substitutes for plain yogurt at ratio`() {
        // Acceptance: substitute ماست چکیده with ماست, quantities scale by ratio.
        val swaps = SubstitutionRegistry.swapsForName("ماست")
        assertTrue("no swaps for ماست", swaps.isNotEmpty())
        val greek = swaps.first { it.toName == "ماست چکیده" }
        assertEquals(1.0, greek.ratio, 0.0001)
        assertEquals("مقدار ماست چکیده برابر ماست است", greek.caveat.isNotEmpty(), true)
    }

    @Test
    fun `ratio scales an amount`() {
        val swap = SubstitutionRegistry.swapsForName("ماست").first { it.toName == "کشک" }
        // 2 units of ماست -> 1.0 of کشک at the table's 0.5 ratio.
        assertEquals(0.5, swap.ratio, 0.0001)
        assertEquals(1.0, 2.0 * swap.ratio, 0.0001)
    }

    @Test
    fun `unknown name has no swaps`() {
        assertTrue(SubstitutionRegistry.swapsForName("چیز ناشناخته").isEmpty())
    }

    @Test
    fun `every swap resolves to a real glossary id`() {
        // installFromJson drops ids missing from the glossary, so if a typo slipped
        // through generation this size would fall short of what the file declares.
        val file = File("src/main/assets/seed/substitutions.json")
        val declared = File("src/main/assets/seed/substitutions.json").readText()
            .split("\"from\"").size - 1
        assertTrue("dropped ${SubstitutionRegistry.size} entries", SubstitutionRegistry.size == declared)
    }

    @Test
    fun `availableSubstitutesFor only returns what the pantry holds`() {
        val yogurtId = IngredientRegistry.resolve("ماست")!!.id
        val strainedId = IngredientRegistry.resolve("ماست چکیده")!!.id
        // Pantry holds plain yogurt; a dish needs strained.
        val held = SubstitutionRegistry.availableSubstitutesFor(strainedId, setOf(yogurtId))
        assertTrue(held.contains(yogurtId))

        // Pantry holds nothing -> no phantom substitute.
        val none = SubstitutionRegistry.availableSubstitutesFor(strainedId, emptySet())
        assertTrue(none.isEmpty())
    }

    @Test
    fun `chickpea swap chain exists for pantry fallback`() {
        val swaps = SubstitutionRegistry.swapsForName("نخود")
        assertTrue(swaps.isNotEmpty())
        assertTrue(swaps.any { it.toName == "لوبیا" })
    }
}

class MissingDiffTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadRealTables() {
            val seed = File("src/main/assets/seed")
            IngredientRegistry.installFromJson(File(seed, "ingredients.json").readText())
            SubstitutionRegistry.installFromJson(File(seed, "substitutions.json").readText())
        }
    }

    @Test
    fun `pantry onion removes onion from the need set`() {
        // Acceptance: Pantry has پیاز -> dish needing پیاز shows no پیاز in need-set.
        val diff = MissingDiff.diff("پیاز، گوجه، تخم مرغ", pantry = listOf("پیاز"))
        assertFalse("onion must not be missing", diff.missing.any {
            IngredientRegistry.resolve(it.display)?.id == "onion"
        })
        assertEquals(2, diff.missing.size)
    }

    @Test
    fun `alias in the pantry covers the canonical need`() {
        // «پیاز قرمز» in the pantry satisfies a dish that lists plain «پیاز».
        val diff = MissingDiff.diff("پیاز", pantry = listOf("۲ عدد پیاز قرمز"))
        assertTrue(diff.allCovered)
    }

    @Test
    fun `approved substitute counts as covered`() {
        // Acceptance: Substitute marked-have counts as covered.
        // Dish needs ماست چکیده, pantry holds plain ماست (a registered swap).
        val diff = MissingDiff.diff("۱ پیمانه ماست چکیده", pantry = listOf("ماست"))
        assertTrue("substitute must cover the need", diff.allCovered)
        val need = diff.needs.single()
        assertEquals("ماست", need.viaSubstitute)
    }

    @Test
    fun `substitute coverage can be turned off for a strict diff`() {
        val diff = MissingDiff.diff("ماست چکیده", pantry = listOf("ماست"), allowSubstitutes = false)
        assertEquals(1, diff.missing.size)
        assertEquals(null, diff.needs.first().viaSubstitute)
    }

    @Test
    fun `a substitute the user also lacks is still missing`() {
        // Needs ماست چکیده; pantry holds neither it nor plain yogurt -> missing.
        val diff = MissingDiff.diff("ماست چکیده", pantry = listOf("برنج"))
        assertTrue(diff.missing.isNotEmpty())
        assertEquals(null, diff.needs.first().viaSubstitute)
    }

    @Test
    fun `unknown ingredients fall back to text matching`() {
        val diff = MissingDiff.diff("چیز عجیب", pantry = listOf("چیز عجیب"))
        assertTrue(diff.allCovered)
        assertEquals(null, diff.needs.first().canonicalId)
    }

    @Test
    fun `empty pantry means everything is missing`() {
        val diff = MissingDiff.diff("پیاز، سیر", pantry = emptyList())
        assertEquals(2, diff.missing.size)
        assertFalse(diff.allCovered)
    }

    @Test
    fun `empty ingredient list is trivially covered`() {
        val diff = MissingDiff.diff("", pantry = emptyList())
        assertTrue(diff.needs.isEmpty())
        assertTrue(diff.allCovered)
        assertEquals("چیزی لازم نیست", diff.summary())
    }

    @Test
    fun `summary reads like the detail line`() {
        val diff = MissingDiff.diff("زعفران، کشمش، پیاز", pantry = listOf("پیاز"))
        assertEquals("۲ ماده کم داری: زعفران، کشمش", diff.summary())
    }

    @Test
    fun `summary reports everything present`() {
        val diff = MissingDiff.diff("پیاز", pantry = listOf("پیاز"))
        assertEquals("همه‌چیز را داری", diff.summary())
    }

    @Test
    fun `itemsOf reuses the shopping parser`() {
        val items = MissingDiff.itemsOf("۲ پیمانه آرد، پیاز، ۱ حبه سیر")
        assertEquals(3, items.size)
        assertEquals("آرد", items[0])
        assertEquals("پیاز", items[1])
        assertEquals("سیر", items[2])
    }

    @Test
    fun `matcher counts a held substitute as coverage`() {
        // Dish wants ماست چکیده; pantry holds only plain ماست. A strict diff still calls
        // it missing, while the matcher's substitute expansion says cookable — that gap
        // is the whole point of wiring the swap table into coverage.
        val dish = "ماست چکیده"
        val pantry = listOf("ماست")
        val strict = MissingDiff.diff(dish, pantry, allowSubstitutes = false)
        assertEquals(1, strict.missing.size)
        assertEquals(1.0, PantryMatcher.coverage(dish, pantry).toDouble(), 0.0001)
        assertTrue(PantryMatcher.isCookable(dish, pantry))
    }

    @Test
    fun `matcher reports the same missing set as the diff`() {
        // With substitutes disabled the two must agree, or the list and the coverage
        // number would contradict each other on screen.
        val dish = "پیاز، ماست چکیده، زعفران"
        val pantry = listOf("پیاز")
        val strict = MissingDiff.diff(dish, pantry, allowSubstitutes = false)
        assertEquals(strict.missing.size, PantryMatcher.missing(dish, pantry).size)
    }
}
