package com.erfanbagheri.tahdig.util

import android.view.HapticFeedbackConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HapticsTest {

    @Test
    fun `off level yields no feedback for either kind`() {
        assertNull(Haptics.constantFor(0, confirm = false))
        assertNull(Haptics.constantFor(0, confirm = true))
    }

    @Test
    fun `light level yields the same short tick for tap and confirm`() {
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, Haptics.constantFor(1, confirm = false))
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, Haptics.constantFor(1, confirm = true))
    }

    @Test
    fun `normal level keeps the original tap and confirm patterns`() {
        assertEquals(HapticFeedbackConstants.KEYBOARD_TAP, Haptics.constantFor(2, confirm = false))
        assertEquals(HapticFeedbackConstants.CONFIRM, Haptics.constantFor(2, confirm = true))
    }

    @Test
    fun `unknown level falls back to normal patterns`() {
        assertEquals(HapticFeedbackConstants.KEYBOARD_TAP, Haptics.constantFor(99, confirm = false))
        assertEquals(HapticFeedbackConstants.CONFIRM, Haptics.constantFor(-5, confirm = true))
    }
}
