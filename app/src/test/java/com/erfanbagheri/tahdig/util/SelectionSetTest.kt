package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #81 — the selection-set toggle logic behind batch dish actions. */
class SelectionSetTest {

    @Test
    fun `fresh set is inactive and empty`() {
        val s = SelectionSet()
        assertFalse(s.isActive)
        assertEquals(0, s.count)
        assertTrue(s.ids.isEmpty())
    }

    @Test
    fun `toggle adds a missing id`() {
        val s = SelectionSet().toggle(7L)
        assertTrue(7L in s.ids)
        assertEquals(1, s.count)
        assertTrue(s.isActive)
    }

    @Test
    fun `toggle removes a present id`() {
        val s = SelectionSet(setOf(7L, 8L)).toggle(7L)
        assertEquals(setOf(8L), s.ids)
        assertEquals(1, s.count)
    }

    @Test
    fun `toggling the last id exits selection mode`() {
        val s = SelectionSet(setOf(7L)).toggle(7L)
        assertFalse(s.isActive)
        assertEquals(0, s.count)
    }

    @Test
    fun `toggle is symmetric for three dishes`() {
        var s = SelectionSet()
        s = s.toggle(1L).toggle(2L).toggle(3L)
        assertEquals(3, s.count)
        s = s.toggle(2L)
        assertEquals(setOf(1L, 3L), s.ids)
    }

    @Test
    fun `add keeps existing picks and ignores duplicates`() {
        val s = SelectionSet(setOf(1L)).add(2L).add(2L)
        assertEquals(setOf(1L, 2L), s.ids)
        assertEquals(2, s.count)
    }

    @Test
    fun `remove drops one id and keeps the rest`() {
        val s = SelectionSet(setOf(1L, 2L, 3L)).remove(2L)
        assertEquals(setOf(1L, 3L), s.ids)
    }

    @Test
    fun `remove of an absent id is a no-op`() {
        val s = SelectionSet(setOf(1L)).remove(99L)
        assertEquals(setOf(1L), s.ids)
    }

    @Test
    fun `clear empties and deactivates`() {
        val s = SelectionSet(setOf(1L, 2L, 3L)).clear()
        assertFalse(s.isActive)
        assertEquals(0, s.count)
    }

    @Test
    fun `toggle does not mutate the original`() {
        val before = SelectionSet(setOf(1L))
        before.toggle(2L)
        assertEquals(setOf(1L), before.ids)
    }
}
