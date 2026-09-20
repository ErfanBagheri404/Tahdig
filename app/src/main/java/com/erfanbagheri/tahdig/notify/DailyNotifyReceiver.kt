package com.erfanbagheri.tahdig.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.erfanbagheri.tahdig.MainActivity
import com.erfanbagheri.tahdig.R
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires a daily "what to cook today" notification. Scheduled with AlarmManager (no WorkManager dep). */
class DailyNotifyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TahdigDatabase.getInstance(context)
                val foods = db.foodDao().randomAny(1)
                val name = foods.firstOrNull()?.name ?: "یک غذای خوشمزه"
                show(context, name)
            } finally {
                pending.finish()
            }
        }
    }

    private fun show(context: Context, foodName: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "پیشنهاد روزانه", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val tap = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("امروز چی بپزیم؟ 🍽")
            .setContentText("پیشنهاد امروز: $foodName")
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        nm.notify(NOTIF_ID, notif)
    }

    companion object {
        const val CHANNEL_ID = "tahdig_daily"
        const val NOTIF_ID = 1001
    }
}
