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
import com.erfanbagheri.tahdig.util.ExpiryMath
import com.erfanbagheri.tahdig.util.NotifyMath
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.StreakMath
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The evening check (#122). One alarm, one receiver, ONE decision:
 *
 * - streak live + no cook today -> loss-aversion nudge (silent if the
 *   weekly floor is already met),
 * - otherwise 24h / 7d idle re-engagement, one-shot each per idle period,
 * - otherwise the daily suggestion.
 *
 * Quiet hours suppress ALL of these (they are reminders). Cooking timers
 * post through TimerReceiver and are deliberately NOT gated here — a timer
 * the user started to cook with must never be silenced.
 */
class DailyNotifyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!SettingsStore.isInitialized()) SettingsStore.init(context)
        // User may have disabled the toggle since the alarm was set.
        if (!SettingsStore.dailyNotify.value) {
            return
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                show(context)
            } finally {
                // One-shot alarm: always re-arm for tomorrow.
                DailyNotifyScheduler.schedule(context)
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

        // Quiet hours: a reminder inside the window is dropped entirely —
        // there is no snooze and no "later" post. Timers never come here.
        if (SettingsStore.quietOn.value && !NotifyMath.mayPost(
                LocalTime.now(),
                NotifyMath.Kind.REMINDER,
                minutesToTime(SettingsStore.quietFromMin.value),
                minutesToTime(SettingsStore.quietUntilMin.value),
            )
        ) {
            return
        }

        val db = TahdigDatabase.getInstance(context)
        val action = decide(db)
        val text = when (action) {
            NotifyMath.Action.STREAK_RISK -> streakRiskText(db)
            NotifyMath.Action.IDLE_24H -> {
                SettingsStore.setIdleTierHours(NotifyMath.stampAfter(action))
                "دیگه ازت خبری نیست! 🍳" to "یه غذای ساده بپز تا ریتم آشپزی‌ات حفظ بشه"
            }
            NotifyMath.Action.IDLE_7D -> {
                SettingsStore.setIdleTierHours(NotifyMath.stampAfter(action))
                "یه هفته‌ست نپختی 🍲" to "فرصت خوبیه برای یه غذای تازه — بیا شروع کن"
            }
            else -> suggestionText(db)
        }
        post(context, text.first, text.second)
    }

    /** The pure decision, reusing exactly what Home's streak card computes. */
    private suspend fun decide(db: TahdigDatabase): NotifyMath.Action {
        val cookedDays = cookedDays(db)
        val today = LocalDate.now()
        val floor = SettingsStore.weeklyFloor.value
        val state = StreakMath.compute(cookedDays, today, SettingsStore.freezes.value, floor)
        val evening = NotifyMath.eveningAction(
            cookedToday = today in cookedDays,
            streakDays = state.current,
            weeklyFloorMet = state.weeklyFloor >= floor,
        )
        if (evening == NotifyMath.Action.STREAK_RISK) return evening

        val hoursIdle = runCatching { db.historyDao().allTimestamps() }
            .getOrDefault(emptyList()).maxOrNull()?.let {
                (System.currentTimeMillis() - it) / 3_600_000.0
            }
        // Nothing at risk and nothing idle -> the original daily suggestion.
        return NotifyMath.idleAction(hoursIdle, SettingsStore.idleTierHours.value)
            ?: NotifyMath.Action.DAILY_SUGGESTION
    }

    private suspend fun cookedDays(db: TahdigDatabase): MutableSet<LocalDate> {
        val zone = ZoneId.systemDefault()
        return runCatching { db.historyDao().allTimestamps() }
            .getOrDefault(emptyList()).mapTo(mutableSetOf()) {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            }
    }

    private suspend fun streakRiskText(db: TahdigDatabase): Pair<String, String> {
        val today = LocalDate.now()
        val floor = SettingsStore.weeklyFloor.value
        val state = StreakMath.compute(
            cookedDays(db), today, SettingsStore.freezes.value, floor,
        )
        val days = PersianText.toPersianDigits(state.current)
        return "ریزش رکورد! 🔥" to "فقط یه پخت تا ادامه‌ی $days روزه"
    }

    /** The original suggestion, plus the expiry line when something is urgent. */
    private suspend fun suggestionText(db: TahdigDatabase): Pair<String, String> {
        val name = runCatching {
            db.foodDao().randomAny(1).firstOrNull()?.name
        }.getOrNull() ?: "یک غذای خوشمزه"

        val expiring = runCatching {
            val now = System.currentTimeMillis()
            val urgent = db.pantryDao().observeAll().first()
                .filter { ExpiryMath.isUrgent(ExpiryMath.daysTo(it.expiresAt, now)) }
                .map { it.item }
            ExpiryMath.summary(urgent)
        }.getOrNull()?.takeIf { it.isNotBlank() }

        return if (expiring != null) {
            "امروز چی بپزیم؟ 🍽" to "$name · $expiring"
        } else {
            "امروز چی بپزیم؟ 🍽" to "پیشنهاد امروز: $name"
        }
    }

    private fun post(context: Context, title: String, body: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "یادآوری هوشمند", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val tap = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
    }

    /** minutes-since-midnight → LocalTime, clamped to the valid range. */
    private fun minutesToTime(min: Int): LocalTime {
        val m = min.coerceIn(0, 24 * 60)
        return LocalTime.of(m / 60, m % 60)
    }

    companion object {
        const val CHANNEL_ID = "tahdig_daily"
        const val NOTIF_ID = 1001
    }
}
