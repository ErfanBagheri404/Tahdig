package com.erfanbagheri.tahdig.util

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View

object Haptics {
    /** Light tap feedback; safe to call in any scope that has a View reference. */
    fun tap(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** Stronger feedback for confirmations / completions. */
    fun confirm(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }
}
