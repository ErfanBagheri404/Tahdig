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
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires a daily "what to cook today" notification. Scheduled with AlarmManager (no WorkManager dep). */
class DailyNotifyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // User may have disabled the toggle since the alarm was set — respect it and stop.
        if (!SettingsStore.dailyNotify.value) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                show(context)
            } finally {
                // One-shot alarm: always re-arm for tomorrow, even if showing failed.
                DailyNotifyScheduler.schedule(context)
                pending.finish()
            }
        }
    }

    private suspend fun show(context: Context) {
        // POST_NOTIFICATIONS is runtime-granted on API 33+; skip quietly when denied.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "پیشنهاد روزانه", NotificationManager.IMPORTANCE_DEFAULT),
        )

        val name = runCatching {
            TahdigDatabase.getInstance(context).foodDao().randomAny(1).firstOrNull()?.name
        }.getOrNull() ?: "یک غذای خوشمزه"

        val tap = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("امروز چی بپزیم؟ 🍽")
            .setContentText("پیشنهاد امروز: $name")
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
    }

    companion object {
        const val CHANNEL_ID = "tahdig_daily"
        const val NOTIF_ID = 1001
    }
}
