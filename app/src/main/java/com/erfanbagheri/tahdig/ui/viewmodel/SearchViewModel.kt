package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.CategoryEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val db = TahdigDatabase.getInstance(app)
    private val foodDao = db.foodDao()
    private val categoryDao = db.categoryDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    /** Comma/،-separated ingredients the user has — dish must contain all of them. */
    private val _ingredients = MutableStateFlow("")
    val ingredients: StateFlow<String> = _ingredients.asStateFlow()

    /** Comma/،-separated terms the user cannot eat — dish must contain none. */
    private val _excluded = MutableStateFlow("")
    val excluded: StateFlow<String> = _excluded.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<FoodEntity>> = combine(_query, _selectedCategoryId, _ingredients, _excluded) { q, cat, ing, ex ->
        Triple(q, cat, Pair(ing, ex))
    }
        .debounce(300)
        .flatMapLatest { (q, cat, ingEx) ->
            foodDao.search(q, cat).map { foods -> filterByIngredients(foods, ingEx.first, ingEx.second) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(text: String) { _query.value = text }

    fun onIngredientsChange(text: String) { _ingredients.value = text }

    fun onExcludedChange(text: String) { _excluded.value = text }

    fun onCategorySelect(categoryId: Long?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
    }

    /** Keep dishes matching all required ingredients and none of the excluded terms. */
    private fun filterByIngredients(foods: List<FoodEntity>, include: String, exclude: String): List<FoodEntity> {
        val must = include.split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }
        val mustNot = exclude.split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }
        if (must.isEmpty() && mustNot.isEmpty()) return foods
        return foods.filter { food ->
            val hay = food.ingredients.lowercase()
            must.all { hay.contains(it.lowercase()) } &&
                mustNot.none { hay.contains(it.lowercase()) }
        }
    }
}
