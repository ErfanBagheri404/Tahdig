package com.erfanbagheri.tahdig.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.erfanbagheri.tahdig.MainActivity
import com.erfanbagheri.tahdig.R
import com.erfanbagheri.tahdig.data.prefs.TimerStore

/**
 * Fires when a countdown reaches zero (#95). Survives process death because
 * AlarmManager delivers to this manifest-registered receiver directly.
 */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, Long.MIN_VALUE)
        if (id == Long.MIN_VALUE) return

        if (!TimerStore.isInitialized()) TimerStore.init(context)
        val timer = TimerStore.get(id)

        if (id == TimerScheduler.STEP_ID) {
            // Cook-mode step mirror: never touches the store.
            fire(context, NOTIF_BASE + 999, "زمان این مرحله تمام شد", "برو سراغ مرحله بعد")
            return
        }
        // Stale alarm guard: a paused/cancelled timer must not notify (#95
        // "check the toggle in the receiver" pattern from the daily alarm).
        if (timer == null || !timer.running) return

        TimerStore.markFired(timer.id)
        // One notification id per timer id: two timers firing minutes apart
        // appear as two rows AND race on the same id collapses safely.
        fire(context, NOTIF_BASE + timer.id.toInt(), timer.name, "زمان تایمر تمام شد ⏰")
    }

    private fun fire(context: Context, notificationId: Int, title: String, text: String) {
        // POST_NOTIFICATIONS is runtime-granted on API 33+; skip quietly when denied.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "تایمرها", NotificationManager.IMPORTANCE_HIGH),
        )

        val tap = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notif)
    }

    companion object {
        const val EXTRA_ID = "timer_id"
        const val CHANNEL_ID = "tahdig_timer"
        const val NOTIF_BASE = 2000
    }
}
