package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coverage math (#90): «۷ از ۴۲ غذا را امتحان کردی» — cooked/total from history
 * rows. Acceptance: untried region shows 0 progress (not a crash), and the count
 * is a pure intersection of region dish ids with cooked ids.
 */
class CuisineRegionsTest {

    @Test
    fun `coverage counts cooked dishes in the region`() {
        // Region has 42 dishes, history cooked 7 of them.
        val region = (1L..42L).toSet()
        val cooked = setOf(1L, 2L, 3L, 4L, 5L, 6L, 7L)
        val cov = CuisineRegions.coverage(region, cooked)
        assertEquals(7, cov.cooked)
        assertEquals(42, cov.total)
        assertEquals(7f / 42f, cov.fraction, 0.0001f)
        assertFalse(cov.isComplete)
    }

    @Test
    fun `untried region shows zero progress, not a crash`() {
        val cov = CuisineRegions.coverage(setOf(1L, 2L, 3L), emptySet())
        assertEquals(0, cov.cooked)
        assertEquals(3, cov.total)
        assertEquals(0f, cov.fraction, 0.0001f)
        assertFalse(cov.isComplete)
    }

    @Test
    fun `empty region is 0 of 0 — fraction 0, no division by zero`() {
        val cov = CuisineRegions.coverage(emptySet(), setOf(1L, 2L))
        assertEquals(0, cov.cooked)
        assertEquals(0, cov.total)
        assertEquals(0f, cov.fraction, 0.0001f)
        assertFalse(cov.isComplete)
    }

    @Test
    fun `fully cooked region is complete`() {
        val cov = CuisineRegions.coverage(setOf(1L, 2L), setOf(1L, 2L, 3L))
        assertEquals(2, cov.cooked)
        assertEquals(2, cov.total)
        assertEquals(1f, cov.fraction, 0.0001f)
        assertTrue(cov.isComplete)
    }

    @Test
    fun `cooked ids outside the region do not count`() {
        val cov = CuisineRegions.coverage(setOf(1L, 2L), setOf(1L, 99L, 100L))
        assertEquals(1, cov.cooked)
        assertEquals(2, cov.total)
    }

    @Test
    fun `every registered region has a Farsi label`() {
        for (r in CuisineRegions.all) {
            assertTrue("blank label for ${r.key}", r.label.isNotBlank())
        }
    }

    @Test
    fun `unknown cuisine key falls back to itself, never blank`() {
        assertEquals("SOMETHING_NEW", CuisineRegions.labelFor("SOMETHING_NEW"))
        // A registered key renders its Farsi label, not the raw value.
        assertEquals("ایرانی (سنتی)", CuisineRegions.labelFor("IRANI"))
    }

    @Test
    fun `persian regions come before the world section`() {
        val all = CuisineRegions.all
        val lastIran = all.indexOfLast { it.section == CuisineRegions.SECTION_IRAN }
        val firstWorld = all.indexOfFirst { it.section == CuisineRegions.SECTION_WORLD }
        assertTrue("world section must come after iran", firstWorld > lastIran)
    }
}
