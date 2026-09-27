package com.erfanbagheri.tahdig.util

import android.view.HapticFeedbackConstants
import android.view.View
import com.erfanbagheri.tahdig.data.prefs.SettingsStore

object Haptics {
    /**
     * Maps (intensity level, kind) to a feedback constant, or null when haptics are off.
     * 0=off, 1=light (short tick for everything), 2=normal (current tap/confirm patterns).
     */
    fun constantFor(level: Int, confirm: Boolean): Int? = when (level) {
        0 -> null
        1 -> HapticFeedbackConstants.CLOCK_TICK
        else -> if (confirm) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP
    }

    /** Light tap feedback; safe to call in any scope that has a View reference. */
    fun tap(view: View) = perform(view, confirm = false)

    /** Stronger feedback for confirmations / completions. */
    fun confirm(view: View) = perform(view, confirm = true)

    private fun perform(view: View, confirm: Boolean) {
        val c = constantFor(SettingsStore.hapticLevelOrNormal(), confirm) ?: return
        view.performHapticFeedback(c)
    }
}
