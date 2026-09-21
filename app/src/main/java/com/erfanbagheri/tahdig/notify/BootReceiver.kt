package com.erfanbagheri.tahdig.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Reschedules the daily alarm after device reboot (alarms don't survive restarts). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DailyNotifyScheduler.schedule(context)
        }
    }
}
