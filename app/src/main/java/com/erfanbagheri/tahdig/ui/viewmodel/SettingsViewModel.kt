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
    val shakeAdvance: StateFlow<Boolean> = SettingsStore.shakeAdvance
    val shakeSensitivity: StateFlow<Float> = SettingsStore.shakeSensitivity

    /** Nutrition profile (#110) — edits recompute the budget immediately. */
    val profile: StateFlow<com.erfanbagheri.tahdig.util.DailyBudget.Profile> = SettingsStore.profile

    fun setProfile(p: com.erfanbagheri.tahdig.util.DailyBudget.Profile) = SettingsStore.setProfile(p)

    val allergens = SettingsStore.allergens
    val allergenHide = SettingsStore.allergenHide

    fun setAllergens(set: Set<String>) = SettingsStore.setAllergens(set)
    fun setAllergenHide(on: Boolean) = SettingsStore.setAllergenHide(on)

    fun setThemeMode(mode: Int) = SettingsStore.setThemeMode(mode)

    fun setDailyNotify(context: android.content.Context, enabled: Boolean) =
        SettingsStore.setDailyNotify(context, enabled)
    fun setVoiceControl(enabled: Boolean) = SettingsStore.setVoiceControl(enabled)
    fun setVoiceReadAloud(enabled: Boolean) = SettingsStore.setVoiceReadAloud(enabled)
    fun setShakeAdvance(enabled: Boolean) = SettingsStore.setShakeAdvance(enabled)

    /** Shake-to-spin the roulette (#123), off by default. */
    val shakeSpin = SettingsStore.shakeSpin
    fun setShakeSpin(on: Boolean) = SettingsStore.setShakeSpin(on)
    fun setShakeSensitivity(value: Float) = SettingsStore.setShakeSensitivity(value)
}
