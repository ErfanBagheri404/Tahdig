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
    private const val KEY_BADGES_SEEN = "badges_seen"          // JSON set (#121)
    private const val KEY_ALLERGENS = "allergens"              // JSON set (#112)
    private const val KEY_ALLERGEN_HIDE = "allergen_hide"      // search-wide hide toggle (#112)
    private const val KEY_SHAKE_SPIN = "shake_spin"            // shake-to-spin roulette (#123)
    private const val KEY_SCANNER = "scanner_enabled"            // barcode scanner (#116), on by default
    private const val KEY_CAP_PRESET = "cap_preset"              // preset name or "" (#113)
    private const val KEY_CAP_CUSTOM = "cap_custom"              // JSON {NUTRIENT: value} (#113)
    private const val KEY_HALAL_STRICT = "halal_strict"          // hide flagged dishes (#118)
    private const val KEY_GLASS_ML = "water_glass_ml"              // glass size (#115)
    private const val KEY_WATER_TARGET = "water_target_ml"          // manual target, -1=auto (#115)
    private const val KEY_CARRY_OVER = "calorie_carry_over"        // roll unspent into tomorrow (#114)
    private const val KEY_PREGNANCY = "pregnancy_mode"                 // pregnancy/breastfeeding mode (#119)
    private const val KEY_CAFFEINE_CAP = "caffeine_cap_mg"             // manual cap, 0 = default (#119)
    private const val KEY_NOTIFY_HOUR = "notify_hour"             // 0 = unset, else 1..23 (#122)
    private const val KEY_QUIET_ON = "quiet_hours_on"             // quiet window enabled (#122)
    private const val KEY_QUIET_FROM = "quiet_from_min"           // minutes since midnight (#122)
    private const val KEY_QUIET_UNTIL = "quiet_until_min"         // minutes since midnight (#122)
    private const val KEY_IDLE_TIER = "notify_idle_tier"          // fired idle tier, hours (#122)
    private const val KEY_NOTIF_PRIMED = "notif_primed"           // permission asked once (#122)
    private const val KEY_NOTIF_DENIED = "notif_denied"           // denial remembered (#122)
    private const val KEY_PHOTO_PROMPT = "photo_prompt_enabled"   // daily lunch photo reminder (#125)
    private const val KEY_PHOTO_PROMPT_HOUR = "photo_prompt_hour" // 0..23, default 13 (#125)

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

    // ── Pregnancy mode + caffeine cap (#119) ───────────────────────
    // Off by default: the safety warnings and the tighter cap only apply
    // when the user opts in. Single toggle — separate submodes for
    // pregnancy vs breastfeeding carry the same caps and warnings.
    private val _pregnancyMode = MutableStateFlow(false)
    val pregnancyMode: StateFlow<Boolean> = _pregnancyMode

    /** Manual caffeine cap in mg; 0 = default (400, or 200 in the mode). */
    private val _caffeineCap = MutableStateFlow(0)
    val caffeineCap: StateFlow<Int> = _caffeineCap

    // ── Smart notifications (#122) ──────────────────────────────
    // The reminder hour is the user's pick or unset (0); quiet hours are a
    // single [from, until) window in minutes-since-midnight; the idle stamp
    // holds the highest tier already fired this period (NotifyMath units).
    private val _notifyHour = MutableStateFlow(0)
    val notifyHour: StateFlow<Int> = _notifyHour

    private val _quietOn = MutableStateFlow(false)
    val quietOn: StateFlow<Boolean> = _quietOn

    private val _quietFromMin = MutableStateFlow(22 * 60)
    val quietFromMin: StateFlow<Int> = _quietFromMin

    private val _quietUntilMin = MutableStateFlow(7 * 60)
    val quietUntilMin: StateFlow<Int> = _quietUntilMin

    private val _idleTierHours = MutableStateFlow(0.0)
    val idleTierHours: StateFlow<Double> = _idleTierHours

    private val _notifPrimed = MutableStateFlow(false)
    val notifPrimed: StateFlow<Boolean> = _notifPrimed

    private val _notifDenied = MutableStateFlow(false)
    val notifDenied: StateFlow<Boolean> = _notifDenied

    // ── Daily photo prompt (#125) ───────────────────────────────
    // Off by default and entirely optional: photos are a nice-to-have, and a
    // nudge about them must never feel like an obligation. Hour defaults to
    // lunch (13:00) because the issue's copy is «یادآور عکس ناهار».
    private val _photoPrompt = MutableStateFlow(false)
    val photoPrompt: StateFlow<Boolean> = _photoPrompt

    private val _photoPromptHour = MutableStateFlow(13)
    val photoPromptHour: StateFlow<Int> = _photoPromptHour

    // ── Calorie carry-over (#114) ──────────────────────────────────
    // Off by default: rolling budget across days is a personal choice,
    // and the default must stay "today's target is today's target".
    private val _carryOver = MutableStateFlow(false)
    val carryOver: StateFlow<Boolean> = _carryOver

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

    // ── Badges (#121) ──────────────────────────────────────────────
    // Only the SEEN set is persisted. The unlocked set is recomputed from
    // history, so a prefs wipe can never lose a badge the user earned, and
    // a "seen" entry can never claim a badge that was never unlocked.
    private val _badgesSeen = MutableStateFlow<Set<String>>(emptySet())
    val badgesSeen: StateFlow<Set<String>> = _badgesSeen
    private val _allergenHide = MutableStateFlow(false)
    val allergenHide: StateFlow<Boolean> = _allergenHide

    // ── Halal-style strict mode (#118) ──────────────────────────────
    // Off by default: the warning row alone never hides anything.
    private val _halalStrict = MutableStateFlow(false)
    val halalStrict: StateFlow<Boolean> = _halalStrict

    /** Hide flagged dishes in search (#118). Persists immediately. */
    fun setHalalStrict(on: Boolean) {
        _halalStrict.value = on
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_HALAL_STRICT, on).apply()
    }

    // Pregnancy / breastfeeding mode (#119): the tighter 200 mg cap and
    // the food-safety warnings read this same flow.
    fun setPregnancyMode(on: Boolean) {
        _pregnancyMode.value = on
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_PREGNANCY, on).apply()
    }

    /**
     * Manual caffeine cap in mg (#119). 0 restores the default. Note the
     * UI's slider is a convenience, not the source of truth: the cap
     * actually applied is [Caffeine.effectiveCap], which lets pregnancy
     * mode override whatever is stored here.
     */
    fun setCaffeineCap(mg: Int) {
        val v = mg.coerceIn(0, 2000)
        _caffeineCap.value = v
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_CAFFEINE_CAP, v).apply()
    }

    /** Roll unspent calories into tomorrow's budget (#114). */
    fun setCarryOver(on: Boolean) {
        _carryOver.value = on
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_CARRY_OVER, on).apply()
    }

    // ── Shake-to-spin roulette (#123) ──────────────────────────────
    private val _shakeSpin = MutableStateFlow(false)
    val shakeSpin: StateFlow<Boolean> = _shakeSpin

    // ── Barcode scanner (#116) ───────────────────────────────────
    // On by default: disabling is the exception, for users who never want
    // the camera entry point in search.
    private val _scannerEnabled = MutableStateFlow(true)
    val scannerEnabled: StateFlow<Boolean> = _scannerEnabled

    // ── Water + weight (#115) ─────────────────────────────────────
    // Glass size and the manual target are prefs, not VM-only state: the
    // stepper must add the same amount tomorrow that it added today, and
    // the weight-derived target must not overwrite a target the user set.
    private val _glassSizeMl = MutableStateFlow(200)
    val glassSizeMl: StateFlow<Int> = _glassSizeMl

    /** null = derive from weight. Stored as -1 so one Int key covers both. */
    private val _waterTargetMl = MutableStateFlow<Int?>(null)
    val waterTargetMl: StateFlow<Int?> = _waterTargetMl

    // ── Nutrient caps (#113) ─────────────────────────────────────
    // SettingsStore over Room here: no DB migration, no schema, and the issue
    // asks for a prefs-plus-entity approach. One key for the preset, one map
    // for overrides. The pure merge rule lives in NutrientCaps.
    private val _capPreset = MutableStateFlow<String?>(null)
    val capPreset: StateFlow<String?> = _capPreset
    private val _capCustom = MutableStateFlow<Map<String, Double>>(emptyMap())
    val capCustom: StateFlow<Map<String, Double>> = _capCustom

    /** Pick a preset by enum name, or null to clear — persists immediately. */
    fun setCapPreset(name: String?) {
        _capPreset.value = name
        if (::prefs.isInitialized) prefs.edit().putString(KEY_CAP_PRESET, name ?: "").apply()
    }

    /** Set one personal override (mg/g); non-positive removes it. Persists immediately. */
    fun setCapCustom(nutrient: String, value: Double) {
        val next = _capCustom.value.toMutableMap()
        if (value > 0.0) next[nutrient] = value else next.remove(nutrient)
        _capCustom.value = next
        if (::prefs.isInitialized) {
            js.encodeToString(
                MapSerializer(String.serializer(), Double.serializer()), next,
            ).let { prefs.edit().putString(KEY_CAP_CUSTOM, it).apply() }
        }
    }

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

    /** {NUTRIENT: mg/g} doubles; non-finite junk is dropped, never trusted blindly. */
    private fun loadDoubleMap(key: String): Map<String, Double> =
        loadMap(key).mapNotNull { (k, v) ->
            v.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }?.let { k to it }
        }.toMap()

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
        _badgesSeen.value = loadSet(KEY_BADGES_SEEN)
        _allergenHide.value = prefs.getBoolean(KEY_ALLERGEN_HIDE, false)
        _halalStrict.value = prefs.getBoolean(KEY_HALAL_STRICT, false)
        _carryOver.value = prefs.getBoolean(KEY_CARRY_OVER, false)
        _pregnancyMode.value = prefs.getBoolean(KEY_PREGNANCY, false)
        _caffeineCap.value = prefs.getInt(KEY_CAFFEINE_CAP, 0).coerceIn(0, 2000)
        _notifyHour.value = prefs.getInt(KEY_NOTIFY_HOUR, 0).let { if (it in 1..23) it else 0 }
        _quietOn.value = prefs.getBoolean(KEY_QUIET_ON, false)
        _quietFromMin.value = prefs.getInt(KEY_QUIET_FROM, 22 * 60).coerceIn(0, 24 * 60)
        _quietUntilMin.value = prefs.getInt(KEY_QUIET_UNTIL, 7 * 60).coerceIn(0, 24 * 60)
        _idleTierHours.value = prefs.getFloat(KEY_IDLE_TIER, 0f).toDouble()
        _notifPrimed.value = prefs.getBoolean(KEY_NOTIF_PRIMED, false)
        _notifDenied.value = prefs.getBoolean(KEY_NOTIF_DENIED, false)
        _photoPrompt.value = prefs.getBoolean(KEY_PHOTO_PROMPT, false)
        _photoPromptHour.value = prefs.getInt(KEY_PHOTO_PROMPT_HOUR, 13).let { if (it in 1..23) it else 13 }
        _shakeSpin.value = prefs.getBoolean(KEY_SHAKE_SPIN, false)
        _scannerEnabled.value = prefs.getBoolean(KEY_SCANNER, true)
        _glassSizeMl.value = prefs.getInt(KEY_GLASS_ML, 200)
        _waterTargetMl.value = prefs.getInt(KEY_WATER_TARGET, -1).takeIf { it > 0 }
        _capPreset.value = prefs.getString(KEY_CAP_PRESET, "")?.ifBlank { null }
        _capCustom.value = loadDoubleMap(KEY_CAP_CUSTOM)
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

    /** Badge ids the user has already been shown (#121). */
    fun setBadgesSeen(set: Set<String>) {
        prefs.edit()
            .putString(
                KEY_BADGES_SEEN,
                js.encodeToString(ListSerializer(String.serializer()), set.toList()),
            )
            .apply()
        _badgesSeen.value = set
    }

    /** User's reminder hour, 1..23; 0 clears it back to the evening default. */
    fun setNotifyHour(hour: Int) {
        val v = if (hour in 1..23) hour else 0
        prefs.edit().putInt(KEY_NOTIFY_HOUR, v).apply()
        _notifyHour.value = v
    }

    /** Quiet hours master switch. Timers are never affected by this (#122). */
    fun setQuietOn(on: Boolean) {
        prefs.edit().putBoolean(KEY_QUIET_ON, on).apply()
        _quietOn.value = on
    }

    /** Quiet window edges, minutes since midnight; from==until disables the window. */
    fun setQuietWindow(fromMin: Int, untilMin: Int) {
        val f = fromMin.coerceIn(0, 24 * 60)
        val u = untilMin.coerceIn(0, 24 * 60)
        prefs.edit().putInt(KEY_QUIET_FROM, f).putInt(KEY_QUIET_UNTIL, u).apply()
        _quietFromMin.value = f
        _quietUntilMin.value = u
    }

    /**
     * Highest idle tier already fired this period (#122). 0 re-arms both
     * tiers — the receiver calls it after a cook so the next idle spell can
     * nudge again.
     */
    fun setIdleTierHours(hours: Double) {
        prefs.edit().putFloat(KEY_IDLE_TIER, hours.toFloat()).apply()
        _idleTierHours.value = hours
    }

    /** Record the outcome of the one post-first-action permission ask (#122). */
    fun setNotifPermission(primed: Boolean, granted: Boolean) {
        prefs.edit()
            .putBoolean(KEY_NOTIF_PRIMED, primed)
            .putBoolean(KEY_NOTIF_DENIED, primed && !granted)
            .apply()
        _notifPrimed.value = primed
        _notifDenied.value = primed && !granted
    }

    /**
     * The daily lunch photo prompt (#125). Enabling schedules the alarm
     * immediately so the very first reminder arrives today when possible;
     * disabling cancels it and stops the receiver re-arming.
     */
    fun setPhotoPrompt(context: Context, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PHOTO_PROMPT, enabled).apply()
        _photoPrompt.value = enabled
        if (enabled) {
            com.erfanbagheri.tahdig.notify.PhotoPromptScheduler.schedule(context)
        } else {
            com.erfanbagheri.tahdig.notify.PhotoPromptScheduler.cancel(context)
        }
    }

    /** The reminder's hour, 1..23; changing it re-arms the next fire. */
    fun setPhotoPromptHour(context: Context, hour: Int) {
        val v = if (hour in 1..23) hour else 13
        prefs.edit().putInt(KEY_PHOTO_PROMPT_HOUR, v).apply()
        _photoPromptHour.value = v
        if (_photoPrompt.value) {
            com.erfanbagheri.tahdig.notify.PhotoPromptScheduler.schedule(context)
        }
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

    /** Glass size in ml (#115) — the stepper's step is a multiple of this. */
    fun setGlassSizeMl(ml: Int) {
        if (ml <= 0 || ml > 2000) return
        prefs.edit().putInt(KEY_GLASS_ML, ml).apply()
        _glassSizeMl.value = ml
    }

    /** Manual water target (#115); null returns to the weight-derived one. */
    fun setWaterTargetMl(ml: Int?) {
        prefs.edit().putInt(KEY_WATER_TARGET, ml ?: -1).apply()
        _waterTargetMl.value = ml?.takeIf { it > 0 }
    }

    /** Barcode scanner (#116), on by default — hiding the search entry point. */
    fun setScannerEnabled(on: Boolean) {
        prefs.edit().putBoolean(KEY_SCANNER, on).apply()
        _scannerEnabled.value = on
    }

    /** Shake-to-spin (#123), off by default — shares the sensitivity slider. */
    fun setShakeSpin(on: Boolean) {
        prefs.edit().putBoolean(KEY_SHAKE_SPIN, on).apply()
        _shakeSpin.value = on
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
