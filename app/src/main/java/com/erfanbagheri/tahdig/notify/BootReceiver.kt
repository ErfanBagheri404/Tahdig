package com.erfanbagheri.tahdig.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.erfanbagheri.tahdig.data.prefs.SettingsStore

/** Reschedules the daily alarm after device reboot (alarms don't survive restarts). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Only re-arm when the user actually has the daily suggestion enabled.
        if (!SettingsStore.isInitialized()) SettingsStore.init(context)
        if (SettingsStore.dailyNotify.value) DailyNotifyScheduler.schedule(context)
    }
}
