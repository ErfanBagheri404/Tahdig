package com.erfanbagheri.tahdig.data

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.erfanbagheri.tahdig.util.MealCalendar
import java.time.LocalDate
import java.time.ZoneId

/**
 * Weekly meal-plan → device calendar (#84). Android-only, [CalendarContract]
 * directly, no library.
 *
 * Dedupe: each slot's stable [MealCalendar.eventKey] is written to the
 * event's CUSTOM_APP_URI, so re-export updates the same row instead of
 * doubling. No DB column, no schema change.
 */
object CalendarExport {

    data class Item(val dayIndex: Int, val slot: String, val dishName: String)

    sealed interface Outcome {
        data class Done(val written: Int) : Outcome
        data object NoPermission : Outcome
        data class Failed(val reason: String) : Outcome
    }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** Find the «ته‌دیگ» calendar, creating it when missing. Null = no provider. */
    fun ensureCalendarId(cr: ContentResolver): Long? {
        cr.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} = ?",
            arrayOf(MealCalendar.CALENDAR_NAME),
            null,
        )?.use { c -> if (c.moveToFirst()) return c.getLong(0) }
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, "tahdig")
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, MealCalendar.CALENDAR_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, MealCalendar.CALENDAR_NAME)
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFFE65100.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, "tahdig")
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
        }
        val createUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "tahdig")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
        return runCatching { cr.insert(createUri, values)?.let { ContentUris.parseId(it) } }
            .getOrNull()
    }

    /**
     * Insert-or-update one event per item. Never throws: every provider
     * failure becomes [Outcome.Failed] with a Farsi reason, never a crash.
     */
    fun exportWeek(
        context: Context,
        items: List<Item>,
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone),
    ): Outcome {
        if (!hasPermission(context)) return Outcome.NoPermission
        return runCatching {
            val cr = context.contentResolver
            val calId = ensureCalendarId(cr)
                ?: return Outcome.Failed("تقویم ته‌دیگ ساخته نشد — روی این گوشی برنامهٔ تقویم نیست؟")
            val weekStart = MealCalendar.weekStart(today)
            var written = 0
            for (item in items) {
                val key = MealCalendar.eventKey(weekStart, item.dayIndex, item.slot)
                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calId)
                    put(CalendarContract.Events.TITLE, MealCalendar.title(item.slot, item.dishName))
                    put(CalendarContract.Events.DESCRIPTION, MealCalendar.description(item.dishName))
                    put(
                        CalendarContract.Events.DTSTART,
                        MealCalendar.eventStart(item.dayIndex, item.slot, weekStart, zone),
                    )
                    put(
                        CalendarContract.Events.DTEND,
                        MealCalendar.eventEnd(item.dayIndex, item.slot, weekStart, zone),
                    )
                    put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
                    // Stable dedupe key in CUSTOM_APP_URI: re-export finds
                    // this row and updates it instead of inserting a duplicate.
                    put(CalendarContract.Events.CUSTOM_APP_URI, key)
                    put(CalendarContract.Events.HAS_ALARM, 0)
                }
                val existing = cr.query(
                    CalendarContract.Events.CONTENT_URI,
                    arrayOf(CalendarContract.Events._ID),
                    "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.CUSTOM_APP_URI} = ?",
                    arrayOf(calId.toString(), key),
                    null,
                )?.use { c -> if (c.moveToFirst()) c.getLong(0) else null }
                if (existing == null) {
                    cr.insert(CalendarContract.Events.CONTENT_URI, values)
                        ?: return Outcome.Failed("ثبت رویداد ممکن نشد — دوباره تلاش کنید")
                } else {
                    cr.update(
                        ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existing),
                        values, null, null,
                    )
                }
                written++
            }
            Outcome.Done(written)
        }.getOrElse { Outcome.Failed("خطا در نوشتن تقویم — دوباره تلاش کنید") }
    }
}
