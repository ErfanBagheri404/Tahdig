package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AC #110: BMR/TDEE math, macro split, ring percentages.
 *
 * The reference numbers are hand-computed Mifflin-St Jeor, so a regression in
 * the formula fails here rather than silently shifting every user's budget.
 */
class DailyBudgetTest {

    private val p = DailyBudget.Profile(
        age = 30, weightKg = 70.0, heightCm = 175.0, activity = 1, goal = 1, hasGoal = true,
    )

    @Test
    fun `bmr matches Mifflin-St Jeor`() {
        // 10*70 + 6.25*175 - 5*30 + 5 = 700 + 1093.75 - 150 + 5 = 1648.75 -> 1648
        assertEquals(1648, DailyBudget.bmr(p))
    }

    @Test
    fun `tdee applies the activity factor`() {
        // 1648 * 1.375 = 2266
        assertEquals(2266, DailyBudget.tdee(p))
    }

    @Test
    fun `goal delta shifts the budget`() {
        assertEquals(2266, DailyBudget.budget(p))                                   // maintain
        assertEquals(1866, DailyBudget.budget(p.copy(goal = 0)))                    // cut -400
        assertEquals(2666, DailyBudget.budget(p.copy(goal = 2)))                    // gain +400
    }

    @Test
    fun `budget never drops below a safe floor`() {
        val tiny = DailyBudget.Profile(age = 90, weightKg = 30.0, heightCm = 140.0, goal = 0)
        assertTrue(DailyBudget.budget(tiny) >= 1200)
    }

    @Test
    fun `macro split is 30-40-30 in grams`() {
        val (protein, carb, fat) = DailyBudget.macroSplit(2000)
        assertEquals(150, protein) // 2000*0.30/4
        assertEquals(200, carb)    // 2000*0.40/4
        assertEquals(66, fat)      // 2000*0.30/9
    }

    @Test
    fun `ring fraction clamps to a full circle`() {
        assertEquals(0.5, DailyBudget.ringFraction(1000, 2000), 0.001)
        assertEquals(1.0, DailyBudget.ringFraction(5000, 2000), 0.001)
        assertEquals(0.0, DailyBudget.ringFraction(0, 2000), 0.001)
    }

    @Test
    fun `no-goal mode returns -1 instead of dividing by zero`() {
        assertEquals(-1.0, DailyBudget.ringFraction(500, 0), 0.001)
    }

    @Test
    fun `day card exposes four rings with a goal and totals-only without`() {
        val withGoal = NutritionDay(
            consumedCal = 500, consumedProtein = 30, consumedCarbs = 60, consumedFat = 20,
            targetCal = 2000, macroTarget = Triple(150, 200, 66),
        )
        assertTrue(withGoal.hasGoal)
        assertEquals(1500, withGoal.remainingCal)
        assertEquals(0.25, withGoal.rings[0], 0.001)

        val noGoal = NutritionDay(consumedCal = 500)
        assertTrue(!noGoal.hasGoal)
        assertEquals(listOf(-1.0, -1.0, -1.0, -1.0), noGoal.rings)
    }

    @Test
    fun `over-budget day reports a negative remaining`() {
        val d = NutritionDay(consumedCal = 2600, targetCal = 2000, macroTarget = Triple(1, 1, 1))
        assertEquals(-600, d.remainingCal)
        assertEquals(1.0, d.rings[0], 0.001)
    }
}
