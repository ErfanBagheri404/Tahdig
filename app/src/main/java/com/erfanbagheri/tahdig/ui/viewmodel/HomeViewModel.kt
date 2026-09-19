package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FavoriteEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.HistoryEntity
import com.erfanbagheri.tahdig.util.MealTimeHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

    init {
        refreshMealLabel()
        loadHistory()
        roll()
        loadDishOfDay()
    }

    /** Pick today's dish from the day-of-year index — no DB change, no extra screen. */
    private fun loadDishOfDay() {
        viewModelScope.launch {
            val all = foodDao.observeAll().first()
            if (all.isNotEmpty()) {
                _dishOfDay.value = all[LocalDate.now().dayOfYear % all.size]
            }
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

    private suspend fun isFavorited(foodId: Long): Boolean =
        favoriteDao.getByFoodId(foodId)?.isBlocked == false

    private fun loadHistory() {
        viewModelScope.launch {
            _historyIds.value = historyDao.recentFoodIds(15)
        }
    }
}
