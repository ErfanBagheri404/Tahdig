package com.erfanbagheri.tahdig.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.erfanbagheri.tahdig.MainActivity
import com.erfanbagheri.tahdig.R
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Home-screen widget showing the dish of the day. Tap opens the app. */
class DishOfDayWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_dish_of_day).apply {
                    setTextViewText(R.id.widget_dish_name, dishOfTheDay(context))
                    setOnClickPendingIntent(
                        R.id.widget_root,
                        PendingIntent.getActivity(
                            context, 0,
                            Intent(context, MainActivity::class.java),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                        ),
                    )
                }
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Same deterministic pick as the home screen: day-of-year modulo the food count.
     * Must stay in sync with HomeViewModel.loadDishOfDay(), otherwise the widget and the
     * app disagree about what "today's dish" is.
     */
    private suspend fun dishOfTheDay(context: Context): String {
        val dao = TahdigDatabase.getInstance(context).foodDao()
        val count = runCatching { dao.count() }.getOrDefault(0)
        if (count <= 0) return "طاق دیگ"
        val index = LocalDate.now().dayOfYear % count
        return runCatching { dao.byIndex(index)?.name }.getOrNull() ?: "طاق دیگ"
    }
}
