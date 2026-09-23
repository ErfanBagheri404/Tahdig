package com.erfanbagheri.tahdig.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.erfanbagheri.tahdig.util.DailyBudget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
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
    private const val KEY_CONVERT_FAV = "convert_favorite"  // last-used pair (#103)
    private const val KEY_AISLE_ORDER = "aisle_order"        // JSON list (#108)
    private const val KEY_AISLE_RENAMES = "aisle_renames"    // JSON map (#108)
    private const val KEY_AISLE_HIDDEN = "aisle_hidden"      // JSON set (#108)
    private const val KEY_TRIP_ACTIVE = "shopping_trip_active" // in-store trip (#108)
    private const val KEY_PROFILE = "nutrition_profile"          // JSON profile (#110)
    private const val KEY_FREEZE = "streak_freezes"              // freeze tokens (#120)
    private const val KEY_FREEZE_MONTH = "streak_freeze_month"   // YYYYMM of last grant
    private const val KEY_WEEKLY_FLOOR = "weekly_floor"          // 1..7 cooks/week (#120)
    private const val KEY_FREEZE_DECLINED = "streak_freeze_declined" // ISO day refused (#120)
    private const val KEY_ALLERGENS = "allergens"              // JSON set (#112)
    private const val KEY_ALLERGEN_HIDE = "allergen_hide"      // search-wide hide toggle (#112)

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

    private val _convertFavorite = MutableStateFlow<String?>(null)
    val convertFavorite: StateFlow<String?> = _convertFavorite

    // ── Shopping aisles + trip mode (#108) ──────────────────────────
    private val _aisleOrder = MutableStateFlow<List<String>>(emptyList())
    val aisleOrder: StateFlow<List<String>> = _aisleOrder

    private val _aisleRenames = MutableStateFlow<Map<String, String>>(emptyMap())
    val aisleRenames: StateFlow<Map<String, String>> = _aisleRenames

    private val _aisleHidden = MutableStateFlow<Set<String>>(emptySet())
    val aisleHidden: StateFlow<Set<String>> = _aisleHidden

    private val _tripActive = MutableStateFlow(false)
    val tripActive: StateFlow<Boolean> = _tripActive

    /** Calorie/macro profile (#110); default = «بدون هدف» (totals only). */
    private val _profile = MutableStateFlow(DailyBudget.Profile())
    val profile: StateFlow<DailyBudget.Profile> = _profile

    // ── Cook streak (#120) ─────────────────────────────────────────
    private val _freezes = MutableStateFlow(1)
    val freezes: StateFlow<Int> = _freezes
    private val _weeklyFloor = MutableStateFlow(3)
    val weeklyFloor: StateFlow<Int> = _weeklyFloor
    private val _freezeDeclinedDay = MutableStateFlow("")
    val freezeDeclinedDay: StateFlow<String> = _freezeDeclinedDay

    // ── Allergy profile (#112) ─────────────────────────────────────
    private val _allergens = MutableStateFlow<Set<String>>(emptySet())
    val allergens: StateFlow<Set<String>> = _allergens
    private val _allergenHide = MutableStateFlow(false)
    val allergenHide: StateFlow<Boolean> = _allergenHide

    private val js = kotlinx.serialization.json.Json

    private fun loadList(key: String): List<String> =
        runCatching {
            js.decodeFromString<List<String>>(prefs.getString(key, "[]") ?: "[]")
        }.getOrDefault(emptyList())

    private fun loadMap(key: String): Map<String, String> =
        runCatching {
            js.decodeFromString<Map<String, String>>(prefs.getString(key, "{}") ?: "{}")
        }.getOrDefault(emptyMap())

    private fun loadSet(key: String): Set<String> = loadList(key).toSet()

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
        _convertFavorite.value = prefs.getString(KEY_CONVERT_FAV, null)
        _aisleOrder.value = loadList(KEY_AISLE_ORDER)
        _aisleRenames.value = loadMap(KEY_AISLE_RENAMES)
        _aisleHidden.value = loadSet(KEY_AISLE_HIDDEN)
        _tripActive.value = prefs.getBoolean(KEY_TRIP_ACTIVE, false)
        _profile.value = prefs.getString(KEY_PROFILE, null)
            ?.let { runCatching { js.decodeFromString(DailyBudget.Profile.serializer(), it) }.getOrNull() }
            ?: DailyBudget.Profile()
        _freezes.value = prefs.getInt(KEY_FREEZE, 1).coerceAtLeast(0)
        _weeklyFloor.value = prefs.getInt(KEY_WEEKLY_FLOOR, 3).coerceIn(1, 7)
        _freezeDeclinedDay.value = prefs.getString(KEY_FREEZE_DECLINED, "") ?: ""
        _allergens.value = loadSet(KEY_ALLERGENS)
        _allergenHide.value = prefs.getBoolean(KEY_ALLERGEN_HIDE, false)
        grantFreezeIfNeeded()
    }

    /**
     * Monthly freeze grant (#120). One token on the 1st, bank capped at
     * [StreakMath.MAX_FREEZE_BANK]; never granted twice in the same month.
     */
    fun grantFreezeIfNeeded(month: Int = java.time.LocalDate.now().monthValue): Boolean {
        val last = prefs.getInt(KEY_FREEZE_MONTH, -1)
        if (last == month) return false
        val (next, _) = com.erfanbagheri.tahdig.util.StreakMath.grantMonthly(
            current = _freezes.value,
            lastGrantMonth = last.takeIf { it in 1..12 },
            thisMonth = month,
        )
        prefs.edit().putInt(KEY_FREEZE, next).putInt(KEY_FREEZE_MONTH, month).apply()
        _freezes.value = next
        return true
    }

    /** Spend one freeze (user accepted the prompt) — floor is never below 0. */
    fun spendFreeze() {
        val next = (_freezes.value - 1).coerceAtLeast(0)
        prefs.edit().putInt(KEY_FREEZE, next).apply()
        _freezes.value = next
    }

    /** Record that the freeze prompt was declined for [isoDay] — decide again tomorrow. */
    fun declineFreeze(isoDay: String) {
        prefs.edit().putString(KEY_FREEZE_DECLINED, isoDay).apply()
        _freezeDeclinedDay.value = isoDay
    }

    /** Weekly floor goal, cooks per Saturday-start week (#120). */
    fun setWeeklyFloor(floor: Int) {
        val v = floor.coerceIn(1, 7)
        prefs.edit().putInt(KEY_WEEKLY_FLOOR, v).apply()
        _weeklyFloor.value = v
    }

    /** Allergen multi-select (#112); values come from the seed's own taxonomy. */
    fun setAllergens(set: Set<String>) {
        prefs.edit()
            .putString(KEY_ALLERGENS, js.encodeToString(ListSerializer(String.serializer()), set.toList()))
            .apply()
        _allergens.value = set
    }

    /** Search-wide hide toggle (#112); the detail band shows regardless. */
    fun setAllergenHide(on: Boolean) {
        prefs.edit().putBoolean(KEY_ALLERGEN_HIDE, on).apply()
        _allergenHide.value = on
    }

    /** Persist the full aisle-manager state in one write (#108). */
    fun setAisleConfig(order: List<String>, renames: Map<String, String>, hidden: Set<String>) {
        prefs.edit()
            .putString(KEY_AISLE_ORDER, js.encodeToString(ListSerializer(String.serializer()), order))
            .putString(KEY_AISLE_RENAMES, js.encodeToString(MapSerializer(String.serializer(), String.serializer()), renames))
            .putString(KEY_AISLE_HIDDEN, js.encodeToString(ListSerializer(String.serializer()), hidden.toList()))
            .apply()
        _aisleOrder.value = order
        _aisleRenames.value = renames
        _aisleHidden.value = hidden
    }

    /** Trip mode is a light flag: start snapshots in the VM, end archives there. */
    fun setTripActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_TRIP_ACTIVE, active).apply()
        _tripActive.value = active
    }

    /** Persist the nutrition profile (#110); edits recompute the budget. */
    fun setProfile(p: DailyBudget.Profile) {
        prefs.edit().putString(KEY_PROFILE, js.encodeToString(DailyBudget.Profile.serializer(), p)).apply()
        _profile.value = p
    }

    /** Last-used converter pair, "from|to" key (#103) — survives restart. */
    fun setConvertFavorite(key: String) {
        prefs.edit().putString(KEY_CONVERT_FAV, key).apply()
        _convertFavorite.value = key
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
