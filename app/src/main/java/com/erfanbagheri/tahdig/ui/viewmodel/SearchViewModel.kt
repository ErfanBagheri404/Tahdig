package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.CategoryEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.ui.screen.NutritionLabelData
import com.erfanbagheri.tahdig.util.AllergenDetector
import com.erfanbagheri.tahdig.util.DifficultyFilter
import com.erfanbagheri.tahdig.util.HalalFlags
import com.erfanbagheri.tahdig.util.NutriLabel
import com.erfanbagheri.tahdig.util.MicroNutrients
import com.erfanbagheri.tahdig.util.NutrientCaps
import com.erfanbagheri.tahdig.util.DietFilter
import com.erfanbagheri.tahdig.util.FirstRun
import com.erfanbagheri.tahdig.util.Flavor
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.SearchFilters
import com.erfanbagheri.tahdig.util.SortOrder
import com.erfanbagheri.tahdig.util.TimeBucket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
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
    /** Nutri-Score A-B only (#111) — needs real per-100g data, so most dishes drop out. */
    private val _nutriAb = MutableStateFlow(false)
    val nutriAb: StateFlow<Boolean> = _nutriAb.asStateFlow()

    /** «در محدوده من» (#113) — keeps dishes inside the user's nutrient caps. */
    private val _withinCaps = MutableStateFlow(false)
    val withinCaps: StateFlow<Boolean> = _withinCaps.asStateFlow()
    fun onWithinCapsToggle(on: Boolean) { _withinCaps.value = on }

    /** Threshold-badge filter (#117); off by default. */
    private val _badge = MutableStateFlow<MicroNutrients.Badge?>(null)
    val badge: StateFlow<MicroNutrients.Badge?> = _badge.asStateFlow()
    fun onBadgeSelect(b: MicroNutrients.Badge?) { _badge.value = b }
    private val _flavors = MutableStateFlow<Set<Flavor>>(emptySet())
    val flavors: StateFlow<Set<Flavor>> = _flavors.asStateFlow()

    // -- #80 user tags -------------------------------------------------------
    private val tagDao = db.tagDao()

    /** All tags, for the collapsible chip row. */
    val allTags: StateFlow<List<com.erfanbagheri.tahdig.data.local.entity.TagEntity>> =
        tagDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Selected tag ids — AND semantics: dish must carry every one. */
    private val _tagIds = MutableStateFlow<Set<Long>>(emptySet())
    val tagIds: StateFlow<Set<Long>> = _tagIds.asStateFlow()

    /** Row is collapsible; collapsed = hidden but selection kept. */
    private val _tagsExpanded = MutableStateFlow(false)
    val tagsExpanded: StateFlow<Boolean> = _tagsExpanded.asStateFlow()
    fun onTagsToggleRow() { _tagsExpanded.value = !_tagsExpanded.value }
    fun onTagToggle(tagId: Long) {
        _tagIds.value = _tagIds.value.toMutableSet().apply {
            if (!remove(tagId)) add(tagId)
        }
    }

    /**
     * Dishes carrying at least one taste tag — the chip row hides itself at zero
     * instead of showing an empty filter (acceptance: «Zero flavor tags → hidden»).
     */
    val flavorCount: StateFlow<Int> = foodDao.observeFlavorCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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

    /** Distinct cuisine codes present in the DB, for the cuisine chip row. */
    val cuisines: StateFlow<List<String>> = foodDao.observeAll()
        .map { foods -> foods.map { it.cuisine }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * One snapshot of every reactive input feeding [results] (#86).
     *
     * Carried as a data class rather than a tuple because the #136 filter axes
     * (time / difficulty / cuisine / sort) join the #134 search inputs, and an
     * 8-element destructure is where argument-order bugs hide.
     */
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
            // #134: the user's own notes are searchable too, merged as extra
            // rows by food id. The #136 axes (time / difficulty / cuisine /
            // sort) then apply in ONE pure pass over the merged list — they are
            // list-level filters, not query inputs, so they must run after
            // withNotes has added its rows or a noted dish would dodge them.
            withNotes(s.q, foodDao.search(s.q, s.cat))
                .map { foods ->
                    SearchFilters.apply(
                        foods = filterByIngredients(foods, s.ing, s.ex),
                        time = s.time,
                        difficulty = s.diff,
                        cuisine = s.cuisine,
                        sort = s.sort,
                        // Stars only when the chosen order needs them: the read
                        // is a full-table scan otherwise.
                        ratings = if (s.sort == SortOrder.RATING_DESC) {
                            ratingDao.allRatings().associate { it.foodId to it.stars }
                        } else {
                            emptyMap()
                        },
                    )
                }
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
        // #80 user tags: AND-filter in SQL (HAVING COUNT = n); combining on the
        // tag set re-runs it when a chip toggles or a tag is edited away.
        .combine(_tagIds) { foods, ids ->
            if (ids.isEmpty()) foods
            else {
                val keep = tagDao.foodsWithAllTagsOnce(ids.toList(), ids.size)
                    .map { it.id }.toSet()
                foods.filter { it.id in keep }
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
        // Nutri-Score A-B filter (#111): composes last with everything above,
        // and is a no-op while the chip is off.
        .combine(_nutriAb) { foods, onlyAb ->
            if (!onlyAb) foods
            else foods.filter { food ->
                val label = NutritionLabelData.of(food.name, food.tags, food.ingredients)
                label.score?.let { NutriLabel.passesFilter(it, keepAB = true) } ?: false
            }
        }
        // «در محدوده من» (#113): drops estimate dishes with a hint-worthy
        // reason — their amounts are unknown, and unknown is not within.
        // No caps active is a no-op so the chip does nothing while unset.
        .combine(_withinCaps) { foods, onlyWithin ->
            if (!onlyWithin) return@combine foods
            val caps = NutrientCaps.merge(
                NutrientCaps.Preset.entries.firstOrNull {
                    it.name == SettingsStore.capPreset.value
                },
                SettingsStore.capCustom.value.mapNotNull { (k, v) ->
                    NutrientCaps.Nutrient.entries.firstOrNull { it.name == k }
                        ?.let { it to v }
                }.toMap(),
            )
            if (caps.isEmpty()) foods
            else foods.filter { food ->
                val label = NutritionLabelData.of(food.name, food.tags, food.ingredients)
                if (label.estimated) false
                else NutrientCaps.allWithin(caps, NutritionLabelData.amounts(label))
            }
        }
        // Threshold badges (#117): last, and a no-op while no badge is picked.
        // Needs real micro data, so estimates never carry a badge.
        .combine(_badge) { foods, b ->
            if (b == null) foods
            else foods.filter { food ->
                val label = NutritionLabelData.of(food.name, food.tags, food.ingredients)
                if (label.estimated) false
                else b in MicroNutrients.badges(
                    fiberG = label.fiberG,
                    sodiumMg = label.saltG.times(1000.0),
                    ironMg = label.ironMg,
                )
            }
        }
        // Halal strict mode (#118): last, and a no-op while the toggle is off.
        // Conservative by construction — only positively flagged dishes drop.
        .combine(SettingsStore.halalStrict) { foods, strict ->
            if (!strict) foods
            else foods.filter { food ->
                !HalalFlags.shouldHide(
                    strict,
                    HalalFlags.flags(food.ingredients),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Zero-result recovery (#126).
     *
     * When the active query returns nothing, drop the last token and search
     * again — «کباب زعفرانی مخصوص» still reaches «کباب زعفرانی». Only the
     * query is relaxed; the other filters stay untouched so the suggestion
     * never contradicts a chip the user deliberately turned on.
     */
    private val _relaxedMatches = MutableStateFlow<List<FoodEntity>>(emptyList())
    val relaxedMatches: StateFlow<List<FoodEntity>> = _relaxedMatches

    init {
        // #126: try each truncation in order and keep the first that returns
        // rows. A single drop is not enough — «paste pasta bake» would relax to
        // «paste pasta», still zero rows, and the "شاید این‌ها" list would ship
        // empty while claiming to have suggestions.
        viewModelScope.launch {
            _query
                .debounce(300)
                .map { FirstRun.relaxedCandidates(it) }
                .distinctUntilChanged()
                .collectLatest { candidates ->
                    var found: List<FoodEntity> = emptyList()
                    for (candidate in candidates) {
                        val hits = foodDao.searchByName(candidate).first()
                        if (hits.isNotEmpty()) {
                            found = hits
                            break
                        }
                    }
                    _relaxedMatches.value = found.take(3)
                }
        }
    }

    /** Clear every active filter at once — the other half of no-match recovery. */
    fun clearFilters() {
        _selectedCategoryId.value = null
        _diet.value = null
        _ingredients.value = ""
        _excluded.value = ""
        _flavors.value = emptySet()
        _nutriAb.value = false
        _withinCaps.value = false
        _badge.value = null
        _timeBucket.value = TimeBucket.ANY
        _difficultyFilter.value = DifficultyFilter.ANY
        _cuisine.value = null
        filterPrefs.edit().remove("time").remove("difficulty").remove("cuisine").apply()
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

    /** Nutri-Score A-B chip (#111); off by default. */
    fun onNutriAbToggle(on: Boolean) { _nutriAb.value = on }

    fun onQueryChange(text: String) { _query.value = text }

    /**
     * Dish-name results plus dishes whose private note matches (#134).
     *
     * Extracted from the pipeline so the combine's types are declared rather
     * than inferred through three nested operators. Queries under two
     * characters skip the note lookup: every note would match, which is not a
     * search.
     */
    private fun withNotes(q: String, base: Flow<List<FoodEntity>>): Flow<List<FoodEntity>> {
        if (q.length < 2) return base
        val noted: Flow<List<FoodEntity>> = ratingDao.searchNotesFlow(q)
            .flatMapLatest { rated ->
                if (rated.isEmpty()) flowOf(emptyList())
                else foodDao.byIdsFlow(rated.map { it.foodId })
            }
        return combine(base, noted) { foods, fromNotes ->
            if (fromNotes.isEmpty()) return@combine foods
            val already = foods.map { it.id }.toSet()
            foods + fromNotes.filter { it.id !in already }
        }
    }


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
