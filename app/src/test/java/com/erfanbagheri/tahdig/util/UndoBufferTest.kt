package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Drives the buffer with an injected clock so the 5-second window is exact rather than
 * real-time-flaky.
 */
class UndoBufferTest {

    @Before
    fun reset() = UndoBuffer.clear()

    @Test
    fun `undo runs within the window`() {
        var undone = false
        UndoBuffer.push("حذف شد", { undone = true }, nowMs = 1_000L)
        val fired = UndoBuffer.pop(nowMs = 1_000L + UndoBuffer.WINDOW_MS - 1)
        assertTrue(fired)
        assertTrue(undone)
    }

    @Test
    fun `nothing to undo after the window closes`() {
        var undone = false
        UndoBuffer.push("حذف شد", { undone = true }, nowMs = 1_000L)
        assertNull(UndoBuffer.current(nowMs = 1_000L + UndoBuffer.WINDOW_MS + 1))
        assertFalse(UndoBuffer.pop(nowMs = 1_000L + UndoBuffer.WINDOW_MS + 1))
        assertFalse("expired undo must not fire", undone)
    }

    @Test
    fun `a newer push replaces the pending undo`() {
        var first = false
        var second = false
        UndoBuffer.push("اول", { first = true }, nowMs = 1_000L)
        UndoBuffer.push("دوم", { second = true }, nowMs = 2_000L)

        assertTrue(UndoBuffer.pop(nowMs = 3_000L))
        assertFalse("the first push is gone once replaced", first)
        assertTrue(second)
        // And it was single-use.
        assertFalse(UndoBuffer.pop(nowMs = 3_000L))
    }

    @Test
    fun `pop empties the buffer`() {
        UndoBuffer.push("حذف شد", {}, nowMs = 0L)
        assertNotNull(UndoBuffer.current(nowMs = 0L))
        assertTrue(UndoBuffer.pop(nowMs = 0L))
        assertNull(UndoBuffer.current(nowMs = 0L))
    }

    @Test
    fun `clear drops without firing`() {
        var undone = false
        UndoBuffer.push("حذف شد", { undone = true }, nowMs = 0L)
        UndoBuffer.clear()
        assertFalse(UndoBuffer.pop(nowMs = 0L))
        assertFalse(undone)
    }

    @Test
    fun `expiry marks the entry so the UI can tell a dead undo`() {
        val e = UndoBuffer.push("حذف شد", {}, nowMs = 0L)
        assertFalse(e.expired)
        UndoBuffer.current(nowMs = UndoBuffer.WINDOW_MS + 1)
        assertTrue(e.expired)
    }
}
