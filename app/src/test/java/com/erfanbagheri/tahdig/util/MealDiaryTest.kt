package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class MealDiaryTest {

    // --- AC: log lands in correct slot by time ----------------------------

    @Test
    fun `morning log is breakfast`() {
        // 08:00 -> صبحانه
        assertEquals(MealTimeHelper.BREAKFAST, MealDiary.slotFor(LocalTime.of(8, 0)))
    }

    @Test
    fun `midday log is lunch`() {
        // 13:00 -> ناهار
        assertEquals(MealTimeHelper.LUNCH, MealDiary.slotFor(LocalTime.of(13, 0)))
    }

    @Test
    fun `afternoon log is a snack`() {
        // 16:00 -> میان‌وعده
        assertEquals(MealTimeHelper.SNACK, MealDiary.slotFor(LocalTime.of(16, 0)))
    }

    @Test
    fun `evening log is dinner`() {
        // 20:00 -> شام
        assertEquals(MealTimeHelper.DINNER, MealDiary.slotFor(LocalTime.of(20, 0)))
    }

    @Test
    fun `gap between breakfast and lunch is a snack not breakfast`() {
        // 10:45 falls in no range: a mid-morning snack, not breakfast
        // stretched twenty minutes.
        assertEquals(MealTimeHelper.SNACK, MealDiary.slotFor(LocalTime.of(10, 45)))
    }

    @Test
    fun `the late evening tail reads as the late slot`() {
        // 23:15 -> شام سبک
        assertEquals(MealTimeHelper.LIGHT_DINNER, MealDiary.slotForMinutes(23 * 60 + 15))
    }

    @Test
    fun `post midnight reads as the late slot`() {
        // 00:30 -> شام سبک, matching MealTimeHelper's clock rule.
        assertEquals(MealTimeHelper.LIGHT_DINNER, MealDiary.slotForMinutes(30))
    }

    @Test
    fun `the range boundaries hold`() {
        assertEquals(MealTimeHelper.BREAKFAST, MealDiary.slotForMinutes(360))
        assertEquals(MealTimeHelper.BREAKFAST, MealDiary.slotForMinutes(629))
        assertEquals(MealTimeHelper.LUNCH, MealDiary.slotForMinutes(690))
        assertEquals(MealTimeHelper.DINNER, MealDiary.slotForMinutes(1319))
    }

    @Test
    fun `out of range minutes clamp to a real time rather than crash`() {
        // 5000 clamps to 1439 = 23:59, the light-dinner slot; -5 clamps to
        // 0 = 00:30-ish, also the late slot. Neither may throw.
        assertEquals(MealTimeHelper.LIGHT_DINNER, MealDiary.slotForMinutes(5000))
        assertEquals(MealTimeHelper.LIGHT_DINNER, MealDiary.slotForMinutes(-5))
    }

    // --- grouping ---------------------------------------------------------

    @Test
    fun `rows group into their slots in slot order`() {
        val rows = listOf("a" to MealTimeHelper.LUNCH, "b" to MealTimeHelper.BREAKFAST)
        val grouped = MealDiary.groupBySlot(rows) { it.second }
        assertEquals(2, grouped.size)
        assertEquals(listOf("a"), grouped[MealTimeHelper.LUNCH]!!.map { it.first })
    }

    @Test
    fun `empty slots never appear in the diary`() {
        val rows = listOf("a" to MealTimeHelper.LUNCH)
        val grouped = MealDiary.groupBySlot(rows) { it.second }
        assertTrue(MealTimeHelper.BREAKFAST !in grouped)
    }

    // --- slot display order -----------------------------------------------

    @Test
    fun `slots order runs breakfast first and the late slot last`() {
        // LIGHT_DINNER is in the list: a 23:00 log lands there, and a slot
        // the diary never renders would hide real rows.
        assertEquals(
            listOf(
                MealTimeHelper.BREAKFAST,
                MealTimeHelper.LUNCH,
                MealTimeHelper.SNACK,
                MealTimeHelper.DINNER,
                MealTimeHelper.LIGHT_DINNER,
            ),
            MealDiary.SLOTS,
        )
    }

    @Test
    fun `only slots holding rows are visible`() {
        val grouped = mapOf(
            MealTimeHelper.LUNCH to listOf("a"),
            MealTimeHelper.LIGHT_DINNER to listOf("b"),
        )
        assertEquals(
            listOf(MealTimeHelper.LUNCH, MealTimeHelper.LIGHT_DINNER),
            MealDiary.visibleSlots(grouped),
        )
    }

    @Test
    fun `an empty bucket never renders a section`() {
        val grouped = mapOf(MealTimeHelper.BREAKFAST to emptyList<String>())
        assertTrue(MealDiary.visibleSlots(grouped).isEmpty())
    }
}
