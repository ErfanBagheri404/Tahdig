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

class FavoritesViewModel(app: Application) : AndroidViewModel(app) {
    private val db = TahdigDatabase.getInstance(app)
    private val favoriteDao = db.favoriteDao()

    val favoritedFoods: StateFlow<List<FoodEntity>> = favoriteDao.observeFavoritedFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedFoods: StateFlow<List<FoodEntity>> = favoriteDao.observeBlockedFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFavorite(foodId: Long) {
        viewModelScope.launch { favoriteDao.deleteByFoodId(foodId) }
    }

    fun unblock(foodId: Long) {
        viewModelScope.launch { favoriteDao.deleteByFoodId(foodId) }
    }

    fun clearBlocked() {
        viewModelScope.launch { favoriteDao.clearBlocked() }
    }
}
