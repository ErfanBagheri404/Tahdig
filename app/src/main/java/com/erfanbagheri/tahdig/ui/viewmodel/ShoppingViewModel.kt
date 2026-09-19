package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(app: Application) : AndroidViewModel(app) {
    private val db = TahdigDatabase.getInstance(app)
    private val shoppingDao = db.shoppingDao()

    val items: StateFlow<List<ShoppingItemEntity>> = shoppingDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setChecked(id: Long, checked: Boolean) {
        viewModelScope.launch { shoppingDao.setChecked(id, checked) }
    }

    fun remove(id: Long) {
        viewModelScope.launch { shoppingDao.deleteById(id) }
    }

    fun clearChecked() {
        viewModelScope.launch { shoppingDao.clearChecked() }
    }

    fun clearAll() {
        viewModelScope.launch { shoppingDao.clearAll() }
    }

    /** Add every ingredient of [ingredients] (comma/،-separated) as a list item. */
    fun addIngredients(foodId: Long, ingredients: String) {
        val parts = ingredients
            .split(',', '،', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.isEmpty()) return
        viewModelScope.launch {
            shoppingDao.insertAll(
                parts.map { ShoppingItemEntity(foodId = foodId, item = it) }
            )
        }
    }
}
