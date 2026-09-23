package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #118 acceptance: a gelatin dish warns, ambiguous/missing data never flags,
 * and the matcher handles Persian and English strings plus derivatives.
 */
class HalalFlagsTest {

    // ── AC 1: a dish with gelatin shows the warning ─────────────────

    @Test
    fun gelatinDishIsFlagged() {
        assertEquals(listOf("ژلاتین حیوانی"), HalalFlags.flags("ژلاتین، شکر، آب"))
    }

    @Test
    fun gelatinEnglishIsFlagged() {
        assertEquals(listOf("ژلاتین حیوانی"), HalalFlags.flags("gelatin, sugar, water"))
    }

    @Test
    fun gelatinWithZwnjSuffixIsFlagged() {
        // «ژلاتین‌دار» — ZWNJ joins the word to a suffix, and normalize strips
        // it, so the key is «ژلاتیندار» and the whole-word match must fire.
        assertEquals(listOf("ژلاتین حیوانی"), HalalFlags.flags("ژلاتین‌دار"))
    }

    @Test
    fun porkDishIsFlagged() {
        assertEquals(listOf("گوشت خوک"), HalalFlags.flags("گوشت خوک، نمک"))
        assertEquals(listOf("گوشت خوک"), HalalFlags.flags("bacon, eggs"))
    }

    @Test
    fun alcoholIsFlaggedEvenWhenCooked() {
        // A wine sauce cooked for an hour still flags — cooking does not
        // reliably drive off alcohol, and the user asked to see it anyway.
        assertEquals(listOf("الکل"), HalalFlags.flags("شراب سفید، گوشت گوسفند"))
    }

    @Test
    fun multipleCategoriesAllReport() {
        val flags = HalalFlags.flags("ژامبون، ژلاتین، شراب")
        assertEquals(3, flags.size)
        assertTrue("گوشت خوک" in flags)
        assertTrue("ژلاتین حیوانی" in flags)
        assertTrue("الکل" in flags)
    }

    // ── AC 2: ambiguous / missing data never flags ───────────────────

    @Test
    fun emptyIngredientsProduceNoFlag() {
        assertEquals(emptyList<String>(), HalalFlags.flags(""))
        assertEquals(emptyList<String>(), HalalFlags.flags("   "))
    }

    @Test
    fun ordinaryPersianDishIsNotFlagged() {
        assertEquals(
            emptyList<String>(),
            HalalFlags.flags("برنج، گوشت گاو، پیاز، سیر، زردچوبه، نمک، روغن"),
        )
    }

    @Test
    fun unknownIngredientIsNotFlagged() {
        // A word nobody recognises must stay silent, not guess.
        assertEquals(emptyList<String>(), HalalFlags.flags("xtqv-1234, zz-plug"))
    }

    // ── Boundary traps: whole-word matching, not substrings ──────────

    @Test
    fun lambNeverTripsHam() {
        // «گوشت گوسفند» is the nearest neighbour to the word ham; it is not pork.
        assertEquals(emptyList<String>(), HalalFlags.flags("lamb chops"))
        assertEquals(emptyList<String>(), HalalFlags.flags("گوشت گوسفند"))
    }

    @Test
    fun ramenNeverTripsRum() {
        assertEquals(emptyList<String>(), HalalFlags.flags("نودل رامن"))
    }

    @Test
    fun breadcrumbsNeverTripsRum() {
        // Real data hit: 28 seed dishes carry «breadcrumbs», which contains
        // "rum". Substring matching would flag all 28 as alcohol.
        assertEquals(emptyList<String>(), HalalFlags.flags("2 beaten eggs، 50g breadcrumbs"))
    }

    @Test
    fun waterAndWhineNeverTripWine() {
        assertEquals(emptyList<String>(), HalalFlags.flags("آب"))
        assertEquals(emptyList<String>(), HalalFlags.flags("whine and water"))
    }

    @Test
    fun mushroomDoesNotTripMuesliOrShrooms() {
        // Guard against a future word-list edit that adds «mushroom».
        assertEquals(emptyList<String>(), HalalFlags.flags("mushroom soup"))
    }

    @Test
    fun gelatinNeedleDoesNotMatchInsideGelatinousEnglishRoot() {
        // «gelatinous» is a different word; the after-boundary must reject it.
        assertEquals(emptyList<String>(), HalalFlags.flags("gelatinous dessert"))
    }

    // ── Joining: «و» and quantities must not smear or hide a flag ────

    @Test
    fun joinedAndLineFlagsOnlyTheFlaggedHalf() {
        assertEquals(listOf("گوشت خوک"), HalalFlags.flags("پیاز و ژامبون"))
        assertEquals(emptyList<String>(), HalalFlags.flags("پیاز و نمک"))
    }

    @Test
    fun quantityPrefixedEnglishLineStillFlags() {
        // TheMealDB lines look like "400ml double cream" — the amount is glued
        // to the word, so the match must survive amount stripping.
        assertEquals(listOf("ژلاتین حیوانی"), HalalFlags.flags("10g gelatin"))
    }

    @Test
    fun flagListHasNoDuplicates() {
        assertEquals(1, HalalFlags.flags("ژلاتین، ژلاتین").size)
        assertEquals(1, HalalFlags.flags("شراب، wine").size)
    }

    // ── Wording: a check, never a certification ──────────────────────

    @Test
    fun warningTextNamesTheCheckNotCertification() {
        val text = HalalFlags.warningText(listOf("ژلاتین حیوانی"))!!
        assertTrue(text.contains("بررسی کن"))
        assertFalse(text.contains("حلال"))
        assertFalse(text.contains("مجاز"))
        assertFalse(text.contains("مطمئن"))
    }

    @Test
    fun noFlagsMeansNoWarningRow() {
        assertNull(HalalFlags.warningText(emptyList()))
    }

    // ── Strict toggle ───────────────────────────────────────────────

    @Test
    fun strictToggleHidesOnlyWhenFlagsExist() {
        assertTrue(HalalFlags.shouldHide(true, listOf("گوشت خوک")))
        assertFalse(HalalFlags.shouldHide(true, emptyList()))
        assertFalse(HalalFlags.shouldHide(false, listOf("گوشت خوک")))
    }
}
