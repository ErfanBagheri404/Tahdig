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
    val hapticLevel: StateFlow<Int> = SettingsStore.hapticLevel

    fun setThemeMode(mode: Int) = SettingsStore.setThemeMode(mode)

    fun setHapticLevel(level: Int) = SettingsStore.setHapticLevel(level)

    fun setDailyNotify(context: android.content.Context, enabled: Boolean) =
        SettingsStore.setDailyNotify(context, enabled)
}
