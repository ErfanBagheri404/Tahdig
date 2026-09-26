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

    // Theme (#128)
    val accentHex: StateFlow<String> = SettingsStore.accentHex
    val highContrast: StateFlow<Boolean> = SettingsStore.highContrast

    /** #92 — 0 until «بازنشانی سلیقه» is tapped. Drives the row's caption. */
    val tasteResetAt: StateFlow<Long> = SettingsStore.tasteResetAt

    /** #92 — stop the accumulated taste steering the feed, keeping the data. */
    fun resetTasteProfile() = SettingsStore.resetTasteProfile()

    fun setAccentHex(hex: String) = SettingsStore.setAccentHex(hex)
    fun setHighContrast(on: Boolean) = SettingsStore.setHighContrast(on)
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

    // Halal-style flags (#118): strict toggle hides flagged dishes in search.
    val halalStrict = SettingsStore.halalStrict
    fun setHalalStrict(on: Boolean) = SettingsStore.setHalalStrict(on)

    // Pregnancy / breastfeeding mode + caffeine cap (#119).
    val pregnancyMode = SettingsStore.pregnancyMode
    val caffeineCap = SettingsStore.caffeineCap
    fun setPregnancyMode(on: Boolean) = SettingsStore.setPregnancyMode(on)
    fun setCaffeineCap(mg: Int) = SettingsStore.setCaffeineCap(mg)

    // Calorie carry-over (#114): the diary's progress strip reads this flow.
    val carryOver = SettingsStore.carryOver
    fun setCarryOver(on: Boolean) = SettingsStore.setCarryOver(on)

    // Nutrient caps (#113): preset name plus {NUTRIENT: mg/g} overrides.
    val capPreset = SettingsStore.capPreset
    val capCustom = SettingsStore.capCustom
    fun setCapPreset(name: String?) = SettingsStore.setCapPreset(name)
    fun setCapCustom(nutrient: String, value: Double) = SettingsStore.setCapCustom(nutrient, value)

    fun setThemeMode(mode: Int) {
        SettingsStore.setThemeMode(mode)
        com.erfanbagheri.tahdig.widget.DishOfDayWidget.refreshAll(getApplication())
    }

    fun setDailyNotify(context: android.content.Context, enabled: Boolean) =
        SettingsStore.setDailyNotify(context, enabled)

    // Daily photo prompt (#125).
    val photoPrompt = SettingsStore.photoPrompt
    val photoPromptHour = SettingsStore.photoPromptHour

    fun setPhotoPrompt(context: android.content.Context, enabled: Boolean) =
        SettingsStore.setPhotoPrompt(context, enabled)
    fun setPhotoPromptHour(context: android.content.Context, hour: Int) =
        SettingsStore.setPhotoPromptHour(context, hour)

    // Smart notifications (#122): user-picked hour, quiet hours, denial hint.
    val notifyHour = SettingsStore.notifyHour
    val quietOn = SettingsStore.quietOn
    val quietFromMin = SettingsStore.quietFromMin
    val quietUntilMin = SettingsStore.quietUntilMin
    val notifDenied = SettingsStore.notifDenied

    fun setNotifyHour(context: android.content.Context, hour: Int) {
        SettingsStore.setNotifyHour(hour)
        if (SettingsStore.dailyNotify.value) {
            com.erfanbagheri.tahdig.notify.DailyNotifyScheduler.schedule(context)
        }
    }

    fun setQuietOn(on: Boolean) = SettingsStore.setQuietOn(on)
    fun setQuietWindow(fromMin: Int, untilMin: Int) = SettingsStore.setQuietWindow(fromMin, untilMin)

    /** Re-record a granted permission (the Settings re-prime path). */
    fun setNotifPermission(primed: Boolean, granted: Boolean) =
        SettingsStore.setNotifPermission(primed, granted)
    fun setVoiceControl(enabled: Boolean) = SettingsStore.setVoiceControl(enabled)
    fun setVoiceReadAloud(enabled: Boolean) = SettingsStore.setVoiceReadAloud(enabled)
    fun setShakeAdvance(enabled: Boolean) = SettingsStore.setShakeAdvance(enabled)

    /** Shake-to-spin the roulette (#123), off by default. */
    val shakeSpin = SettingsStore.shakeSpin
    fun setShakeSpin(on: Boolean) = SettingsStore.setShakeSpin(on)

    // Barcode scanner (#116) — hides the scanner entry point in search.
    val scannerEnabled = SettingsStore.scannerEnabled
    fun setScannerEnabled(on: Boolean) = SettingsStore.setScannerEnabled(on)
    fun setShakeSensitivity(value: Float) = SettingsStore.setShakeSensitivity(value)
}
