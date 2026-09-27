package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Occasion windows (#88). Acceptance: forcing the device date to 30 آذر shows the
 * Yalda shelf, outside every window nothing shows, and the Jalali rules hold across
 * year boundaries (30 آذر → 1 دی wraps, and the winter window spans into Farvardin).
 */
class OccasionRegistryTest {

    @Before
    fun setUp() {
        OccasionRegistry.load(
            """
            [
              {"key":"yalda","title":"شب یلدا","shelf":"شب یلدا نزدیکه",
               "rule":{"type":"jalali","from":[9,28],"to":[9,30]},"dishes":[416,2]},
              {"key":"summer","title":"تابستان","shelf":"تابستان",
               "rule":{"type":"jalali","from":[4,1],"to":[6,31]},"dishes":[116]},
              {"key":"nowruz","title":"نوروز","shelf":"نوروز",
               "rule":{"type":"jalali","from":[1,1],"to":[1,4]},"dishes":[51]},
              {"key":"winter","title":"زمستان","shelf":"زمستان",
               "rule":{"type":"jalali","from":[12,25],"to":[1,5]},"dishes":[45]}
            ]
            """.trimIndent()
        )
    }

    @Test
    fun `30 Azar shows the Yalda shelf`() {
        // 30 آذر 1403 = 20 December 2024
        val yalda = OccasionRegistry.activeOn(LocalDate.of(2024, 12, 20))
        assertEquals("yalda", yalda?.key)
        assertEquals("شب یلدا نزدیکه", yalda?.shelf)
        assertTrue(yalda!!.dishes.isNotEmpty())
    }

    @Test
    fun `outside every window nothing is active`() {
        // 1 مهر 1403 = 22 September 2024 — no window covers it
        assertNull(OccasionRegistry.activeOn(LocalDate.of(2024, 9, 22)))
    }

    @Test
    fun `jalali year boundary converts correctly`() {
        // 1 فروردین 1404 = 21 March 2025
        val j = JalaliDate.toJalali(LocalDate.of(2025, 3, 21))
        assertEquals(1404, j.year)
        assertEquals(1, j.month)
        assertEquals(1, j.day)
    }

    @Test
    fun `winter window wraps across the new year`() {
        // Test window wraps اسفند 25 … فروردین 5. Both sides of the wrap must fire.
        // Anchors: 1403 is a leap Jalali year, so اسفند 30 = 2025-03-20 and
        // 1404/1/1 = 2025-03-21 (the passing Nowruz test) ⇒ اسفند 28 = 03-18.
        // 28 اسفند 1403 → key 1228 ≥ 1225 → winter (after Yalda, before Nowruz)
        assertEquals("winter", OccasionRegistry.activeOn(LocalDate.of(2025, 3, 18))?.key)
        // 5 فروردین 1404 → key 105 ≤ 105 → winter (Nowruz's window ends at 4)
        assertEquals("winter", OccasionRegistry.activeOn(LocalDate.of(2025, 3, 25))?.key)
        // Yalda still wins its own window over the wrap behind it
        assertEquals("yalda", OccasionRegistry.activeOn(LocalDate.of(2024, 12, 20))?.key)
    }

    @Test
    fun `nowruz window matches early Farvardin`() {
        // 2 فروردین 1404 = 22 March 2025
        assertEquals("nowruz", OccasionRegistry.activeOn(LocalDate.of(2025, 3, 22))?.key)
    }

    @Test
    fun `summer window matches Tir through Shahrivar`() {
        // 15 تیر 1403 = 5 July 2024
        assertEquals("summer", OccasionRegistry.activeOn(LocalDate.of(2024, 7, 5))?.key)
    }

    @Test
    fun `first matching window wins — file order is priority`() {
        // Two windows overlap on 30 آذر; the earlier entry must win.
        OccasionRegistry.load(
            """
            [
              {"key":"first","title":"الف","shelf":"الف",
               "rule":{"type":"jalali","from":[9,1],"to":[9,30]},"dishes":[1]},
              {"key":"second","title":"ب","shelf":"ب",
               "rule":{"type":"jalali","from":[9,1],"to":[9,30]},"dishes":[2]}
            ]
            """.trimIndent()
        )
        assertEquals("first", OccasionRegistry.activeOn(LocalDate.of(2024, 12, 20))?.key)
    }

    @Test
    fun `an unparsable library degrades to no shelves`() {
        OccasionRegistry.load("not json at all")
        assertTrue(OccasionRegistry.all.isEmpty())
        assertNull(OccasionRegistry.activeOn(LocalDate.of(2024, 12, 20)))
    }

    @Test
    fun `window helper handles both directions`() {
        assertTrue(JalaliDate.inWindow(1225, 105, 1230))   // wrap: 30 Azar
        assertTrue(JalaliDate.inWindow(1225, 105, 103))    // wrap: 3 Dey
        assertFalse(JalaliDate.inWindow(1225, 105, 615))   // outside wrap
        assertTrue(JalaliDate.inWindow(401, 631, 415))     // normal range
    }

    @Test
    fun `hijri rule type does not crash on any date`() {
        OccasionRegistry.load(
            """[{"key":"ramadan","title":"رمضان","shelf":"رمضان",
                 "rule":{"type":"hijri","from":[9,1],"to":[9,29]},"dishes":[297]}]"""
        )
        // Just must not throw; the answer depends on the device's Hijri calendar.
        OccasionRegistry.activeOn(LocalDate.of(2024, 3, 15))
    }
}
