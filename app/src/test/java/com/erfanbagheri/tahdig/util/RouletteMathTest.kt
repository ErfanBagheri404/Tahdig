package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouletteMathTest {

    private val candidates = (1L..40L).toList()

    // ── pool construction ───────────────────────────────────────────

    @Test
    fun vetoedDishes_neverReturn_evenWhenAllRecentAreGone() {
        val pool = RouletteMath.buildPool(candidates, vetoed = setOf(5L, 6L), recent = emptyList())
        assertFalse(5L in pool)
        assertFalse(6L in pool)
        assertEquals(RouletteMath.MAX_SLICES, pool.size)
    }

    @Test
    fun pool_neverExceedsTwelveSlices() {
        val pool = RouletteMath.buildPool(candidates, vetoed = setOf(), recent = emptyList())
        assertEquals(RouletteMath.MAX_SLICES, pool.size)
    }

    @Test
    fun recentDishes_droppedWhenFreshAlternativesExist() {
        val recent = (1L..40L).toList()  // everything recent…
        // …except one: the wheel still spins with it.
        val almostAllRecent = recent.dropLast(1)
        val pool = RouletteMath.buildPool(candidates, vetoed = setOf(), recent = almostAllRecent)
        assertEquals(listOf(40L), pool)
    }

    @Test
    fun allRecent_fallsBackToFullPool_insteadOfEmptyWheel() {
        val pool = RouletteMath.buildPool(candidates, vetoed = setOf(), recent = candidates)
        assertTrue(pool.isNotEmpty())
        assertEquals(RouletteMath.MAX_SLICES, pool.size)
    }

    @Test
    fun vetoWinsOverRecent_fallbackStillExcludesVetoed() {
        // Every candidate recent AND some vetoed → fallback to "fresh" set
        // must still honor the session veto (AC: never repeats vetoed).
        val pool = RouletteMath.buildPool(candidates, vetoed = setOf(39L, 40L), recent = candidates)
        assertFalse(39L in pool)
        assertFalse(40L in pool)
    }

    // ── seeded determinism ─────────────────────────────────────────

    @Test
    fun sameSeed_sameWinner() {
        val pool = RouletteMath.buildPool(candidates, setOf(), emptyList())
        val a = RouletteMath.pick(pool, seed = 42L)
        val b = RouletteMath.pick(pool, seed = 42L)
        assertEquals(a, b)
    }

    @Test
    fun differentSeeds_produceDifferentWinners() {
        val pool = RouletteMath.buildPool(candidates, setOf(), emptyList())
        // Over 200 seeds the pick cannot stay identical unless broken.
        val picks = (0L..200L).map { RouletteMath.pick(pool, it) }.toSet()
        assertTrue("pick ignores the seed", picks.size > 1)
    }

    @Test
    fun pick_alwaysMemberOfPool() {
        val pool = RouletteMath.buildPool(candidates, setOf(1L), emptyList())
        (0L..50L).forEach { seed ->
            assertTrue(RouletteMath.pick(pool, seed) in pool)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun pick_emptyPool_throws() {
        RouletteMath.pick(emptyList(), seed = 1L)
    }

    // ── landing geometry: rotation must put the winner under the pointer ──

    @Test
    fun landing_putChosenSliceCenterAtTop() {
        for (count in 1..RouletteMath.MAX_SLICES) {
            for (index in 0 until count) {
                val rotation = RouletteMath.landingDegrees(index, count)
                val landed = (RouletteMath.sliceCenterDeg(index, count) + rotation) % 360.0
                val distance = minOf(landed, 360.0 - landed)
                assertTrue(
                    "slice $index of $count lands ${'$'}{distance}° off the pointer",
                    distance < 0.01,
                )
            }
        }
    }

    @Test
    fun landing_includesFullTurns_neverJustAFlick() {
        val rotation = RouletteMath.landingDegrees(0, 12)
        assertTrue(rotation >= RouletteMath.TURNS * 360f)
        assertTrue(rotation <= (RouletteMath.TURNS + 1) * 360f)
    }

    @Test
    fun landing_isFloatRotationOfTheSameWinnerEveryTime() {
        assertEquals(
            RouletteMath.landingDegrees(3, 8),
            RouletteMath.landingDegrees(3, 8),
            0.0f,
        )
        assertNotEquals(
            RouletteMath.landingDegrees(3, 8),
            RouletteMath.landingDegrees(4, 8),
            0.001f,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun landing_moreSlicesThanWheel_throws() {
        RouletteMath.landingDegrees(0, RouletteMath.MAX_SLICES + 1)
    }
}
