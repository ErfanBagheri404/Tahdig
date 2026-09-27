package com.erfanbagheri.tahdig.util

/**
 * Keep-screen-on decision for cook mode (#98): the old global always-on flag is
 * replaced by a per-step rule — awake only while a step's timer is RUNNING and
 * longer than [LONG_STEP_SECS], plus the session's manual override. Pure so the
 * decision by duration/pause state is unit-testable (AC).
 */
object KeepAwake {

    /** A timer at or under this length is not worth holding the screen awake. */
    const val LONG_STEP_SECS = 120L

    /**
     * @param stepDurationSec the current step's parsed duration, null when the
     *   step has no timer (reading a step should not pin the screen).
     * @param running false covers paused AND not-yet-started timers.
     * @param manualOverride the session's top-bar switch — always wins.
     */
    fun shouldKeepAwake(
        stepDurationSec: Long?,
        running: Boolean,
        manualOverride: Boolean,
    ): Boolean = manualOverride ||
        (stepDurationSec != null && stepDurationSec > LONG_STEP_SECS && running)
}
