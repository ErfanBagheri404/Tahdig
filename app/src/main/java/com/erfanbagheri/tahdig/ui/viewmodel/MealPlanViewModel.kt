package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.CalendarExport
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.MealPlanEntity
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MealPlanViewModel(app: Application) : AndroidViewModel(app) {
    private val mealPlanDao = TahdigDatabase.getInstance(app).mealPlanDao()
    private val foodDao = TahdigDatabase.getInstance(app).foodDao()

    init {
        if (!SettingsStore.isInitialized()) SettingsStore.init(app)
    }

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

    // One cached flow per day so switching days reuses the same subscription
    private val slotCache = mutableMapOf<Int, StateFlow<List<MealPlanEntity>>>()

    fun observeSlots(dayIndex: Int): StateFlow<List<MealPlanEntity>> = slotCache.getOrPut(dayIndex) {
        mealPlanDao.observeSlotsForDay(dayIndex)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val week: StateFlow<List<MealPlanEntity>> = mealPlanDao.observePlan()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // #84 auto-sync: «همگام‌سازی خودکار برنامه با تقویم» on ⇒ every plan
    // change rewrites that week's events. Idempotent via SYNC_ID dedupe, and
    // a missing permission degrades to nothing — the manual export button on
    // the plan screen is the path that asks, with a Farsi reason.
    val calendarSync: StateFlow<Boolean> = SettingsStore.calendarSync

    init {
        viewModelScope.launch {
            combine(week, calendarSync) { plan, on -> plan to on }
                .collect { (plan, on) -> if (on) autoExport(plan) }
        }
    }

    private suspend fun autoExport(plan: List<MealPlanEntity>) {
        if (!CalendarExport.hasPermission(getApplication())) return
        val foodMap = foods.value.associateBy { it.id }
        val items = plan.mapNotNull { row ->
            foodMap[row.foodId]?.let { CalendarExport.Item(row.dayIndex, row.mealSlot, it.name) }
        }
        if (items.isEmpty()) return
        withContext(Dispatchers.IO) { CalendarExport.exportWeek(getApplication(), items) }
    }
}
