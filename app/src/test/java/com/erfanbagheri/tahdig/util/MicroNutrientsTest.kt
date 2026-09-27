package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.util.MicroNutrients.Badge
import com.erfanbagheri.tahdig.util.NutrientCaps.Nutrient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** %DV math and threshold-badge boundaries (#117). */
class MicroNutrientsTest {

    // ── %DV ─────────────────────────────────────────────────────────

    @Test
    fun dvPercentIsTheStandardRatio() {
        // 460 mg sodium of the 2300 mg DV is 20%.
        assertEquals(20, MicroNutrients.dvPercent(460.0, 2300.0))
        assertEquals(0, MicroNutrients.dvPercent(0.0, 2300.0))
        assertEquals(100, MicroNutrients.dvPercent(28.0, 28.0))
    }

    @Test
    fun missingDvIsNullNotZero() {
        // A zero bar would read as "none of this is good for you".
        assertNull(MicroNutrients.dvPercent(5.0, null))
        assertNull(MicroNutrients.dvPercent(5.0, 0.0))
    }

    // ── rows ────────────────────────────────────────────────────────

    @Test
    fun absentNutrientKeepsItsRowWithNullAmount() {
        val rows = MicroNutrients.rows(
            fiberG = 5.0, sodiumMg = 200.0, potassiumMg = null,
            calciumMg = 100.0, ironMg = null, vitDUg = null, b12Ug = null,
        )
        val byLabel = rows.associateBy { it.label }
        // Absent data keeps the row but carries no number — the «—» contract.
        assertNull("potassium imputed", byLabel["پتاسیم"]?.amount)
        assertNull("iron imputed", byLabel["آهن"]?.amount)
        assertNull("vitD imputed", byLabel["ویتامین D"]?.amount)
        assertNull("b12 imputed", byLabel["ویتامین B12"]?.amount)
        assertEquals(5.0, byLabel["فیبر"]?.amount)
        assertEquals(200.0, byLabel["سدیم"]?.amount)
    }

    @Test
    fun everyTrackedNutrientGetsARow() {
        assertEquals(
            setOf("فیبر", "سدیم", "پتاسیم", "کلسیم", "آهن", "ویتامین D", "ویتامین B12"),
            MicroNutrients.rows(null, null, null, null, null, null, null)
                .map { it.label }.toSet(),
        )
    }

    @Test
    fun rowCarriesUnitAndDv() {
        val row = MicroNutrients.rows(
            fiberG = 14.0, sodiumMg = null, potassiumMg = null,
            calciumMg = null, ironMg = null, vitDUg = null, b12Ug = null,
        ).first()
        assertEquals("گرم", row.unit)
        assertEquals(50, row.dvPercent)  // 14 / 28
    }

    @Test
    fun allAbsentStillYieldsTheFullTable() {
        assertEquals(7, MicroNutrients.rows(null, null, null, null, null, null, null).size)
    }

    // ── threshold badges ────────────────────────────────────────────

    @Test
    fun highFiberBoundaryIsInclusive() {
        assertTrue(Badge.HIGH_FIBER in MicroNutrients.badges(6.0, null, null))
        assertTrue(Badge.HIGH_FIBER !in MicroNutrients.badges(5.9, null, null))
    }

    @Test
    fun lowSodiumBoundaryIsInclusive() {
        assertTrue(Badge.LOW_SODIUM in MicroNutrients.badges(null, 140.0, null))
        assertTrue(Badge.LOW_SODIUM !in MicroNutrients.badges(null, 140.1, null))
    }

    @Test
    fun highIronBoundaryIsInclusive() {
        assertTrue(Badge.HIGH_IRON in MicroNutrients.badges(null, null, 3.6))
        assertTrue(Badge.HIGH_IRON !in MicroNutrients.badges(null, null, 3.5))
    }

    @Test
    fun absentDataEarnsNoBadge() {
        assertTrue(MicroNutrients.badges(null, null, null).isEmpty())
    }

    // ── daily accumulation (#117 AC) ───────────────────────────────

    @Test
    fun dailyTotalSumsMeasuredMeals() {
        val meals = listOf(
            mapOf(Nutrient.SODIUM to 300.0),
            mapOf(Nutrient.SODIUM to 450.0),
        )
        assertEquals(750.0, MicroNutrients.dailyTotal(meals, Nutrient.SODIUM), 0.001)
    }

    @Test
    fun mealsWithoutDataAreSkippedNotCountedAsZero() {
        // A null value means unknown. Counting it as zero would make a day of
        // estimates look like a healthy low-sodium day.
        val meals = listOf(
            mapOf(Nutrient.SODIUM to null),
            mapOf(Nutrient.SODIUM to 400.0),
        )
        assertEquals(400.0, MicroNutrients.dailyTotal(meals, Nutrient.SODIUM), 0.001)
    }

    @Test
    fun nothingMeasurableReportsMinusOneNotZero() {
        val meals = listOf(mapOf(Nutrient.SODIUM to null), mapOf(Nutrient.SODIUM to null))
        assertEquals(-1.0, MicroNutrients.dailyTotal(meals, Nutrient.SODIUM), 0.001)
        assertNull(MicroNutrients.dailyStatus(-1.0, 2300.0))
    }

    @Test
    fun emptyDayReportsMinusOne() {
        assertEquals(-1.0, MicroNutrients.dailyTotal(emptyList(), Nutrient.SODIUM), 0.001)
    }

    @Test
    fun dailyTotalTriStateAgainstCap() {
        // 2000 + 1000 = 3000 mg.
        val meals = listOf(mapOf(Nutrient.SODIUM to 2000.0), mapOf(Nutrient.SODIUM to 1000.0))
        val total = MicroNutrients.dailyTotal(meals, Nutrient.SODIUM)
        assertEquals(3000.0, total, 0.001)
        // 3000/2300 > 1 -> over. 3000/3500 = 0.857 -> warn band (>= 0.8, < 1).
        // 3000/5000 = 0.6 -> pass.
        assertEquals(NutrientCaps.Status.FAIL, MicroNutrients.dailyStatus(total, 2300.0))
        assertEquals(NutrientCaps.Status.WARN, MicroNutrients.dailyStatus(total, 3500.0))
        assertEquals(NutrientCaps.Status.PASS, MicroNutrients.dailyStatus(total, 5000.0))
    }

    @Test
    fun exactlyAtDailyCapIsPass() {
        val meals = listOf(mapOf(Nutrient.SODIUM to 2300.0))
        assertEquals(
            NutrientCaps.Status.PASS,
            MicroNutrients.dailyStatus(MicroNutrients.dailyTotal(meals, Nutrient.SODIUM), 2300.0),
        )
    }

    @Test
    fun multipleBadgesCanStack() {
        val badges = MicroNutrients.badges(fiberG = 8.0, sodiumMg = 100.0, ironMg = 4.0)
        assertEquals(setOf(Badge.HIGH_FIBER, Badge.LOW_SODIUM, Badge.HIGH_IRON), badges)
    }
}
