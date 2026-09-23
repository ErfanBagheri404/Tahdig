package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FavoriteEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.HistoryEntity
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.util.DailyBudget
import com.erfanbagheri.tahdig.util.NutritionDay
import com.erfanbagheri.tahdig.util.NutritionLog
import com.erfanbagheri.tahdig.util.AllergenDetector
import com.erfanbagheri.tahdig.util.LeftoverMatcher
import com.erfanbagheri.tahdig.util.MealTimeHelper
import com.erfanbagheri.tahdig.util.OccasionRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import com.erfanbagheri.tahdig.util.ExpiryMath
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val db = TahdigDatabase.getInstance(app)
    private val foodDao = db.foodDao()
    private val historyDao = db.historyDao()
    private val favoriteDao = db.favoriteDao()
    private val categoryDao = db.categoryDao()

    // ── current suggestion ──────────────────────────────────────────
    private val _suggestion = MutableStateFlow<FoodEntity?>(null)
    val suggestion: StateFlow<FoodEntity?> = _suggestion.asStateFlow()

    // ── leftover prompt ────────────────────────────────────────────
    /** Dishes suggested from leftovers. Empty = no card shown. */
    private val _leftoverSuggestions = MutableStateFlow<List<FoodEntity>>(emptyList())
    val leftoverSuggestions: StateFlow<List<FoodEntity>> = _leftoverSuggestions.asStateFlow()

    // ── UI state ────────────────────────────────────────────────────
    private val _mealLabel = MutableStateFlow(MealTimeHelper.currentLabel())
    val mealLabel: StateFlow<String> = _mealLabel.asStateFlow()

    private val _historyIds = MutableStateFlow<List<Long>>(emptyList())
    val historyIds: StateFlow<List<Long>> = _historyIds.asStateFlow()

    /** true when the current suggestion is favorited (is_blocked = false row exists). */
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    /** Deterministic dish of the day — same dish all day, stable across restarts. */
    private val _dishOfDay = MutableStateFlow<FoodEntity?>(null)
    val dishOfDay: StateFlow<FoodEntity?> = _dishOfDay.asStateFlow()

    // ── occasion shelf (#88) ────────────────────────────────────────
    /** Active occasion today, or null — null hides the shelf entirely (AC). */
    private val _occasion = MutableStateFlow(
        OccasionRegistry.activeOn(LocalDate.now())
    )
    val occasion: StateFlow<com.erfanbagheri.tahdig.util.Occasion?> = _occasion.asStateFlow()

    private val _occasionDishes = MutableStateFlow<List<FoodEntity>>(emptyList())
    val occasionDishes: StateFlow<List<FoodEntity>> = _occasionDishes.asStateFlow()

    // ── Morning expiry summary (#106) ───────────────────────────────
    /** «۳ قلم تا ۲ روز آینده: …» from Room alone; "" hides the card entirely. */
    val expirySummary: StateFlow<String> = db.pantryDao().observeAll()
        .map { list ->
            val now = System.currentTimeMillis()
            ExpiryMath.summary(
                list.filter { ExpiryMath.isUrgent(ExpiryMath.daysTo(it.expiresAt, now)) }
                    .sortedBy { it.expiresAt }
                    .map { it.item },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        refreshMealLabel()
        loadHistory()
        roll()
        loadDishOfDay()
        loadOccasion()
    }

    /** Pick today's dish from the day-of-year index — no DB change, no extra screen. */
    fun loadDishOfDay() {
        viewModelScope.launch {
            val all = foodDao.observeAll().first()
            if (all.isNotEmpty()) {
                _dishOfDay.value = all[LocalDate.now().dayOfYear % all.size]
            }
        }
    }

    /** Refresh day-dependent state (call on foreground/resume) — midnight-safe. */
    fun refreshDay() {
        refreshMealLabel()
        loadDishOfDay()
        loadOccasion()
        loadNutritionDay()
    }

    /**
     * The day key the totals flow is bound to (#110). Re-assigned on resume so
     * crossing midnight rebuilds the query instead of reporting yesterday.
     */
    private val _dayTick = MutableStateFlow(NutritionLog.dayKey())

    /**
     * Today's eaten-vs-target state for the «امروز» card (#110). Day-keyed so
     * midnight rolls the card over on its own; the budget recomputes live from
     * the stored profile, so a Settings edit updates the ring without a reload.
     */
    val nutritionDay: StateFlow<NutritionDay> = combine(
        _dayTick.flatMapLatest { day -> db.nutritionLogDao().observeTotals(day) },
        SettingsStore.profile,
    ) { totals, profile ->
        val budget = DailyBudget.budget(profile)
        NutritionDay(
            consumedCal = totals.cal,
            consumedProtein = totals.pro,
            consumedFat = totals.fat,
            consumedCarbs = totals.carb,
            targetCal = if (profile.hasGoal) budget else 0,
            macroTarget = if (profile.hasGoal) DailyBudget.macroSplit(budget) else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutritionDay())

    fun loadNutritionDay() { _dayTick.value = NutritionLog.dayKey() }

    /** UI bundle for #120: streak state plus whether the freeze prompt shows. */
    data class StreakUi(
        val state: com.erfanbagheri.tahdig.util.StreakMath.State,
        val promptVisible: Boolean,
    )

    /**
     * Cook streak (#120). History stamps → local days → [StreakMath.compute].
     * The prompt only appears when a decline hasn't been recorded today AND a
     * freeze is the difference between holding and resetting — a met weekly
     * floor never nags.
     */
    val streakUi: StateFlow<StreakUi> = combine(
        historyDao.observeRecent(1000),
        SettingsStore.weeklyFloor,
        SettingsStore.freezes,
        SettingsStore.freezeDeclinedDay,
        _dayTick,
    ) { rows, floor, freezes, declined, _ ->
        val today = LocalDate.now()
        val cookedDays = rows.mapTo(mutableSetOf()) {
            java.time.Instant.ofEpochMilli(it.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }
        val declinedToday = declined == today.toString()
        val state = com.erfanbagheri.tahdig.util.StreakMath.compute(
            cookedDays, today, if (declinedToday) 0 else freezes, floor,
        )
        val prompt = if (declinedToday || freezes <= 0) {
            false
        } else {
            val withoutFreeze =
                com.erfanbagheri.tahdig.util.StreakMath.compute(cookedDays, today, 0, floor)
            state.current > 0 && withoutFreeze.current == 0
        }
        StreakUi(state, prompt)
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        StreakUi(com.erfanbagheri.tahdig.util.StreakMath.State(0, 0, 1, 0, 3), false),
    )

    /** Spend a freeze on the prompt's «بله» — recompute runs via the same flow. */
    fun acceptFreeze() = SettingsStore.spendFreeze()

    /** «نه» — don't ask again today (AC: decide again tomorrow). */
    fun declineFreeze() = SettingsStore.declineFreeze(LocalDate.now().toString())

    /** Re-evaluate today's occasion and load its curated dish strip (#88). */
    fun loadOccasion() {
        _occasion.value = OccasionRegistry.activeOn(LocalDate.now())
        val occ = _occasion.value ?: run { _occasionDishes.value = emptyList(); return }
        viewModelScope.launch {
            _occasionDishes.value = foodDao.byIds(occ.dishes)
        }
    }

    /** Re-read meal bucket (call from a timer or recomposition). */
    fun refreshMealLabel() {
        _mealLabel.value = MealTimeHelper.currentLabel()
    }

    /** Pick a random food, weighted to avoid recent history. */
    fun roll() {
        viewModelScope.launch {
            val bucket = MealTimeHelper.currentBucket()
            val recent = historyDao.recentFoodIds(15)

            var pick = foodDao.randomByMealTime(bucket, null)

            if (pick != null && pick.id in recent) {
                repeat(4) {
                    val candidate = foodDao.randomByMealTime(bucket, null)
                    if (candidate != null && candidate.id !in recent) {
                        pick = candidate
                        return@repeat
                    }
                }
            }

            // Fallback: random from ANY bucket if current bucket is empty
            if (pick == null) {
                pick = foodDao.randomAny(1).firstOrNull()
            }

            // Smart weighting: 30% chance favor a favorited dish (skip if already favorited)
            if (pick != null && !isFavorited(pick.id) && Math.random() < 0.30) {
                val favPicks = favoriteDao.observeFavoritedFoods().first()
                    .filter { it.mealTime.contains(bucket, ignoreCase = true) }
                if (favPicks.isNotEmpty()) {
                    pick = favPicks.random()
                }
            }

            _suggestion.value = pick
            _isFavorite.value = pick?.let { isFavorited(it.id) } ?: false

            pick?.let { food ->
                historyDao.insert(
                    HistoryEntity(
                        foodId = food.id,
                        timestamp = System.currentTimeMillis(),
                        mealTime = bucket,
                    )
                )
                loadHistory()
            }
        }
    }

    /** Toggle favorite status on current suggestion. */
    fun toggleFavorite() {
        viewModelScope.launch {
            val food = _suggestion.value ?: return@launch
            if (isFavorited(food.id)) {
                favoriteDao.deleteByFoodId(food.id)
                _isFavorite.value = false
            } else {
                favoriteDao.upsert(FavoriteEntity(foodId = food.id, isBlocked = false))
                _isFavorite.value = true
            }
        }
    }

    /** Toggle blocked status on current suggestion. */
    fun toggleBlocked() {
        viewModelScope.launch {
            _suggestion.value?.let { food ->
                foodDao.setBlocked(food.id, !food.isBlocked)
                roll()
            }
        }
    }

    /**
     * Leftovers for an ARBITRARY dish (#98: cooked via cook-mode's done state —
     * the home suggestion may be a different dish entirely). Surfaces the same
     * prompt card as [markCooked].
     */
    fun showLeftoversFor(food: FoodEntity) {
        viewModelScope.launch {
            val all = foodDao.observeAll().first()
            val profile = SettingsStore.allergens.value
            val hide = SettingsStore.allergenHide.value
            // Allergen exclusion (#112): when the toggle is on the matcher must
            // not RANK a conflicting dish — filtering after ranking would let one
            // slip through when the list is shorter than its top-N.
            _leftoverSuggestions.value = LeftoverMatcher.findLeftovers(food, all)
                .filterNot {
                    AllergenDetector.shouldHide(
                        hide, profile, AllergenDetector.detect(it.ingredients),
                    )
                }
        }
    }

    /**
     * User says they cooked the current dish. Shows leftover suggestions:
     * dishes sharing ≥2 ingredients with the cooked dish.
     */
    fun markCooked() {
        _suggestion.value?.let { food ->
            showLeftoversFor(food)
            NutritionLog.logCooked(db, food, viewModelScope)
        }
        roll()
    }

    /** Dismiss the leftover suggestion card. */
    fun dismissLeftover() {
        _leftoverSuggestions.value = emptyList()
    }

    private suspend fun isFavorited(foodId: Long): Boolean =
        favoriteDao.getByFoodId(foodId)?.isBlocked == false

    private fun loadHistory() {
        viewModelScope.launch {
            _historyIds.value = historyDao.recentFoodIds(15)
        }
    }
}
