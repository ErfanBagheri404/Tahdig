package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutriLabelTest {

    // ── %DV ────────────────────────────────────────────────────────

    @Test
    fun percentDv_usesTheFdaReferences() {
        assertEquals(50, NutriLabel.percentDv(39.0, NutriLabel.DV.FAT_G))
        assertEquals(100, NutriLabel.percentDv(78.0, NutriLabel.DV.FAT_G))
        assertEquals(26, NutriLabel.percentDv(600.0, NutriLabel.DV.SODIUM_MG))
    }

    @Test
    fun percentDv_neverNegativeOrAbsurd() {
        assertEquals(0, NutriLabel.percentDv(0.0, NutriLabel.DV.FAT_G))
        assertEquals(0, NutriLabel.percentDv(-5.0, NutriLabel.DV.FAT_G))
        // A nonsense reference must not divide by zero.
        assertEquals(0, NutriLabel.percentDv(10.0, 0.0))
        assertEquals(999, NutriLabel.percentDv(1_000_000.0, NutriLabel.DV.FAT_G))
    }

    // ── Nutri-Score bands ──────────────────────────────────────────

    private fun facts(
        kj: Double = 0.0, sugars: Double = 0.0, satFat: Double = 0.0,
        salt: Double = 0.0, fiber: Double = 0.0, protein: Double = 0.0,
        fruitVeg: Int = 0,
    ) = NutriLabel.Facts(kj, sugars, satFat, salt, fiber, protein, fruitVeg)

    @Test
    fun waterLikeFood_scoresA() {
        assertEquals(NutriLabel.Score.A, NutriLabel.nutriScore(facts()))
    }

    @Test
    fun freshVegetables_scoreA_onFruitVegCredit() {
        // Low negatives, high fruit/veg → A even with modest fibre.
        assertEquals(
            NutriLabel.Score.A,
            NutriLabel.nutriScore(facts(kj = 200.0, fiber = 3.0, fruitVeg = 90)),
        )
    }

    @Test
    fun oliveOil_scoresC_orWorse() {
        // Pure fat: no fruit/veg credit, no fibre, no protein → energy + sat fat.
        val score = NutriLabel.nutriScore(
            facts(kj = 3400.0, satFat = 14.0, salt = 0.0),
        )
        assertTrue("expected C or worse, got $score", score.ordinal >= NutriLabel.Score.C.ordinal)
    }

    @Test
    fun sugaryDrinkLikeFood_scoresD() {
        // 1800 kJ (5) + 40 g sugars (10) = 15 negative, no positive credit → D.
        val score = NutriLabel.nutriScore(
            facts(kj = 1800.0, sugars = 40.0, satFat = 0.0, salt = 0.1),
        )
        assertEquals(NutriLabel.Score.D, score)
    }

    @Test
    fun worstCaseFood_scoresE() {
        // Score must clear 18: maxed energy, sugars, sat fat and salt.
        val score = NutriLabel.nutriScore(
            facts(kj = 3600.0, sugars = 60.0, satFat = 20.0, salt = 3.0),
        )
        assertEquals(NutriLabel.Score.E, score)
    }

    @Test
    fun proteinIsIgnoredOnceNegativesReachEleven() {
        // A very fatty, very salty food with high protein must NOT be rescued:
        // protein points are skipped at negative >= 11 (the 2023 rule).
        val fatty = facts(kj = 3400.0, sugars = 20.0, satFat = 12.0, salt = 2.5, protein = 30.0)
        val score = NutriLabel.nutriScore(fatty)
        assertTrue("protein wrongly credited: $score", score.ordinal >= NutriLabel.Score.D.ordinal)

        // The same food with trivial protein must not score BETTER.
        val withoutProtein = NutriLabel.nutriScore(fatty.copy(proteinG = 0.0))
        assertTrue(score.ordinal <= withoutProtein.ordinal)
    }

    @Test
    fun fiberAlwaysCounts_evenAtHighNegatives() {
        val base = facts(kj = 3400.0, sugars = 20.0, satFat = 12.0, salt = 2.5)
        val withFiber = NutriLabel.nutriScore(base.copy(fiberG = 6.0))
        val without = NutriLabel.nutriScore(base)
        assertTrue("fibre ignored at high negatives", withFiber.ordinal <= without.ordinal)
    }

    @Test
    fun scoreIsMonotonicInSugars() {
        val low = NutriLabel.nutriScore(facts(kj = 1200.0, sugars = 5.0))
        val high = NutriLabel.nutriScore(facts(kj = 1200.0, sugars = 30.0))
        assertTrue("more sugar scored better", high.ordinal >= low.ordinal)
    }

    // ── NOVA ───────────────────────────────────────────────────────

    @Test
    fun singleWholeFood_isGroupOne() {
        assertEquals(1, NutriLabel.nova("تخم‌مرغ"))
        assertEquals(1, NutriLabel.nova("سیب"))
    }

    @Test
    fun culinaryIngredient_isGroupTwo() {
        assertEquals(2, NutriLabel.nova("آرد، شکر، کره"))
        assertEquals(2, NutriLabel.nova("flour, sugar, butter"))
    }

    @Test
    fun preservedFood_isGroupThree() {
        assertEquals(3, NutriLabel.nova("ماهی دودی، نمک"))
        assertEquals(3, NutriLabel.nova("canned tuna, salt"))
    }

    @Test
    fun additiveMarkers_areGroupFour() {
        assertEquals(4, NutriLabel.nova("آرد، امولسیفایر، اسانس"))
        assertEquals(4, NutriLabel.nova("water, sugar, emulsifier, flavouring"))
        assertEquals(4, NutriLabel.nova("milk, modified starch"))
    }

    @Test
    fun unknownText_errsTowardLessProcessed() {
        // Conservative: no evidence of processing must not become "ultra".
        assertEquals(1, NutriLabel.nova(""))
        assertEquals(1, NutriLabel.nova("چیز نامعلوم"))
    }

    @Test
    fun novaLabels_areFarsiAndDistinct() {
        val labels = (1..4).map { NutriLabel.novaLabel(it) }
        assertEquals(4, labels.toSet().size)
        assertTrue(labels.none { it.isBlank() })
    }

    // ── filter composition ─────────────────────────────────────────

    @Test
    fun abFilter_keepsOnlyAB_andOffKeepsEverything() {
        assertTrue(NutriLabel.passesFilter(NutriLabel.Score.A, keepAB = true))
        assertTrue(NutriLabel.passesFilter(NutriLabel.Score.B, keepAB = true))
        assertFalse(NutriLabel.passesFilter(NutriLabel.Score.C, keepAB = true))
        assertFalse(NutriLabel.passesFilter(NutriLabel.Score.E, keepAB = true))
        // Filter off → every grade passes, so it composes as a no-op.
        NutriLabel.Score.values().forEach {
            assertTrue(NutriLabel.passesFilter(it, keepAB = false))
        }
    }
}
