package com.erfanbagheri.tahdig.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SettingsStore {
    private const val PREFS_NAME = "tahdig_settings"
    private const val KEY_THEME_MODE = "theme_mode" // 0=system, 1=light, 2=dark
    private const val KEY_DAILY_NOTIFY = "daily_notify"

    private lateinit var prefs: SharedPreferences
    private val _themeMode = MutableStateFlow(0)
    val themeMode: StateFlow<Int> = _themeMode

    private val _dailyNotify = MutableStateFlow(false)
    val dailyNotify: StateFlow<Boolean> = _dailyNotify

    fun isInitialized(): Boolean = ::prefs.isInitialized

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _themeMode.value = prefs.getInt(KEY_THEME_MODE, 0)
        _dailyNotify.value = prefs.getBoolean(KEY_DAILY_NOTIFY, false)
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    /** Toggle the daily suggestion notification; schedules/cancels the alarm. */
    fun setDailyNotify(context: Context, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_NOTIFY, enabled).apply()
        _dailyNotify.value = enabled
        if (enabled) {
            com.erfanbagheri.tahdig.notify.DailyNotifyScheduler.schedule(context)
        } else {
            com.erfanbagheri.tahdig.notify.DailyNotifyScheduler.cancel(context)
        }
    }
}
