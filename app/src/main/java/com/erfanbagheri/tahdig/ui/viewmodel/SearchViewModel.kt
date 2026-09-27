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
import com.erfanbagheri.tahdig.util.HalalFlags
import com.erfanbagheri.tahdig.util.NutriLabel
import com.erfanbagheri.tahdig.util.MicroNutrients
import com.erfanbagheri.tahdig.util.NutrientCaps
import com.erfanbagheri.tahdig.util.DietFilter
import com.erfanbagheri.tahdig.util.FirstRun
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
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
    }

    /** Nutri-Score A-B chip (#111); off by default. */
    fun onNutriAbToggle(on: Boolean) { _nutriAb.value = on }

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
