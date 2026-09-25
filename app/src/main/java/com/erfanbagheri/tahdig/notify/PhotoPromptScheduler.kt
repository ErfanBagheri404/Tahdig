package com.erfanbagheri.tahdig.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.util.NotifyMath
import java.time.ZoneId

/**
 * The SECOND alarm owner (#125): the optional daily lunch photo prompt.
 *
 * Separate from the evening reminder because it has its own toggle, its own
 * hour, and its own suppression rule (a photo posted today silences it).
 * Inexact, one-shot, re-armed by the receiver — the same shape as the
 * evening scheduler.
 */
object PhotoPromptScheduler {
    private const val REQUEST_CODE = 2002

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, PhotoPromptReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun nextTriggerAt(now: Long = System.currentTimeMillis()): Long {
        val hour = SettingsStore.photoPromptHour.value.coerceIn(1, 23)
        return NotifyMath.nextTriggerAt(now, hour, ZoneId.systemDefault(), minute = 0)
    }

    fun schedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerAt(), pendingIntent(context))
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
    }
}
