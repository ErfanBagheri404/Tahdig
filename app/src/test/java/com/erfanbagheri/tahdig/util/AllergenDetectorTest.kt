package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * #112: derivative map, registry lookup through quantity stripping, and
 * hide-filter composition. Uses a minimal registry fixture — pure JVM.
 */
class AllergenDetectorTest {

    @Before
    fun installFixture() {
        IngredientRegistry.installFromJson(
            """
            {"ingredients":[
              {"id":"flour","fa":"آرد","en":"flour","allergen":"گلوتن"},
              {"id":"milk","fa":"شیر","allergen":"لبنیات"},
              {"id":"eggs","fa":"تخم مرغ","allergen":"تخم‌مرغ"},
              {"id":"yogurt","fa":"ماست","allergen":"لبنیات"},
              {"id":"cream","fa":"خامه","en":"double cream","allergen":"لبنیات"},
              {"id":"oil","fa":"روغن"}
            ]}
            """.trimIndent(),
        )
    }

    @Test
    fun registryHit_stripsQuantityFirst() {
        // "۲ پیمانه آرد" must not defeat the alias lookup.
        assertEquals(setOf("گلوتن"), AllergenDetector.detect("۲ پیمانه آرد"))
        assertEquals(setOf("لبنیات"), AllergenDetector.detect("۱ لیوان شیر"))
    }

    @Test
    fun multipleIngredients_unionAllergens() {
        val hits = AllergenDetector.detect("۲ پیمانه آرد, ۱ لیوان شیر, ۳ عدد تخم مرغ")
        assertEquals(setOf("گلوتن", "لبنیات", "تخم‌مرغ"), hits)
    }

    @Test
    fun arabicDigitsAndSeparator_formsStillResolve() {
        assertEquals(setOf("لبنیات"), AllergenDetector.detect("٢ لیوان ماست"))
        assertEquals(
            setOf("گلوتن", "لبنیات"),
            AllergenDetector.detect("آرد و شیر"),
        )
    }

    @Test
    fun unknownLine_derivativeMapCatchesHiddenGluten() {
        assertEquals(setOf("گلوتن"), AllergenDetector.detect("مالت جو"))
        assertEquals(setOf("گلوتن"), AllergenDetector.detect("دکسترین ذرت"))
        assertEquals(setOf("لبنیات"), AllergenDetector.detect("شیرخشک"))
    }

    @Test
    fun englishGluedAmounts_resolveAfterStripping() {
        // Imported TheMealDB lines: number glued to unit, English name after.
        assertEquals(setOf("لبنیات"), AllergenDetector.detect("400ml double cream"))
        assertEquals(
            setOf("لبنیات", "گلوتن"),
            AllergenDetector.detect("400ml double cream, 200g flour"),
        )
        // Bare amount with no word still strips: "250ml شیر" → شیر.
        assertEquals(setOf("لبنیات"), AllergenDetector.detect("250ml شیر"))
    }

    @Test
    fun noData_producesNoWarning_conservative() {
        assertEquals(emptySet<String>(), AllergenDetector.detect(""))
        assertEquals(emptySet<String>(), AllergenDetector.detect("   "))
        assertEquals(emptySet<String>(), AllergenDetector.detect("کینوا، بادمجان"))
    }

    @Test
    fun conflicts_neverTrueWithEmptyProfile() {
        assertFalse(AllergenDetector.conflicts(emptySet(), setOf("لبنیات")))
        assertTrue(AllergenDetector.conflicts(setOf("لبنیات"), setOf("لبنیات", "گلوتن")))
        assertFalse(AllergenDetector.conflicts(setOf("سویا"), setOf("لبنیات")))
    }

    @Test
    fun hideComposition_requiresToggleAndConflict() {
        val profile = setOf("لبنیات")
        val dish = setOf("لبنیات")
        assertTrue(AllergenDetector.shouldHide(true, profile, dish))
        // Toggle off → dish stays visible (band still shows on detail).
        assertFalse(AllergenDetector.shouldHide(false, profile, dish))
        assertFalse(AllergenDetector.shouldHide(true, emptySet(), dish))
        assertFalse(AllergenDetector.shouldHide(true, profile, setOf("گلوتن")))
    }

    @Test
    fun profileOptions_matchSeedTaxonomy() {
        // Every option must be a value the seed can actually emit.
        val seed = setOf(
            "لبنیات", "گلوتن", "مغزها", "ماهی", "تخم‌مرغ", "کنجد", "صدف", "سویا",
        )
        assertEquals(seed, AllergenDetector.PROFILE_OPTIONS.toSet())
    }
}
