package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #98 AC: "Unit test: keep-awake decision function by step duration/pause state".
 */
class KeepAwakeTest {

    @Test fun `long running simmer keeps the screen awake`() {
        assertTrue(KeepAwake.shouldKeepAwake(stepDurationSec = 480L, running = true, manualOverride = false))
        // Strictly greater than 2 min.
        assertTrue(KeepAwake.shouldKeepAwake(stepDurationSec = 121L, running = true, manualOverride = false))
    }

    @Test fun `paused long timer sleeps`() {
        assertFalse(KeepAwake.shouldKeepAwake(stepDurationSec = 480L, running = false, manualOverride = false))
    }

    @Test fun `not-yet-started long timer sleeps`() {
        // running=false covers "loaded but never started" — reading must not pin.
        assertFalse(KeepAwake.shouldKeepAwake(stepDurationSec = 480L, running = false, manualOverride = false))
    }

    @Test fun `short step never holds the screen`() {
        assertFalse(KeepAwake.shouldKeepAwake(stepDurationSec = 120L, running = true, manualOverride = false))
        assertFalse(KeepAwake.shouldKeepAwake(stepDurationSec = 90L, running = true, manualOverride = false))
    }

    @Test fun `step without a timer sleeps`() {
        assertFalse(KeepAwake.shouldKeepAwake(stepDurationSec = null, running = false, manualOverride = false))
        // Even a stray running=true with no duration stays off — no duration, no rule.
        assertFalse(KeepAwake.shouldKeepAwake(stepDurationSec = null, running = true, manualOverride = false))
    }

    @Test fun `manual override always wins`() {
        assertTrue(KeepAwake.shouldKeepAwake(stepDurationSec = null, running = false, manualOverride = true))
        assertTrue(KeepAwake.shouldKeepAwake(stepDurationSec = 60L, running = false, manualOverride = true))
        assertTrue(KeepAwake.shouldKeepAwake(stepDurationSec = 480L, running = false, manualOverride = true))
    }
}
