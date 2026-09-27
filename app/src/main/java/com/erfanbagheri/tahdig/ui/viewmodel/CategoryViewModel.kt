package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.CategoryEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.UndoHub
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CategoryViewModel(app: Application) : AndroidViewModel(app) {
    private val db = TahdigDatabase.getInstance(app)

    val categories: StateFlow<List<CategoryEntity>> = db.categoryDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Dishes under a given category, for the category detail view. */
    fun dishesByCategory(categoryId: Long) = db.foodDao().search("", categoryId)

    /**
     * #81 batch «حذف»: the single-dish call in a loop, one undo entry for the
     * whole batch (#127's rule — one snackbar per user action). Hidden, not
     * destroyed: the catalogue is seed data and the app never hard-deletes a
     * dish; hiding is what the swipe on Home already does.
     */
    fun deleteDishes(foodIds: List<Long>) {
        if (foodIds.isEmpty()) return
        viewModelScope.launch {
            foodIds.forEach { db.foodDao().setBlocked(it, true) }
            UndoHub.arm("${PersianText.toPersianDigits(foodIds.size)} غذا حذف شد") {
                viewModelScope.launch { foodIds.forEach { db.foodDao().setBlocked(it, false) } }
            }
        }
    }
}
