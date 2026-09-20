package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MealPlanViewModel(app: Application) : AndroidViewModel(app) {
    private val mealPlanDao = TahdigDatabase.getInstance(app).mealPlanDao()
    private val foodDao = TahdigDatabase.getInstance(app).foodDao()

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

    fun observeSlots(dayIndex: Int): StateFlow<List<com.erfanbagheri.tahdig.data.local.entity.MealPlanEntity>> =
        mealPlanDao.observePlan().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
