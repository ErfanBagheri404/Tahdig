package com.erfanbagheri.tahdig.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.data.prefs.TimerStore

/** Reschedules the daily alarm after device reboot (alarms don't survive restarts). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Only re-arm when the user actually has the daily suggestion enabled.
        if (!SettingsStore.isInitialized()) SettingsStore.init(context)
        if (SettingsStore.dailyNotify.value) DailyNotifyScheduler.schedule(context)
        // The photo prompt (#125) is its own alarm and must survive a reboot too.
        if (SettingsStore.photoPrompt.value) PhotoPromptScheduler.schedule(context)
        // Running countdowns died with the reboot — re-arm them (#95).
        if (!TimerStore.isInitialized()) TimerStore.init(context)
        TimerStore.rearmAll(context)
    }
}
