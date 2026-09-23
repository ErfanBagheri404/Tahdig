package com.erfanbagheri.tahdig.util

import java.util.Random

/**
 * Dinner roulette (#123): pool construction and the seeded spin.
 *
 * Everything here is a pure function of its inputs — the wheel UI only
 * renders [landingDegrees], so the winner can be asserted without a clock,
 * a sensor, or a frame.
 */
object RouletteMath {

    /** The wheel shows at most [MAX_SLICES] segments; beyond that it stops
     *  being readable, so the pool is truncated (never the pick logic). */
    const val MAX_SLICES = 12

    /** Full turns before landing — long enough to read as a spin. */
    const val TURNS = 5

    /**
     * Build the wheel pool: drop vetoed dishes outright (session rule — a
     * vetoed dish must never come back), then prefer dishes outside the
     * last-[recent] picks so the wheel does not re-offer what was just
     * cooked. Falling back to the full set when *every* candidate is recent
     * keeps small buckets from rendering an empty wheel.
     */
    fun buildPool(
        candidates: List<Long>,
        vetoed: Set<Long>,
        recent: List<Long>,
    ): List<Long> {
        val fresh = candidates.filterNot { it in vetoed }
        val notRecent = fresh.filterNot { it in recent }
        val pool = if (notRecent.isNotEmpty()) notRecent else fresh
        return pool.take(MAX_SLICES)
    }

    /**
     * Deterministic winner for a seed: [pool] order is an input, so the
     * same (seed, pool) always lands the same dish — testable, and the UI
     * needs the winner up front to animate a honest landing.
     */
    fun pick(pool: List<Long>, seed: Long): Long {
        require(pool.isNotEmpty()) { "empty pool" }
        return pool[Random(seed).nextInt(pool.size)]
    }

    /**
     * Degrees of clockwise rotation that put slice [index] under the top
     * pointer: slice centers sit at `index * arc + arc/2` measured clockwise
     * from 12 o'clock, the wheel turns [TURNS] extra times so the stop never
     * looks snapped.
     */
    fun landingDegrees(index: Int, sliceCount: Int): Float {
        require(sliceCount in 1..MAX_SLICES) { "bad slice count $sliceCount" }
        val arc = 360.0 / sliceCount
        val center = index * arc + arc / 2.0
        val align = (360.0 - center % 360.0) % 360.0
        return (TURNS * 360.0 + align).toFloat()
    }

    /** Mid-angle of a slice, degrees clockwise from 12 o'clock. */
    fun sliceCenterDeg(index: Int, sliceCount: Int): Double =
        index * (360.0 / sliceCount) + 180.0 / sliceCount
}
