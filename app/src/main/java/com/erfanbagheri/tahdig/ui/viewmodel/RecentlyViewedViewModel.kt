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

class RecentlyViewedViewModel(app: Application) : AndroidViewModel(app) {
    private val recentViewDao = TahdigDatabase.getInstance(app).recentViewDao()

    val recent: StateFlow<List<FoodEntity>> = recentViewDao.recentFoods(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordView(foodId: Long) {
        viewModelScope.launch { recentViewDao.recordView(foodId) }
    }
}
