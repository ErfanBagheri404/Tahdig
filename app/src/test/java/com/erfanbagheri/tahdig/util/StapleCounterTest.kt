package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * AC #109: stepper never goes negative, bulk-clear undo restores counts
 * exactly, quantities clamp at [StapleCounter.MAX].
 */
class StapleCounterTest {

    @Test
    fun `decrement stops at zero`() {
        assertEquals(0, StapleCounter.step(0, -1))
        assertEquals(0, StapleCounter.step(1, -1))
        // A big negative delta cannot underflow either.
        assertEquals(0, StapleCounter.step(2, -99))
    }

    @Test
    fun `increment adds one and clamps at MAX`() {
        assertEquals(3, StapleCounter.step(2, 1))
        assertEquals(StapleCounter.MAX, StapleCounter.step(StapleCounter.MAX, 1))
        assertEquals(StapleCounter.MAX, StapleCounter.step(StapleCounter.MAX, 40))
    }

    @Test
    fun `out-of-range input clamps instead of corrupting`() {
        assertEquals(StapleCounter.MAX, StapleCounter.clamp(10_000))
        assertEquals(0, StapleCounter.clamp(-7))
        assertEquals(5, StapleCounter.clamp(5))
    }

    @Test
    fun `fill sets stocked to exactly one`() {
        assertEquals(1, StapleCounter.filledValue())
    }

    @Test
    fun `undo snapshot preserves ids and counts exactly`() {
        data class Row(val id: Long, val qty: Int)
        val before = mutableListOf(Row(1, 4), Row(2, 0), Row(3, 99))
        val snap = StapleCounter.snapshotForClear(before)
        // The bulk clear wipes the source list; undo must not depend on it.
        before.clear()
        assertEquals(emptyList<Row>(), before)
        assertEquals(listOf(Row(1, 4), Row(2, 0), Row(3, 99)), snap)
        assertEquals(listOf(4, 0, 99), snap.map { it.qty })
    }

    @Test
    fun `empty pantry snapshot arms no undo`() {
        assertEquals(emptyList<Any>(), StapleCounter.snapshotForClear(emptyList<Any>()))
    }
}
