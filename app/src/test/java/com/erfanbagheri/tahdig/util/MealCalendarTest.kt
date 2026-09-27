package com.erfanbagheri.tahdig.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** #84: slot→time, Saturday week start, stable identity, title format. */
class MealCalendarTest {

    private val tehran = ZoneId.of("Asia/Tehran")
    // 2026-09-26 is a Saturday.
    private val sat = LocalDate.of(2026, 9, 26)

    @Test fun `week starts Saturday`() {
        assertEquals(sat, MealCalendar.weekStart(sat)) // Saturday itself
        assertEquals(sat, MealCalendar.weekStart(sat.plusDays(1))) // Sunday
        assertEquals(sat, MealCalendar.weekStart(sat.plusDays(2))) // Monday
        assertEquals(sat, MealCalendar.weekStart(sat.plusDays(6))) // Friday
        assertEquals(sat.plusDays(7), MealCalendar.weekStart(sat.plusDays(7))) // next Saturday
    }

    @Test fun `slot hours`() {
        assertEquals(8, MealCalendar.slotHour("صبحانه"))
        assertEquals(13, MealCalendar.slotHour("ناهار"))
        assertEquals(20, MealCalendar.slotHour("شام"))
        assertEquals(12, MealCalendar.slotHour("میان‌وعده")) // unknown never midnight
    }

    @Test fun `lunch Saturday is 13_00 local`() {
        val expected = ZonedDateTime.of(sat, java.time.LocalTime.of(13, 0), tehran)
            .toInstant().toEpochMilli()
        assertEquals(expected, MealCalendar.eventStart(0, "ناهار", sat, tehran))
    }

    @Test fun `day index offsets from Saturday`() {
        val tue = MealCalendar.eventStart(3, "شام", sat, tehran)
        val expected = ZonedDateTime.of(sat.plusDays(3), java.time.LocalTime.of(20, 0), tehran)
            .toInstant().toEpochMilli()
        assertEquals(expected, tue)
    }

    @Test fun `event lasts one hour`() {
        val start = MealCalendar.eventStart(1, "صبحانه", sat, tehran)
        assertEquals(start + 3_600_000L, MealCalendar.eventEnd(1, "صبحانه", sat, tehran))
    }

    @Test fun `zone is explicit`() {
        val tehranMs = MealCalendar.eventStart(0, "ناهار", sat, tehran)
        val utcMs = MealCalendar.eventStart(0, "ناهار", sat, ZoneId.of("UTC"))
        // Same wall clock, different instants: 13:00 Tehran = 09:30 UTC,
        // i.e. the Tehran instant is 3.5h EARLIER.
        assertEquals(-(3_600_000L * 3 + 1_800_000L), tehranMs - utcMs)
    }

    @Test fun `full week yields 21 distinct keys`() {
        val slots = listOf("صبحانه", "ناهار", "شام")
        val keys = (0..6).flatMap { d -> slots.map { MealCalendar.eventKey(sat, d, it) } }
        assertEquals(21, keys.distinct().size)
    }

    @Test fun `key is stable and ignores the dish name`() {
        // Editing a slot keeps the key: re-export updates, never duplicates.
        assertEquals(
            MealCalendar.eventKey(sat, 2, "ناهار"),
            MealCalendar.eventKey(sat, 2, "ناهار"),
        )
        assertNotEquals(
            MealCalendar.eventKey(sat, 2, "ناهار"),
            MealCalendar.eventKey(sat.plusDays(7), 2, "ناهار"), // next week differs
        )
        assertTrue(MealCalendar.eventKey(sat, 2, "ناهار").startsWith("tahdig:"))
    }

    @Test fun `title format`() {
        assertEquals("ناهار: قورمه‌سبزی", MealCalendar.title("ناهار", "قورمه‌سبزی"))
    }
}
