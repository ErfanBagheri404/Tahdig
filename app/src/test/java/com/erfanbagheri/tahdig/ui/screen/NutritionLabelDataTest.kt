package com.erfanbagheri.tahdig.ui.screen

import com.erfanbagheri.tahdig.util.NutriLabel
import com.erfanbagheri.tahdig.util.NutritionDB
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The estimate/real split (#111) is the part that must never lie: a grade is
 * only produced when the inputs behind it are real.
 */
class NutritionLabelDataTest {

    @After
    fun reset() {
        // The registry is a singleton; leave it empty so other tests start clean.
        NutritionDB.reset()
    }

    private fun entry(
        fa: String,
        calories: Int,
        protein: Double = 0.0,
        fat: Double = 0.0,
        carbs: Double = 0.0,
        sugars: Double = 0.0,
        satFat: Double = 0.0,
        fiber: Double = 0.0,
        salt: Double = 0.0,
        kj: Double = 0.0,
    ) = fa to NutritionDB.Entry(
        calories = calories, protein = protein, fat = fat, carbs = carbs,
        sugars = sugars, saturatedFat = satFat, fiber = fiber, salt = salt,
        energyKj = kj,
    )

    private fun install(vararg pairs: Pair<String, NutritionDB.Entry>) {
        NutritionDB.reset()
        NutritionDB.installEntries(pairs.toMap())
    }

    // ── real path ──────────────────────────────────────────────────

    @Test
    fun coveredDish_producesScoresAndNoEstimateFlag() {
        install(
            entry("کره", 717, protein = 0.9, fat = 81.1, carbs = 0.1,
                sugars = 0.1, satFat = 51.4, salt = 0.03, kj = 3000.0),
            entry("آرد", 364, protein = 10.3, fat = 1.0, carbs = 76.3,
                sugars = 0.3, satFat = 0.2, fiber = 2.7, salt = 0.005, kj = 1523.0),
        )
        val label = NutritionLabelData.of("کیک", "دسر", "کره، آرد")
        assertTrue("should be real", !label.estimated)
        assertEquals(717 + 364, label.calories)
        assertNotNull("grade missing on real data", label.score)
        assertNotNull("nova missing on real data", label.nova)
        assertEquals(0.4, label.sugarG, 0.001)
        assertEquals(51.6, label.satFatG, 0.001)
        assertEquals(2.7, label.fiberG, 0.001)
    }

    @Test
    fun gradeIsOnlyShownWhenScoreInputsExist() {
        // Calories only — no sugars/sat-fat/salt anywhere. Scoring this would
        // read as an A for every dish, so the grade must stay null.
        install(
            entry("آرد", 364, protein = 10.3, fat = 1.0, carbs = 76.3),
            entry("برنج", 365, protein = 7.1, fat = 0.7, carbs = 80.0),
        )
        val label = NutritionLabelData.of("پلو", "پلو", "آرد، برنج")
        assertTrue("real table expected", !label.estimated)
        assertNull("grade invented without score inputs", label.score)
        assertNotNull("nova only needs the ingredient list", label.nova)
    }

    // ── estimate path ──────────────────────────────────────────────

    @Test
    fun uncoveredDish_fallsBackToEstimateWithNoBadges() {
        install()  // nothing covered
        val label = NutritionLabelData.of("کباب کوبیده", "کباب", "گوشت، پیاز")
        assertTrue("should be flagged estimated", label.estimated)
        assertTrue("estimate must carry calories", label.calories > 0)
        assertNull("no grade on an estimate", label.score)
        assertNull("no nova on an estimate", label.nova)
    }

    @Test
    fun singleCoveredIngredient_isNotEnoughToBeReal() {
        // MIN_COVERED = 2: one lucky hit must not turn the table "real".
        install(entry("کره", 717, protein = 0.9, fat = 81.1, carbs = 0.1))
        val label = NutritionLabelData.of("نان و کره", "نان", "کره، نان محلی")
        assertTrue(label.estimated)
    }

    @Test
    fun estimateStillCarriesHeuristicMacros() {
        install()
        val label = NutritionLabelData.of("سالاد شیرازی", "سبزیجات", "خیار، گوجه")
        assertTrue(label.estimated)
        // The heuristic is name-first: «سالاد» → 90 kcal, not the veg default.
        assertEquals(90, label.calories)
        assertTrue(label.fatG > 0)
        assertTrue(label.carbG >= 0)
    }

    @Test
    fun emptyIngredients_neverCrashes() {
        install()
        val label = NutritionLabelData.of("نامعلوم", "", "")
        assertTrue(label.estimated)
        assertTrue(label.calories >= 0)
        assertNull(label.score)
    }

    @Test
    fun farsiCommaSeparator_isUnderstood() {
        install(
            entry("کره", 717, protein = 0.9, fat = 81.1, carbs = 0.1,
                sugars = 0.1, satFat = 51.4, salt = 0.03, kj = 3000.0),
            entry("شکر", 385, carbs = 99.9, sugars = 99.8, kj = 1610.0),
        )
        // «،» rather than «,» must split the same way.
        val label = NutritionLabelData.of("شیرینی", "شیرینی", "کره، شکر")
        assertTrue(!label.estimated)
        assertEquals(717 + 385, label.calories)
        // Overwhelmingly sugary → a poor grade, never A.
        assertTrue(
            "sugar bomb graded too well: ${label.score}",
            label.score != NutriLabel.Score.A,
        )
    }
}
