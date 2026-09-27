package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoBufferTest {

    /** Hand-cranked clock — the window is tested without ever sleeping. */
    private class FakeClock(var t: Long = 1_000L) {
        fun advance(ms: Long) { t += ms }
        fun read(): Long = t
    }

    private fun buffer(clock: FakeClock) = UndoBuffer(clock = clock::read)

    @Test fun `armed action is undoable inside the window`() {
        val clock = FakeClock()
        val buf = buffer(clock)
        var restored = false
        buf.arm("حذف") { restored = true }
        assertEquals("حذف", buf.pending)
        clock.advance(4_000)
        assertTrue(buf.undo())
        assertTrue(restored)
    }

    @Test fun `expired action is not undone`() {
        val clock = FakeClock()
        val buf = buffer(clock)
        var restored = false
        buf.arm("حذف") { restored = true }
        clock.advance(10_001)
        assertFalse(buf.undo())
        assertFalse(restored)
        assertNull(buf.pending)
    }

    @Test fun `exactly at the boundary the window is still open`() {
        // The full window must still undo: the snackbar is visible for that
        // whole tick, so the last moment has to be actionable.
        val clock = FakeClock()
        val buf = buffer(clock)
        var restored = false
        buf.arm("حذف") { restored = true }
        clock.advance(UndoBuffer.DEFAULT_WINDOW_MS)
        assertTrue(buf.undo())
        assertTrue(restored)
    }

    @Test fun `undo runs once only`() {
        val clock = FakeClock()
        val buf = buffer(clock)
        var runs = 0
        buf.arm("حذف") { runs++ }
        assertTrue(buf.undo())
        // A second tap must be a no-op, not a second restore.
        assertFalse(buf.undo())
        assertEquals(1, runs)
    }

    @Test fun `undo with nothing armed is a no-op`() {
        val buf = buffer(FakeClock())
        assertFalse(buf.undo())
        assertNull(buf.pending)
    }

    @Test fun `newest action replaces the previous one`() {
        val clock = FakeClock()
        val buf = buffer(clock)
        var first = 0
        var second = 0
        buf.arm("اول") { first++ }
        buf.arm("دوم") { second++ }
        assertTrue(buf.undo())
        // Only the latest action is undoable — the buffer promises one slot.
        assertEquals(0, first)
        assertEquals(1, second)
    }

    @Test fun `clear drops the pending action`() {
        val clock = FakeClock()
        val buf = buffer(clock)
        var restored = false
        buf.arm("حذف") { restored = true }
        buf.clear()
        assertNull(buf.pending)
        assertFalse(buf.undo())
        assertFalse(restored)
    }

    @Test fun `re-arming from inside the inverse survives`() {
        // The inverse itself is destructive (e.g. undo of a merge re-arms).
        val clock = FakeClock()
        val buf = buffer(clock)
        var outer = 0
        var inner = 0
        buf.arm("بیرونی") {
            outer++
            buf.arm("درونی") { inner++ }
        }
        assertTrue(buf.undo())
        assertEquals(1, outer)
        // The inner arm must not have been cleared by the outer clear().
        assertEquals("درونی", buf.pending)
        assertTrue(buf.undo())
        assertEquals(1, inner)
    }

    @Test fun `remaining time counts down and floors at zero`() {
        val clock = FakeClock()
        val buf = buffer(clock)
        assertEquals(0L, buf.remainingMs())
        buf.arm("حذف") {}
        assertEquals(UndoBuffer.DEFAULT_WINDOW_MS, buf.remainingMs())
        clock.advance(2_000)
        assertEquals(UndoBuffer.DEFAULT_WINDOW_MS - 2_000, buf.remainingMs())
        clock.advance(60_000)
        assertEquals(0L, buf.remainingMs())
    }

    @Test fun `isExpired is false while unarmed`() {
        val clock = FakeClock()
        val buf = buffer(clock)
        clock.advance(60_000)
        // No pending action is not the same as an expired one.
        assertFalse(buf.isExpired())
    }
}
