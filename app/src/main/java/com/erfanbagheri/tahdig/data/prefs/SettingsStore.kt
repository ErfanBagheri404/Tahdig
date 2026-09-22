package com.erfanbagheri.tahdig.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SettingsStore {
    private const val PREFS_NAME = "tahdig_settings"
    private const val KEY_THEME_MODE = "theme_mode" // 0=system, 1=light, 2=dark
    private const val KEY_ONBOARDED = "onboarded"
    private const val KEY_DAILY_NOTIFY = "daily_notify"
    private const val KEY_VOICE_CONTROL = "voice_control"   // hands-free cook mode (#94)
    private const val KEY_VOICE_READ = "voice_read_aloud"   // TTS of next step (#94)
    private const val KEY_SHAKE = "shake_advance"           // shake-to-advance (#96)
    private const val KEY_SHAKE_SENS = "shake_sensitivity"  // 0f..1f slider (#96)

    private lateinit var prefs: SharedPreferences
    private val _themeMode = MutableStateFlow(0)
    val themeMode: StateFlow<Int> = _themeMode

    private val _onboarded = MutableStateFlow(false)
    val onboarded: StateFlow<Boolean> = _onboarded

    private val _dailyNotify = MutableStateFlow(false)
    val dailyNotify: StateFlow<Boolean> = _dailyNotify

    private val _voiceControl = MutableStateFlow(false)
    val voiceControl: StateFlow<Boolean> = _voiceControl

    private val _voiceReadAloud = MutableStateFlow(false)
    val voiceReadAloud: StateFlow<Boolean> = _voiceReadAloud

    private val _shakeAdvance = MutableStateFlow(false)
    val shakeAdvance: StateFlow<Boolean> = _shakeAdvance

    private val _shakeSensitivity = MutableStateFlow(0.5f)
    val shakeSensitivity: StateFlow<Float> = _shakeSensitivity

    fun isInitialized(): Boolean = ::prefs.isInitialized

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _themeMode.value = prefs.getInt(KEY_THEME_MODE, 0)
        _onboarded.value = prefs.getBoolean(KEY_ONBOARDED, false)
        _dailyNotify.value = prefs.getBoolean(KEY_DAILY_NOTIFY, false)
        _voiceControl.value = prefs.getBoolean(KEY_VOICE_CONTROL, false)
        _voiceReadAloud.value = prefs.getBoolean(KEY_VOICE_READ, false)
        _shakeAdvance.value = prefs.getBoolean(KEY_SHAKE, false)
        _shakeSensitivity.value = prefs.getFloat(KEY_SHAKE_SENS, 0.5f)
    }

    /** Shake-to-advance in cook mode (#96); default off — gestures are opt-in. */
    fun setShakeAdvance(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHAKE, enabled).apply()
        _shakeAdvance.value = enabled
    }

    /** Shake sensitivity 0..1 (#96); higher = advances on a lighter shake. */
    fun setShakeSensitivity(value: Float) {
        val v = value.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_SHAKE_SENS, v).apply()
        _shakeSensitivity.value = v
    }

    /** Hands-free cook-mode voice control master (#94); default off. */
    fun setVoiceControl(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_CONTROL, enabled).apply()
        _voiceControl.value = enabled
    }

    /** Read the next step aloud via TTS (#94); separate switch, default off. */
    fun setVoiceReadAloud(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_READ, enabled).apply()
        _voiceReadAloud.value = enabled
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setOnboarded() {
        prefs.edit().putBoolean(KEY_ONBOARDED, true).apply()
        _onboarded.value = true
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
