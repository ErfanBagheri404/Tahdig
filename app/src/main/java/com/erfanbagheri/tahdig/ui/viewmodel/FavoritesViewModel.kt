package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.util.UndoHub
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
        viewModelScope.launch {
            val row = favoriteDao.getByFoodId(foodId) ?: return@launch
            favoriteDao.deleteByFoodId(foodId)
            UndoHub.arm("از علاقه‌مندی‌ها حذف شد") {
                viewModelScope.launch { favoriteDao.upsert(row) }
            }
        }
    }

    fun unblock(foodId: Long) {
        viewModelScope.launch {
            val row = favoriteDao.getByFoodId(foodId) ?: return@launch
            favoriteDao.deleteByFoodId(foodId)
            UndoHub.arm("مسدودی برداشته شد") {
                viewModelScope.launch { favoriteDao.upsert(row) }
            }
        }
    }

    /**
     * Unblock everything, restorable for the undo window (#127). The inverse
     * re-inserts the exact rows, so a mis-tap does not cost the user their
     * whole block list.
     */
    fun clearBlocked() {
        viewModelScope.launch {
            val rows = favoriteDao.blockedRows()
            if (rows.isEmpty()) return@launch
            favoriteDao.clearBlocked()
            UndoHub.arm("${rows.size} مسدودی برداشته شد") {
                viewModelScope.launch { rows.forEach { favoriteDao.upsert(it) } }
            }
        }
    }
}
