package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.util.NutrientCaps.Nutrient
import com.erfanbagheri.tahdig.util.NutrientCaps.Preset
import com.erfanbagheri.tahdig.util.NutrientCaps.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tri-state boundaries and preset/custom composition (#113). */
class NutrientCapsTest {

    // ── boundaries ────────────────────────────────────────────────

    @Test
    fun wellUnderCapPasses() {
        assertEquals(Status.PASS, NutrientCaps.evaluate(100.0, 400.0))
    }

    @Test
    fun exactlyAtCapIsStillPass() {
        // A ceiling is "up to and including" — 400 of 400 is not a violation.
        assertEquals(Status.PASS, NutrientCaps.evaluate(400.0, 400.0))
    }

    @Test
    fun warnBandStartsAtEightyPercent() {
        assertEquals(Status.PASS, NutrientCaps.evaluate(319.9, 400.0))
        assertEquals(Status.WARN, NutrientCaps.evaluate(320.0, 400.0))
        assertEquals(Status.WARN, NutrientCaps.evaluate(399.0, 400.0))
    }

    @Test
    fun justOverCapFails() {
        assertEquals(Status.FAIL, NutrientCaps.evaluate(400.1, 400.0))
    }

    @Test
    fun zeroCapIsNoCap() {
        assertNull(NutrientCaps.evaluate(999.0, 0.0))
        assertNull(NutrientCaps.evaluate(999.0, -5.0))
    }

    // ── presets ───────────────────────────────────────────────────

    @Test
    fun hypertensionPresetCapsSodium() {
        val caps = NutrientCaps.merge(Preset.HYPERTENSION, emptyMap())
        assertEquals(setOf(Nutrient.SODIUM), caps.keys)
        assertEquals(400.0, caps.getValue(Nutrient.SODIUM), 0.001)
    }

    @Test
    fun kidneyPresetCapsPotassiumPhosphorusProtein() {
        val caps = NutrientCaps.merge(Preset.KIDNEY, emptyMap())
        assertEquals(
            setOf(Nutrient.POTASSIUM, Nutrient.PHOSPHORUS, Nutrient.PROTEIN),
            caps.keys,
        )
    }

    @Test
    fun everyPresetHasAtLeastOneCap() {
        Preset.entries.forEach { p ->
            assertTrue("preset $p has no caps", p.caps.isNotEmpty())
            assertTrue("preset $p has a non-positive cap", p.caps.values.all { it > 0.0 })
        }
    }

    // ── composition ───────────────────────────────────────────────

    @Test
    fun customCapOverridesPresetForSameNutrient() {
        val caps = NutrientCaps.merge(Preset.HYPERTENSION, mapOf(Nutrient.SODIUM to 250.0))
        assertEquals("preset overwrote the personal limit", 250.0, caps.getValue(Nutrient.SODIUM), 0.001)
    }

    @Test
    fun customCapsStackWithPresetForOtherNutrients() {
        val caps = NutrientCaps.merge(Preset.HYPERTENSION, mapOf(Nutrient.SUGAR to 15.0))
        assertEquals(2, caps.size)
        assertEquals(400.0, caps.getValue(Nutrient.SODIUM), 0.001)
        assertEquals(15.0, caps.getValue(Nutrient.SUGAR), 0.001)
    }

    @Test
    fun nonPositiveCustomCapIsIgnored() {
        val caps = NutrientCaps.merge(Preset.HYPERTENSION, mapOf(Nutrient.SUGAR to 0.0))
        assertEquals(setOf(Nutrient.SODIUM), caps.keys)
    }

    @Test
    fun noPresetAndNoCustomMeansNoCaps() {
        assertTrue(NutrientCaps.merge(null, emptyMap()).isEmpty())
    }

    // ── per-dish checks ───────────────────────────────────────────

    @Test
    fun checkSkipsNutrientsWithNoData() {
        // Potassium capped but absent from the amounts: must NOT be reported as
        // a comfortable zero, because absent data is not a low value.
        val caps = NutrientCaps.merge(Preset.KIDNEY, emptyMap())
        val checks = NutrientCaps.check(caps, mapOf(Nutrient.POTASSIUM to 120.0))
        assertEquals(1, checks.size)
        assertEquals(Nutrient.POTASSIUM, checks[0].nutrient)
    }

    @Test
    fun failingNutrientMakesAllWithinFalse() {
        val caps = NutrientCaps.merge(Preset.HYPERTENSION, emptyMap())
        assertTrue(NutrientCaps.allWithin(caps, mapOf(Nutrient.SODIUM to 200.0)))
        assertTrue(!NutrientCaps.allWithin(caps, mapOf(Nutrient.SODIUM to 500.0)))
    }

    @Test
    fun emptyCapsAcceptEverything() {
        assertTrue(NutrientCaps.allWithin(emptyMap(), mapOf(Nutrient.SODIUM to 9999.0)))
    }

    @Test
    fun describeRendersPersianDigitsAndUnit() {
        val caps = NutrientCaps.merge(Preset.HYPERTENSION, emptyMap())
        val line = NutrientCaps.check(caps, mapOf(Nutrient.SODIUM to 320.0)).first().describe()
        assertTrue("no Persian digits in: $line", line.contains("۳۲۰"))
        assertTrue("unit missing in: $line", line.contains("میلی‌گرم"))
        assertTrue("status label missing in: $line", line.contains(Status.WARN.label))
    }

    @Test
    fun statusCarriesASymbolSoColourIsNotTheOnlyCue() {
        Status.entries.forEach {
            assertTrue("$it has no symbol", it.symbol.isNotBlank())
            assertTrue("$it has no label", it.label.isNotBlank())
        }
    }
}
