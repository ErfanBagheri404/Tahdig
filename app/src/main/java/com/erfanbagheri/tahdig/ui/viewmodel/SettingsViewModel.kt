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

    fun setThemeMode(mode: Int) = SettingsStore.setThemeMode(mode)
}
