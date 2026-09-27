package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The diary's slot chip cycle (#114). Cycling must cover exactly the four
 * user-facing slots and always terminate — LIGHT_DINNER folds back to
 * BREAKFAST rather than getting a fifth stop, because it is a clock
 * artefact (23:00) the user never picks on purpose.
 */
class MealSlotCycleTest {

    @Test
    fun `cycle runs breakfast to lunch to snack to dinner`() {
        assertEquals(MealTimeHelper.LUNCH, MealTimeHelper.nextSlot(MealTimeHelper.BREAKFAST))
        assertEquals(MealTimeHelper.SNACK, MealTimeHelper.nextSlot(MealTimeHelper.LUNCH))
        assertEquals(MealTimeHelper.DINNER, MealTimeHelper.nextSlot(MealTimeHelper.SNACK))
    }

    @Test
    fun `dinner wraps back to breakfast`() {
        assertEquals(MealTimeHelper.BREAKFAST, MealTimeHelper.nextSlot(MealTimeHelper.DINNER))
    }

    @Test
    fun `the late clock slot folds into the cycle instead of dead-ending`() {
        // A row logged at 23:00 is LIGHT_DINNER; tapping its chip must move
        // it somewhere real, not stay put.
        assertEquals(MealTimeHelper.BREAKFAST, MealTimeHelper.nextSlot(MealTimeHelper.LIGHT_DINNER))
    }

    @Test
    fun `an unknown slot never traps the row`() {
        assertEquals(MealTimeHelper.BREAKFAST, MealTimeHelper.nextSlot("NONSENSE"))
    }

    @Test
    fun `four taps return to the starting slot`() {
        var slot = MealTimeHelper.BREAKFAST
        repeat(4) { slot = MealTimeHelper.nextSlot(slot) }
        assertEquals(MealTimeHelper.BREAKFAST, slot)
    }

    @Test
    fun `every cycle target is a slot the diary renders`() {
        // The cycle can only land somewhere the diary draws, or a tap would
        // move a row into a section that does not exist.
        var slot = MealTimeHelper.BREAKFAST
        repeat(8) {
            slot = MealTimeHelper.nextSlot(slot)
            assertEquals(true, slot in MealDiary.SLOTS)
        }
    }
}
