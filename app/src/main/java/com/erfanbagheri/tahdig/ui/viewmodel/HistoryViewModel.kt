package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.HistoryWithFood
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val db = TahdigDatabase.getInstance(app)
    private val historyDao = db.historyDao()
    private val favoriteDao = db.favoriteDao()

    val historyItems: StateFlow<List<HistoryWithFood>> = historyDao.observeHistoryWithFood()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearHistory() {
        viewModelScope.launch { historyDao.clearAll() }
    }
}
