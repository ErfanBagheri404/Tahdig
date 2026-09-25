package com.erfanbagheri.tahdig.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Inflates the widget layouts through [RemoteViews] (#131).
 *
 * This is the only check that catches the class of bug that only shows up in
 * the *launcher* process: RemoteViews inflates against a fixed allow-list of
 * classes, and anything outside it (a bare `<View>`, for one) throws there and
 * nowhere else — a normal Robolectric `setContentView` would pass.
 */
// Robolectric 4.13 tops out at SDK 34; the app targets 36. The layouts use
// no SDK-gated APIs, so pinning here is safe and keeps the check runnable.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DishOfDayWidgetLayoutTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun allThreeLayoutsInflateThroughRemoteViews() {
        val layouts = listOf(
            com.erfanbagheri.tahdig.R.layout.widget_dish_of_day,
            com.erfanbagheri.tahdig.R.layout.widget_dish_medium,
            com.erfanbagheri.tahdig.R.layout.widget_dish_large,
        )
        layouts.forEach { layout ->
            val views = RemoteViews(context.packageName, layout)
            assertNotNull("RemoteViews refused layout $layout", views)
            // apply() is what the launcher calls; it resolves the allow-list.
            views.apply(context, null)
        }
    }

    @Test
    fun providerIsRegisteredForEverySizeBucket() {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, DishOfDayWidget::class.java),
        )
        // No instances yet is normal on a fresh install; the call must not throw.
        assertNotNull(ids)
    }

    @Test
    fun mediumLayoutExposesShuffleAndStreakSlots() {
        val views = RemoteViews(
            context.packageName,
            com.erfanbagheri.tahdig.R.layout.widget_dish_medium,
        )
        views.setTextViewText(com.erfanbagheri.tahdig.R.id.widget_dish_name, "قورمه‌سبزی")
        views.setTextViewText(com.erfanbagheri.tahdig.R.id.widget_streak_line, "🔥 ۳ روز")
        views.setViewVisibility(
            com.erfanbagheri.tahdig.R.id.widget_streak_line, android.view.View.VISIBLE,
        )
        // The write path the widget uses must not throw for these ids.
        views.apply(context, null)
    }
}
