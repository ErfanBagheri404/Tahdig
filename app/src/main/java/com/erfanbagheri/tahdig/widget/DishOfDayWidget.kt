package com.erfanbagheri.tahdig.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.erfanbagheri.tahdig.R
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                val db = TahdigDatabase.getInstance(context)
                val name = db.foodDao().randomAny(1).firstOrNull()?.name ?: "طاق دیگ"
                val views = RemoteViews(context.packageName, R.layout.widget_dish_of_day).apply {
                    setTextViewText(R.id.widget_dish_name, name)
                    setOnClickPendingIntent(
                        R.id.widget_root,
                        android.app.PendingIntent.getActivity(
                            context, 0,
                            android.content.Intent(context, com.erfanbagheri.tahdig.MainActivity::class.java),
                            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
                        ),
                    )
                }
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
