package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.CategoryEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.util.AllergenDetector
import com.erfanbagheri.tahdig.util.DietFilter
import com.erfanbagheri.tahdig.util.Flavor
import com.erfanbagheri.tahdig.util.PersianText
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

    /** Dietary filter applied after the DB query (in-memory, tag-based). */
    private val _diet = MutableStateFlow<DietFilter?>(null)
    val diet: StateFlow<DietFilter?> = _diet.asStateFlow()
    /** Comma/،-separated ingredients the user has — dish must contain all of them. */
    private val _ingredients = MutableStateFlow("")
    val ingredients: StateFlow<String> = _ingredients.asStateFlow()
    /** Comma/،-separated terms the user cannot eat — dish must contain none. */
    private val _excluded = MutableStateFlow("")
    val excluded: StateFlow<String> = _excluded.asStateFlow()
    /** Selected taste axes (#89) — AND semantics: dish must carry every selected axis. */
    private val _flavors = MutableStateFlow<Set<Flavor>>(emptySet())
    val flavors: StateFlow<Set<Flavor>> = _flavors.asStateFlow()

    /**
     * Dishes carrying at least one taste tag — the chip row hides itself at zero
     * instead of showing an empty filter (acceptance: «Zero flavor tags → hidden»).
     */
    val flavorCount: StateFlow<Int> = foodDao.observeFlavorCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
        .combine(_diet) { foods, diet ->
            if (diet == null) foods else foods.filter { diet.matches(it.tags) }
        }
        .combine(_flavors) { foods, selected ->
            if (selected.isEmpty()) foods
            else foods.filter { food ->
                val have = food.flavors.split(',').toSet()
                selected.all { it.name in have }
            }
        }
        // Allergen hide-filter (#112): last in the chain so it composes with
        // every other filter; no-op while the toggle or profile is empty.
        .combine(
            combine(SettingsStore.allergens, SettingsStore.allergenHide) { p, h -> p to h },
        ) { foods, (profile, hide) ->
            if (!hide || profile.isEmpty()) foods
            else foods.filter { !AllergenDetector.shouldHide(hide, profile, AllergenDetector.detect(it.ingredients)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(text: String) { _query.value = text }

    /** Called on IME search action — persists the query to history. */
    fun onSubmit() {
        val q = _query.value.trim()
        if (q.isNotEmpty()) SearchHistory(getApplication()).record(q)
    }
    fun onIngredientsChange(text: String) { _ingredients.value = text }
    fun onExcludedChange(text: String) { _excluded.value = text }

    fun onCategorySelect(categoryId: Long?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
    }

    fun onDietSelect(diet: DietFilter?) {
        _diet.value = if (_diet.value == diet) null else diet
    }

    /** Multi-select toggle across the taste axes (#89) — AND, not OR. */
    fun onFlavorToggle(flavor: Flavor) {
        _flavors.value = _flavors.value.toMutableSet().apply {
            if (!remove(flavor)) add(flavor)
        }
    }

    /**
     * Keep dishes matching all required ingredients and none of the excluded terms.
     * Both sides go through [PersianText.normalize] so ZWNJ / Arabic-Kaf / Yeh variants match.
     */
    private fun filterByIngredients(foods: List<FoodEntity>, include: String, exclude: String): List<FoodEntity> {
        fun terms(raw: String) = raw.split(',', '،').map { PersianText.normalize(it) }.filter { it.isNotEmpty() }
        val must = terms(include)
        val mustNot = terms(exclude)
        if (must.isEmpty() && mustNot.isEmpty()) return foods
        return foods.filter { food ->
            val hay = PersianText.normalize(food.ingredients)
            must.all { hay.contains(it) } && mustNot.none { hay.contains(it) }
        }
    }
}
