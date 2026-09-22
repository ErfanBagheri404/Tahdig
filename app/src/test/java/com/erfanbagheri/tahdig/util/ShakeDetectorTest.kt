package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #96 AC: "Sensitivity slider changes threshold (unit test: classification
 * function)" + "off by default; on + shake advances, shake during sub-60s
 * countdown does not".
 */
class ShakeDetectorTest {

    @Test fun `higher sensitivity lowers the threshold`() {
        val low = ShakeDetector.thresholdFor(0f)
        val mid = ShakeDetector.thresholdFor(0.5f)
        val high = ShakeDetector.thresholdFor(1f)
        assertTrue("higher sensitivity must need less force", high < mid && mid < low)
    }

    @Test fun `sensitivity is clamped to the slider range`() {
        assertEquals(ShakeDetector.thresholdFor(0f), ShakeDetector.thresholdFor(-3f), 0.001f)
        assertEquals(ShakeDetector.thresholdFor(1f), ShakeDetector.thresholdFor(9f), 0.001f)
    }

    @Test fun `a spike past the threshold is a shake`() {
        val t = ShakeDetector.thresholdFor(0.5f)
        assertTrue(ShakeDetector.isShake(t + 1f, t, nowMs = 10_000L, lastShakeMs = 0L))
        assertTrue("exactly at the threshold counts", ShakeDetector.isShake(t, t, 10_000L, 0L))
    }

    @Test fun `a gentle stir is not a shake`() {
        val t = ShakeDetector.thresholdFor(0.5f)
        assertFalse(ShakeDetector.isShake(t - 1f, t, 10_000L, 0L))
        assertFalse(ShakeDetector.isShake(0f, t, 10_000L, 0L))
    }

    @Test fun `re-arm window swallows the rest of the burst`() {
        val t = ShakeDetector.thresholdFor(0.5f)
        // 1s after the last one: still inside the 1.5s re-arm.
        assertFalse(ShakeDetector.isShake(t + 5f, t, nowMs = 6_000L, lastShakeMs = 5_000L))
        // Past it: a genuinely new shake.
        assertTrue(ShakeDetector.isShake(t + 5f, t, nowMs = 7_000L, lastShakeMs = 5_000L))
    }

    @Test fun `a sub-minute countdown blocks the advance`() {
        assertFalse(ShakeDetector.canAdvance(remainingSec = 59L, running = true))
        assertFalse(ShakeDetector.canAdvance(remainingSec = 1L, running = true))
        assertFalse("exactly 60s still blocks", ShakeDetector.canAdvance(60L, true))
    }

    @Test fun `long timers and untimed steps advance freely`() {
        assertTrue(ShakeDetector.canAdvance(remainingSec = 480L, running = true))
        assertTrue("paused timer does not block", ShakeDetector.canAdvance(30L, running = false))
        assertTrue("no timer at all", ShakeDetector.canAdvance(null, running = false))
    }

    @Test fun `magnitude is the length of the vector`() {
        assertEquals(5f, ShakeDetector.magnitude(3f, 4f, 0f), 0.001f)
        assertEquals(0f, ShakeDetector.magnitude(0f, 0f, 0f), 0.001f)
    }

    @Test fun `raw accelerometer has gravity removed`() {
        // At rest the raw sensor reads ~9.81 — that is not a shake.
        assertEquals(0f, ShakeDetector.magnitudeOf(ShakeDetector.GRAVITY, linearSensor = false), 0.01f)
        // A linear-acceleration reading is used as-is.
        assertEquals(15f, ShakeDetector.magnitudeOf(15f, linearSensor = true), 0.01f)
        // Raw at rest + a spike: gravity subtracted, spike kept.
        assertEquals(5f, ShakeDetector.magnitudeOf(ShakeDetector.GRAVITY + 5f, false), 0.01f)
    }
}
