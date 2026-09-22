package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.ShoppingItemEntity
import com.erfanbagheri.tahdig.util.IngredientParser
import com.erfanbagheri.tahdig.util.IngredientRegistry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(app: Application) : AndroidViewModel(app) {
    private val db = TahdigDatabase.getInstance(app)
    private val shoppingDao = db.shoppingDao()
    private val mealPlanDao = db.mealPlanDao()
    private val foodDao = db.foodDao()

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
        viewModelScope.launch { mergeInto(split(ingredients), foodId) }
    }

    /**
     * Add the ingredients of every dish in the weekly plan in one pass, so a dish
     * planned twice does not double its onion and overlapping dishes share rows.
     * Plan rows span several dishes, so they carry no single [foodId].
     */
    fun addPlanIngredients() {
        viewModelScope.launch {
            val foodIds = mealPlanDao.allFoodIds()
            if (foodIds.isEmpty()) return@launch
            mergeInto(foodDao.byIds(foodIds).flatMap { split(it.ingredients) }, null)
        }
    }

    private fun split(ingredients: String) = ingredients
        .split(',', '،', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    /**
     * Merge [parts] into the existing list: rows describing the same unit+item fold into
     * one with summed quantities, and existing rows keep their id and checked state.
     */
    private suspend fun mergeInto(parts: List<String>, foodId: Long?) {
        if (parts.isEmpty()) return
        // Merge with what is already on the list so adding two dishes that both
        // need onion yields one row, not two. Existing rows keep their id and
        // checked state — only their text is rewritten with the summed quantity.
        val existing = shoppingDao.allRows()
        // Existing rows are already display text ("۳ عدد پیاز"), so re-parse them
        // to recover the unit/item identity they merge on.
        val byKey = existing.associateBy {
            val p = IngredientParser.parse(it.item)
            IngredientRegistry.mergeKeyFor(p.unit, p.item)
        }
        val merged = IngredientParser.merge(existing.map { it.item } + parts)

        val keep = mutableListOf<Pair<Long, String>>()   // id -> new text
        val add = mutableListOf<String>()
        for (m in merged) {
            val text = m.display()
            val prev = byKey[IngredientRegistry.mergeKeyFor(m.unit, m.item)]
            if (prev != null) keep += prev.id to text else add += text
        }
        // Delete only the rows that were folded into another row.
        val keepIds = keep.map { it.first }.toSet()
        existing.filter { it.id !in keepIds }.forEach { shoppingDao.deleteById(it.id) }
        keep.forEach { (id, text) -> shoppingDao.updateText(id, text) }
        if (add.isNotEmpty()) {
            shoppingDao.insertAll(add.map { ShoppingItemEntity(foodId = foodId, item = it) })
        }
    }
}
