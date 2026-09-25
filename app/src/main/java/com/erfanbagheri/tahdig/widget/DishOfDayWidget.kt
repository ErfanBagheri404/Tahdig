package com.erfanbagheri.tahdig.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.erfanbagheri.tahdig.MainActivity
import com.erfanbagheri.tahdig.R
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.util.StreakMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Home-screen widget (#131): what to eat, how long you've kept at it, and one
 * tap to change the answer. Three size variants from the same data.
 */
class DishOfDayWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) render(context, appWidgetManager, id)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        render(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val manager = AppWidgetManager.getInstance(context)
        val cn = ComponentName(context, DishOfDayWidget::class.java)
        val ids = manager.getAppWidgetIds(cn) ?: IntArray(0)

        when (intent.action) {
            ACTION_SHUFFLE -> {
                val p = prefs(context)
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val count = runCatching {
                            TahdigDatabase.getInstance(context).foodDao().count()
                        }.getOrDefault(0)
                        val todayMidnight = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
                        val sameDay = p.getLong(KEY_SPIN_DAY, 0L) == todayMidnight
                        p.edit()
                            .putInt(KEY_SPIN, (p.getInt(KEY_SPIN, 0) + 1).let {
                                if (count > 0) it % count else it
                            })
                            .putInt(KEY_SPIN_COUNT, if (sameDay) p.getInt(KEY_SPIN_COUNT, 0) + 1 else 1)
                            .putLong(KEY_SPIN_DAY, todayMidnight)
                            .apply()
                        for (id in ids) render(context, manager, id)
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_WIDGET_REFRESH -> {
                if (ids.isNotEmpty()) {
                    val pending = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            for (id in ids) render(context, manager, id)
                        } finally {
                            pending.finish()
                        }
                    }
                }
            }
        }
    }

    private fun render(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                // The platform already reports these in dp; pass them through.
                val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
                val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40)
                val size = WidgetLogic.sizeFor(w, h)
                val state = load(context, size)
                val views = build(context, size, state)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } finally {
                pending.finish()
            }
        }
    }

    private data class WidgetState(
        val dishName: String,
        val dishId: Long,
        val extras: List<Pair<Long, String>>,
        val streakLine: String?,
        val spin: Int,
    )

    private suspend fun load(context: Context, size: WidgetLogic.Size): WidgetState {
        val db = TahdigDatabase.getInstance(context)
        val count = runCatching { db.foodDao().count() }.getOrDefault(0)
        val todayIndex = if (count > 0) LocalDate.now().dayOfYear % count else 0
        val p = prefs(context)
        val spin = p.getInt(KEY_SPIN, 0)
        val spinCount = p.getInt(KEY_SPIN_COUNT, 0)
        val spinDay = p.getLong(KEY_SPIN_DAY, 0L)
        val todayMidnight = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        val held = spinCount > 0 && spinDay == todayMidnight
        val mainIndex = if (held) WidgetLogic.shuffledIndex(spin, count) else todayIndex

        val main = runCatching { db.foodDao().byIndex(mainIndex) }.getOrNull()
        val extras: List<Pair<Long, String>> = if (size == WidgetLogic.Size.LARGE) {
            val others = runCatching { db.foodDao().randomAny(3) }.getOrDefault(emptyList())
            others.map { it.id to it.name }
        } else emptyList()

        return WidgetState(
            dishName = main?.name ?: "ته‌دیگ",
            dishId = main?.id ?: -1L,
            extras = extras,
            streakLine = streakLine(context),
            spin = spin,
        )
    }

    private suspend fun streakLine(context: Context): String? {
        if (!SettingsStore.isInitialized()) SettingsStore.init(context)
        val ts = runCatching {
            TahdigDatabase.getInstance(context).historyDao().allTimestamps()
        }.getOrDefault(emptyList())
        val days = ts.map {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()
        val state = StreakMath.compute(
            cookedDays = days,
            today = LocalDate.now(),
            freezesLeft = SettingsStore.freezes.value,
            weeklyFloor = SettingsStore.weeklyFloor.value,
        )
        return WidgetLogic.streakLine(state)
    }

    private fun build(context: Context, size: WidgetLogic.Size, s: WidgetState): RemoteViews {
        val layout = when (size) {
            WidgetLogic.Size.LARGE -> R.layout.widget_dish_large
            WidgetLogic.Size.MEDIUM -> R.layout.widget_dish_medium
            WidgetLogic.Size.COMPACT -> R.layout.widget_dish_of_day
        }
        return RemoteViews(context.packageName, layout).apply {
            when (size) {
                WidgetLogic.Size.COMPACT -> {
                    setTextViewText(R.id.widget_dish_name, s.dishName)
                }
                WidgetLogic.Size.MEDIUM -> {
                    setTextViewText(R.id.widget_dish_name, s.dishName)
                    if (s.streakLine != null) {
                        setTextViewText(R.id.widget_streak_line, s.streakLine)
                        setViewVisibility(R.id.widget_streak_line, View.VISIBLE)
                    } else {
                        setViewVisibility(R.id.widget_streak_line, View.GONE)
                    }
                    setOnClickPendingIntent(
                        R.id.widget_btn_shuffle,
                        shuffleIntent(context, s.spin),
                    )
                }
                WidgetLogic.Size.LARGE -> {
                    if (s.streakLine != null) {
                        setTextViewText(R.id.widget_streak_line, s.streakLine)
                        setViewVisibility(R.id.widget_streak_line, View.VISIBLE)
                    } else {
                        setViewVisibility(R.id.widget_streak_line, View.GONE)
                    }
                    val rowIds = intArrayOf(
                        R.id.widget_dish_row_1,
                        R.id.widget_dish_row_2,
                        R.id.widget_dish_row_3,
                    )
                    rowIds.forEachIndexed { i, viewId ->
                        val item = s.extras.getOrNull(i)
                        val title = item?.second ?: s.dishName
                        val foodId = item?.first ?: s.dishId
                        setTextViewText(viewId, "• $title")
                        setOnClickPendingIntent(viewId, dishIntent(context, foodId))
                    }
                    setOnClickPendingIntent(
                        R.id.widget_btn_shuffle,
                        shuffleIntent(context, s.spin),
                    )
                }
            }
            setOnClickPendingIntent(R.id.widget_root, dishIntent(context, s.dishId))
        }
    }

    private fun dishIntent(context: Context, foodId: Long): PendingIntent {
        require(WidgetLogic.routeForDish(foodId) == WidgetLogic.Action.OPEN_DISH)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DEST_DETAIL)
            putExtra(MainActivity.EXTRA_FOOD_ID, foodId)
        }
        val reqCode = 1000 + if (foodId >= 0) (foodId % 500).toInt() else 0
        return PendingIntent.getActivity(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun shuffleIntent(context: Context, spin: Int): PendingIntent {
        val intent = Intent(context, DishOfDayWidget::class.java).apply {
            action = ACTION_SHUFFLE
        }
        return PendingIntent.getBroadcast(
            context,
            2000 + (spin % 500),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.erfanbagheri.tahdig.widget.REFRESH"
        const val ACTION_SHUFFLE = "com.erfanbagheri.tahdig.widget.SHUFFLE"
        private const val PREFS = "tahdig_widget"
        private const val KEY_SPIN = "spin"
        private const val KEY_SPIN_COUNT = "spin_count"
        private const val KEY_SPIN_DAY = "spin_day"

        /** External triggers: cook stamp, theme change, midnight reset. */
        fun refreshAll(context: Context) {
            val intent = Intent(context, DishOfDayWidget::class.java).apply {
                action = ACTION_WIDGET_REFRESH
            }
            context.sendBroadcast(intent)
        }
    }
}
