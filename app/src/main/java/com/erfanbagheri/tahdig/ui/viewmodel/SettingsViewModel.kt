package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    init {
        if (!SettingsStore.isInitialized()) SettingsStore.init(app)
    }

    val themeMode: StateFlow<Int> = SettingsStore.themeMode
    val dailyNotify: StateFlow<Boolean> = SettingsStore.dailyNotify
    val voiceControl: StateFlow<Boolean> = SettingsStore.voiceControl
    val voiceReadAloud: StateFlow<Boolean> = SettingsStore.voiceReadAloud

    fun setThemeMode(mode: Int) = SettingsStore.setThemeMode(mode)

    fun setDailyNotify(context: android.content.Context, enabled: Boolean) =
        SettingsStore.setDailyNotify(context, enabled)
    fun setVoiceControl(enabled: Boolean) = SettingsStore.setVoiceControl(enabled)
    fun setVoiceReadAloud(enabled: Boolean) = SettingsStore.setVoiceReadAloud(enabled)
}
