package com.erfanbagheri.tahdig.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.util.NotifyMath
import java.time.ZoneId

/**
 * The ONE owner of the evening reminder alarm (#122).
 *
 * It replaces the fixed 11:00 alarm: the trigger hour is the user's pick
 * (`SettingsStore.notifyHour`) and the receiver decides WHICH notification
 * — daily suggestion, streak-risk, 24h idle, 7d idle — or none at all.
 *
 * Inexact on purpose: exact alarms need SCHEDULE_EXACT_ALARM on API 31+,
 * and a cooking nudge does not justify it.
 */
object DailyNotifyScheduler {
    private const val REQUEST_CODE = 2001

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, DailyNotifyReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Epoch millis of the next fire at the user's reminder hour, strictly in the future. */
    fun nextTriggerAt(now: Long = System.currentTimeMillis()): Long =
        NotifyMath.nextTriggerAt(now, NotifyMath.reminderHour(SettingsStore.notifyHour.value), ZoneId.systemDefault())

    fun schedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerAt(), pendingIntent(context))
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
    }
}
