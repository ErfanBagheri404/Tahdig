package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.MealPlanEntity
import com.erfanbagheri.tahdig.util.PlanGenerator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MealPlanViewModel(app: Application) : AndroidViewModel(app) {
    private val db = TahdigDatabase.getInstance(app)
    private val mealPlanDao = db.mealPlanDao()
    private val foodDao = db.foodDao()
    private val favoriteDao = db.favoriteDao()
    private val historyDao = db.historyDao()

    private val _currentDay = kotlinx.coroutines.flow.MutableStateFlow(0)
    val currentDay: StateFlow<Int> = _currentDay

    fun setDay(day: Int) { _currentDay.value = day }

    val foods: StateFlow<List<FoodEntity>> = foodDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun assignFood(dayIndex: Int, slot: String, foodId: Long) {
        viewModelScope.launch { mealPlanDao.setSlot(dayIndex, slot, foodId) }
    }

    fun clearFood(dayIndex: Int, slot: String) {
        viewModelScope.launch { mealPlanDao.clearSlot(dayIndex, slot) }
    }

    // ── generator ───────────────────────────────────────────────────
    private val _generating = kotlinx.coroutines.flow.MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating

    /** Plan grid before an auto-fill, for the undo affordance. */
    private var undoSnapshot: List<MealPlanEntity>? = null

    private val _canUndo = kotlinx.coroutines.flow.MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo

    /**
     * Fill the week. Existing slots are kept — only empty ones are generated, so a tap
     * never throws away manual choices. Hold the "regenerate all" path for a long-press.
     */
    fun generateWeek(regenerateAll: Boolean = false) {
        viewModelScope.launch {
            _generating.value = true
            val existing = mealPlanDao.observePlan().first()
            undoSnapshot = existing
            val keep = if (regenerateAll) {
                emptySet<Pair<Int, String>>()
            } else {
                existing.map { it.dayIndex to it.mealSlot }.toSet()
            }

            val catalog = foodDao.observeAll().first()
            val favorites = favoriteDao.observeFavoritedFoods().first().map { it.id }.toSet()
            val recent = historyDao.recentFoodIds(15)

            val plan = PlanGenerator.generate(
                foods = catalog,
                favoritesIds = favorites,
                recentIds = recent,
                keepSlots = keep,
                seed = weekSeed(),
            )

            if (regenerateAll) mealPlanDao.clearAll()
            plan.forEach { (key, foodId) ->
                mealPlanDao.setSlot(key.first, key.second, foodId)
            }
            _canUndo.value = true
            _generating.value = false
        }
    }

    /** Restore the grid as it was before the last generation. */
    fun undoGenerate() {
        val snapshot = undoSnapshot ?: return
        viewModelScope.launch {
            mealPlanDao.clearAll()
            snapshot.forEach { mealPlanDao.setSlot(it.dayIndex, it.mealSlot, it.foodId) }
            undoSnapshot = null
            _canUndo.value = false
        }
    }

    /** Monday-based week number — stable within a week so regenerating the same week agrees. */
    private fun weekSeed(): Long =
        java.time.LocalDate.now().toEpochDay() / 7L

    // One cached flow per day so switching days reuses the same subscription
    private val slotCache = mutableMapOf<Int, StateFlow<List<MealPlanEntity>>>()

    fun observeSlots(dayIndex: Int): StateFlow<List<MealPlanEntity>> = slotCache.getOrPut(dayIndex) {
        mealPlanDao.observeSlotsForDay(dayIndex)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
}
