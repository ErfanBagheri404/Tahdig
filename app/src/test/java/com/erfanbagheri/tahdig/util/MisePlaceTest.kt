package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Mise-en-place identity (#99): a check must survive serving scaling — that is the
 * whole acceptance criterion.
 */
class MisePlaceTest {

    @Test
    fun `hash is stable across scaled servings`() {
        val blob = "۲ پیمانه آرد، ۳ عدد تخم‌مرغ، نمک"
        val one = MisePlace.rowsFor(MisePlace.rowsOf(blob), 1.0).map { it.hash }
        val six = MisePlace.rowsFor(MisePlace.rowsOf(blob), 6.0).map { it.hash }
        assertEquals(one, six)
    }

    @Test
    fun `hash is stable when the scaled blob is re-parsed`() {
        // The displayed quantity changes (۲ -> ۱۲) but identity must not.
        val blob = "۲ پیمانه آرد"
        val scaled = ServingScaler.scale(blob, 6.0)
        assertNotEquals(blob, scaled) // premise: the display really did change
        assertEquals(
            MisePlace.rowsOf(blob).map { MisePlace.hashOf(it.item, it.unit) },
            MisePlace.rowsOf(scaled).map { MisePlace.hashOf(it.item, it.unit) },
        )
    }

    @Test
    fun `unit distinguishes two rows of the same ingredient`() {
        // "۲ پیمانه آرد" and "۱۰۰ گرم آرد" are separate prep rows — separate checks.
        assertNotEquals(
            MisePlace.hashOf("آرد", "پیمانه"),
            MisePlace.hashOf("آرد", "گرم"),
        )
    }

    @Test
    fun `alias variants fold into one checked state`() {
        // Canonical id match: checking «پیاز قرمز» checks «پیاز» — same ingredient.
        assertEquals(
            MisePlace.hashOf("پیاز", null),
            MisePlace.hashOf("پیاز قرمز", null),
        )
    }

    @Test
    fun `rows keep original order and spelling`() {
        val rows = MisePlace.rowsOf("۲ پیمانه آرد، پیاز، ۳ عدد تخم‌مرغ")
        assertEquals(listOf("آرد", "پیاز", "تخم‌مرغ"), rows.map { it.item })
        // Persian آ must survive — normalize is for matching, never display.
        assertTrue(rows[0].display().contains("آرد"))
    }

    @Test
    fun `blank and empty entries are dropped`() {
        assertTrue(MisePlace.rowsOf("").isEmpty())
        assertTrue(MisePlace.rowsOf("، ،").isEmpty())
        assertEquals(2, MisePlace.rowsOf("پیاز، ، سیر").size)
    }

    @Test
    fun `unknown ingredient still gets a stable text hash`() {
        // No glossary entry: hash falls back to normalized text, still stable.
        assertEquals(
            MisePlace.hashOf("ماده‌ناشناخته", null),
            MisePlace.hashOf("  ماده ناشناخته ", null),
        )
    }

    @Test
    fun `quantity is never part of the hash`() {
        assertEquals(
            MisePlace.hashOf(IngredientParser.parse("۲ پیمانه آرد").item, "پیمانه"),
            MisePlace.hashOf(IngredientParser.parse("۹ پیمانه آرد").item, "پیمانه"),
        )
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun installRegistry() {
            // Alias folding test needs the real glossary; install the seed table
            // directly (test-order independent — the static table is JVM-wide).
            IngredientRegistry.installFromJson(
                java.io.File("src/main/assets/seed/ingredients.json").readText(),
            )
        }
    }
}
