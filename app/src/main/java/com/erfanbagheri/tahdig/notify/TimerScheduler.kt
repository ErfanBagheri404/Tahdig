package com.erfanbagheri.tahdig.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.erfanbagheri.tahdig.util.TimerState

/**
 * AlarmManager arm/disarm for countdown timers (#95). One PendingIntent per
 * timer id, so pausing one timer can never cancel another's alarm.
 *
 * Exact on API <31 (no permission needed) and on 31+ when
 * SCHEDULE_EXACT_ALARM is granted; degrades to inexact otherwise instead of
 * throwing — a cooking timer that fires a few minutes late beats a crash.
 * The cook-mode STEP timer shares this path with its own request code.
 */
object TimerScheduler {

    /** Distinct from DailyNotifyScheduler's 2001. */
    private const val BASE_REQUEST = 3000

    /** The step countdown's mirror alarm (no TimerStore row of its own). */
    const val STEP_ID = -1L

    fun schedule(context: Context, timer: TimerState) {
        if (timer.endsAtMs <= 0L) return
        arm(context, timer.id, timer.endsAtMs)
    }

    /** Step mirror: arm at [endsAtMs] with a display name for the notification. */
    fun scheduleStep(context: Context, endsAtMs: Long) = arm(context, STEP_ID, endsAtMs)

    fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, id))
    }

    private fun arm(context: Context, id: Long, endsAtMs: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, id)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtMs, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtMs, pi)
        }
    }

    private fun pendingIntent(context: Context, id: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            // Negative ids (STEP_ID) map below the base without colliding.
            BASE_REQUEST + id.toInt(),
            Intent(context, TimerReceiver::class.java).putExtra(TimerReceiver.EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
