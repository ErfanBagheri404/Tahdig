package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionEstimateTest {

    @Test
    fun `longest keyword wins`() {
        // "شیرینی" (400) is longer than "دسر" (350), so the dessert macro wins.
        val info = NutritionEstimate.estimate("حلوا", "دسر شیرینی")
        assertEquals(400, info.calories)
    }

    @Test
    fun `kebab has high protein`() {
        val info = NutritionEstimate.estimate("کباب کوبیده", "گوشتی")
        assertEquals(320, info.calories)
        assertEquals("24g", info.protein) // 320 * 0.30 / 4
    }

    @Test
    fun `unknown dish falls back to 250`() {
        val info = NutritionEstimate.estimate("چیز ناشناخته", "")
        assertEquals(250, info.calories)
    }

    @Test
    fun `salad is low calorie`() {
        val info = NutritionEstimate.estimate("سالاد شیرازی", "سبزیجات")
        assertEquals(90, info.calories)
    }

    @Test
    fun `carbs never negative`() {
        // If protein + fat ratios summed > 1 this would go negative — coerce to 0.
        val info = NutritionEstimate.estimate("املت", "")
        val carb = info.carb.removeSuffix("g").toInt()
        assertTrue(carb >= 0)
    }
}
