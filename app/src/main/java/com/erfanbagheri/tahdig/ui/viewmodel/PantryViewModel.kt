package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.PantryItemEntity
import com.erfanbagheri.tahdig.util.PantryMatcher
import com.erfanbagheri.tahdig.util.PersianText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A dish and how much of it the pantry already covers. */
data class CookableDish(val food: FoodEntity, val coverage: Float)

/**
 * Pantry: the staples the user keeps at home, and how well they cover each dish.
 *
 * Ranking runs in memory over the whole catalogue (543 rows — cheap), so there is no
 * ingredient join table and no per-keystroke DAO query.
 */
class PantryViewModel(app: Application) : AndroidViewModel(app) {
    private val db = TahdigDatabase.getInstance(app)
    private val pantryDao = db.pantryDao()

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    val items: StateFlow<List<PantryItemEntity>> = pantryDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Every dish scored against the pantry, best first. Empty pantry = empty list
     * (a ranking against nothing is meaningless).
     */
    val ranked: StateFlow<List<CookableDish>> =
        combine(db.foodDao().observeAll(), pantryDao.observeAll()) { foods, pantry ->
            val have = pantry.map { it.item }
            if (have.isEmpty()) return@combine emptyList()
            foods.map { CookableDish(it, PantryMatcher.coverage(it.ingredients, have)) }
                .filter { it.coverage > 0f }
                .sortedWith(compareByDescending<CookableDish> { it.coverage }
                    .thenBy { it.food.prepTimeMin })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onDraftChange(text: String) { _draft.value = text }

    /** Adds the draft, splitting on comma / Farsi comma so a pasted list works. */
    fun addDraft() {
        val parts = _draft.value.split(',', '،')
            .map { PersianText.normalize(it).trim() }
            .filter { it.isNotEmpty() }
        if (parts.isEmpty()) return
        viewModelScope.launch {
            parts.forEach { pantryDao.insert(PantryItemEntity(item = it)) }
            _draft.value = ""
        }
    }

    fun remove(id: Long) = viewModelScope.launch { pantryDao.deleteById(id) }

    fun clearAll() = viewModelScope.launch { pantryDao.clearAll() }
}
