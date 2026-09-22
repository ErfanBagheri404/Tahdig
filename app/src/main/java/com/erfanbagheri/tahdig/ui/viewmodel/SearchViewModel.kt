package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.CategoryEntity
import com.erfanbagheri.tahdig.data.local.entity.FilterPresetEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.util.DifficultyFilter
import com.erfanbagheri.tahdig.util.DietFilter
import com.erfanbagheri.tahdig.util.FilterPresetCodec
import com.erfanbagheri.tahdig.util.FilterPresetPayload
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.SearchFilters
import com.erfanbagheri.tahdig.util.SortOrder
import com.erfanbagheri.tahdig.util.TimeBucket
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
    private val ratingDao = db.ratingDao()

    /** Filter state persists across restarts (acceptance for #86). */
    private val filterPrefs = app.getSharedPreferences("tahdig_search_filters", 0)

    /** One snapshot of every reactive input feeding [results]. */
    private data class FilterState(
        val q: String,
        val cat: Long?,
        val ing: String,
        val ex: String,
        val time: TimeBucket,
        val diff: DifficultyFilter,
        val cuisine: String?,
        val sort: SortOrder,
    )

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

    // ── filter sheet state ─────────────────────────────────────────
    private val _timeBucket = MutableStateFlow(
        savedEnum("time", TimeBucket.ANY),
    )
    val timeBucket: StateFlow<TimeBucket> = _timeBucket.asStateFlow()

    private val _difficultyFilter = MutableStateFlow(
        savedEnum("difficulty", DifficultyFilter.ANY),
    )
    val difficultyFilter: StateFlow<DifficultyFilter> = _difficultyFilter.asStateFlow()

    /** Null = every cuisine. Exact [FoodEntity.cuisine] code, matched case-insensitively. */
    private val _cuisine = MutableStateFlow(filterPrefs.getString("cuisine", null))
    val cuisine: StateFlow<String?> = _cuisine.asStateFlow()

    private val _sortOrder = MutableStateFlow(
        savedEnum("sort", SortOrder.SUGGESTED),
    )
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private inline fun <reified T : Enum<T>> savedEnum(key: String, default: T): T {
        val raw = filterPrefs.getString(key, null) ?: return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }

    /** Active non-sort filter count — badge on the filter icon. */
    val activeFilterCount: StateFlow<Int> = combine(
        _timeBucket, _difficultyFilter, _cuisine,
    ) { t, d, c ->
        (if (t != TimeBucket.ANY) 1 else 0) +
            (if (d != DifficultyFilter.ANY) 1 else 0) +
            (if (c != null) 1 else 0)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Saved filter presets (#87) — rows plus their decoded payloads, in save order. */
    val presets: StateFlow<List<Pair<FilterPresetEntity, FilterPresetPayload>>> =
        db.filterPresetDao().observeAll()
            .map { rows -> rows.mapNotNull { r -> FilterPresetCodec.decode(r.payload)?.let { r to it } } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Distinct cuisine codes present in the DB, for the cuisine chip row. */
    val cuisines: StateFlow<List<String>> = foodDao.observeAll()
        .map { foods -> foods.map { it.cuisine }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<FoodEntity>> = combine(
        _query, _selectedCategoryId, _ingredients, _excluded,
        _timeBucket, _difficultyFilter, _cuisine, _sortOrder,
    ) { a ->
        @Suppress("UNCHECKED_CAST")
        FilterState(
            q = a[0] as String,
            cat = a[1] as Long?,
            ing = a[2] as String,
            ex = a[3] as String,
            time = a[4] as TimeBucket,
            diff = a[5] as DifficultyFilter,
            cuisine = a[6] as String?,
            sort = a[7] as SortOrder,
        )
    }
        .debounce(300)
        .flatMapLatest { s ->
            foodDao.search(s.q, s.cat).map { foods ->
                val filtered = filterByIngredients(foods, s.ing, s.ex)
                val ratings = if (s.sort == SortOrder.RATING_DESC) {
                    ratingDao.allRatings().associate { it.foodId to it.stars }
                } else {
                    emptyMap()
                }
                SearchFilters.apply(
                    foods = filtered,
                    time = s.time,
                    difficulty = s.diff,
                    cuisine = s.cuisine,
                    sort = s.sort,
                    ratings = ratings,
                )
            }
        }
        .combine(_diet) { foods, diet ->
            if (diet == null) foods else foods.filter { diet.matches(it.tags) }
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

    fun onTimeSelect(bucket: TimeBucket) {
        _timeBucket.value = if (_timeBucket.value == bucket) TimeBucket.ANY else bucket
        filterPrefs.edit().putString("time", _timeBucket.value.name).apply()
    }

    fun onDifficultySelect(filter: DifficultyFilter) {
        _difficultyFilter.value = if (_difficultyFilter.value == filter) DifficultyFilter.ANY else filter
        filterPrefs.edit().putString("difficulty", _difficultyFilter.value.name).apply()
    }

    fun onCuisineSelect(code: String?) {
        _cuisine.value = if (_cuisine.value == code) null else code
        filterPrefs.edit().putString("cuisine", _cuisine.value).apply()
    }

    fun onSortSelect(sort: SortOrder) {
        _sortOrder.value = sort
        filterPrefs.edit().putString("sort", sort.name).apply()
    }

    /** Clear every filter chip in one action (sort is kept — it is an ordering, not a filter). */
    fun clearFilters() {
        _timeBucket.value = TimeBucket.ANY
        _difficultyFilter.value = DifficultyFilter.ANY
        _cuisine.value = null
        filterPrefs.edit().remove("time").remove("difficulty").remove("cuisine").apply()
    }

    // ── filter presets (#87) ────────────────────────────────────────

    /** Every non-query axis, so a preset restores the complete sheet state. */
    private fun currentPayload() = FilterPresetPayload(
        categoryId = _selectedCategoryId.value,
        diet = _diet.value?.name,
        ingredients = _ingredients.value,
        excluded = _excluded.value,
        time = _timeBucket.value.name,
        difficulty = _difficultyFilter.value.name,
        cuisine = _cuisine.value,
        sort = _sortOrder.value.name,
    )

    /** Same-name save replaces (unique index) — the rename-free edit path. */
    fun savePreset(name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        viewModelScope.launch { db.filterPresetDao().save(n, FilterPresetCodec.encode(currentPayload())) }
    }

    /** Tap = apply the full snapshot (acceptance: original state restored). */
    fun applyPreset(payload: FilterPresetPayload) {
        val p = FilterPresetCodec.resolve(payload)
        _selectedCategoryId.value = p.categoryId
        _diet.value = p.diet
        _ingredients.value = p.ingredients
        _excluded.value = p.excluded
        _timeBucket.value = p.time
        _difficultyFilter.value = p.difficulty
        _cuisine.value = p.cuisine
        _sortOrder.value = p.sort
        // Mirror the sheet's own persistence so a restart keeps the applied preset.
        filterPrefs.edit()
            .putString("time", p.time.name)
            .putString("difficulty", p.difficulty.name)
            .putString("cuisine", p.cuisine)
            .putString("sort", p.sort.name)
            .apply()
    }

    fun renamePreset(id: Long, name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        viewModelScope.launch { db.filterPresetDao().rename(id, n) }
    }

    /** Deletes ONLY this row — dishes and sibling presets are untouched (acceptance). */
    fun deletePreset(id: Long) {
        viewModelScope.launch { db.filterPresetDao().delete(id) }
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
