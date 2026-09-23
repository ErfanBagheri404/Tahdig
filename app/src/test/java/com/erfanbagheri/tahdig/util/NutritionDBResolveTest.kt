package com.erfanbagheri.tahdig.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The resolver decides whether a dish reaches real per-100g data at all, so
 * the normalisation and the alias fallback both need pinning.
 */
class NutritionDBResolveTest {

    @After
    fun reset() = NutritionDB.reset()

    private fun install(vararg keys: String) {
        NutritionDB.reset()
        NutritionDB.installEntries(
            keys.associateWith {
                NutritionDB.Entry(calories = 100, protein = 1.0, fat = 2.0, carbs = 3.0)
            },
        )
    }

    @Test
    fun stripsEnglishAmountAndUnit() {
        assertEquals("double cream", NutritionDB.normalize("400ml double cream"))
        // "caster sugar" is its own table key, so only the amount goes.
        assertEquals("caster sugar", NutritionDB.normalize("2 tbsp caster sugar"))
        assertEquals("onion", NutritionDB.normalize("1 large chopped onion"))
        assertEquals("flour", NutritionDB.normalize("1/2 cup plain flour"))
    }

    @Test
    fun stripsPersianAmountAndUnit() {
        assertEquals("آرد", NutritionDB.normalize("۲ پیمانه آرد"))
        assertEquals("کره", NutritionDB.normalize("100 گرم کره"))
    }

    @Test
    fun resolvesAfterStripping() {
        install("double cream")
        assertNotNull("amount prefix blocked the lookup", NutritionDB.get("400ml double cream"))
    }

    @Test
    fun resolvesFarsiIngredient() {
        install("آرد")
        assertNotNull(NutritionDB.get("آرد"))
    }

    @Test
    fun longestAliasWins() {
        install("گوشت", "گوشت گاو")
        // «گوشت گاو» (beef) must not collapse onto the generic «گوشت» row.
        assertEquals(100, NutritionDB.get("گوشت گاو")?.calories)
        assertEquals(100, NutritionDB.get("گوشت")?.calories)
    }

    @Test
    fun wordBoundaryStopsFalsePrefix() {
        install("cream")
        // "creamed coconut" must not resolve to plain cream.
        assertNull(NutritionDB.get("creamed coconut"))
    }

    @Test
    fun unknownIngredientStaysUnknown() {
        install("آرد")
        assertNull("invented a match for an unknown ingredient", NutritionDB.get("زعفران دودی"))
    }

    @Test
    fun emptyAndBlankAreNull() {
        install("آرد")
        assertNull(NutritionDB.get(""))
        assertNull(NutritionDB.get("   "))
    }

    @Test
    fun trailingQualifiersDoNotBlockMatch() {
        install("onion")
        assertNotNull(NutritionDB.get("chopped onion"))
        assertNotNull(NutritionDB.get("onion, finely sliced"))
    }
}
