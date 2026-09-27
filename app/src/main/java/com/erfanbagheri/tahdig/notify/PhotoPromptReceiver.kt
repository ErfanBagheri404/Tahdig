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
import com.erfanbagheri.tahdig.util.PhotoChallenge
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires the daily lunch photo prompt (#125).
 *
 * Suppressed if:
 * - the user disabled the toggle,
 * - a photo was ALREADY added to the journal today,
 * - the clock lands inside quiet hours (and quiet hours are on).
 *
 * Tapping the notification deep-links to the journal tab with the photo
 * picker primed.
 */
class PhotoPromptReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!SettingsStore.isInitialized()) SettingsStore.init(context)
        if (!SettingsStore.photoPrompt.value) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                show(context)
            } finally {
                PhotoPromptScheduler.schedule(context)
                pending.finish()
            }
        }
    }

    private suspend fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val db = TahdigDatabase.getInstance(context)
        val photos = runCatching { db.journalDao().photoTimestamps() }.getOrDefault(emptyList())
        val hasPhotoToday = PhotoChallenge.hasPhotoOn(photos, LocalDate.now())

        val verdict = PhotoChallenge.verdict(
            enabled = SettingsStore.photoPrompt.value,
            now = LocalTime.now(),
            quietOn = SettingsStore.quietOn.value,
            quietFrom = minutesToTime(SettingsStore.quietFromMin.value),
            quietUntil = minutesToTime(SettingsStore.quietUntilMin.value),
            hasPhotoToday = hasPhotoToday,
        )
        if (verdict != PhotoChallenge.Verdict.POST) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "یادآور عکس ناهار", NotificationManager.IMPORTANCE_DEFAULT),
        )

        val tap = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DEST_JOURNAL_PHOTO)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("امروز ناهار عکس بگیر 📸")
            .setContentText("یک عکس از غذای امروزت بنداز تا دفترت زنده بمونه")
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
    }

    private fun minutesToTime(min: Int): LocalTime {
        val m = min.coerceIn(0, 24 * 60)
        return LocalTime.of(m / 60, m % 60)
    }

    companion object {
        const val CHANNEL_ID = "tahdig_photo_prompt"
        const val NOTIF_ID = 1002
        private const val REQUEST_CODE = 2003
    }
}
