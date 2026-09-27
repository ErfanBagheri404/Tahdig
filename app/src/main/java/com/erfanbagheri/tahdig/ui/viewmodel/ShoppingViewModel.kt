package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.ShoppingItemEntity
import com.erfanbagheri.tahdig.util.IngredientParser
import com.erfanbagheri.tahdig.data.local.entity.ShoppingTripEntity
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.util.AislePlanner
import com.erfanbagheri.tahdig.util.IngredientRegistry
import com.erfanbagheri.tahdig.util.MissingDiff
import com.erfanbagheri.tahdig.util.UndoHub
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(app: Application) : AndroidViewModel(app) {
    private val db = TahdigDatabase.getInstance(app)
    private val shoppingDao = db.shoppingDao()
    private val tripDao = db.shoppingTripDao()
    private val jx = kotlinx.serialization.json.Json
    private val mealPlanDao = db.mealPlanDao()
    private val foodDao = db.foodDao()

    val items: StateFlow<List<ShoppingItemEntity>> = shoppingDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Pantry display names, feeding the «داری» badge and sink-to-bottom ordering. */
    val pantryItems: StateFlow<List<String>> = db.pantryDao().observeAll()
        .map { list -> list.map { it.item } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * True when the pantry already covers a list row. Reuses the missing-diff so the
     * badge and the detail screen's gap line can never disagree — an approved substitute
     * counts as "have" in both.
     */
    fun inPantry(row: String, pantry: List<String>): Boolean =
        pantry.isNotEmpty() && MissingDiff.diff(row, pantry).allCovered

    fun setChecked(id: Long, checked: Boolean) {
        viewModelScope.launch { shoppingDao.setChecked(id, checked) }
    }

    /**
     * Delete a row, keeping it restorable for the undo window (#127).
     * The whole row is captured first — the inverse re-inserts it with its
     * original id and created_at, so undo restores the list exactly.
     */
    fun remove(id: Long) {
        viewModelScope.launch {
            val row = shoppingDao.byId(id) ?: return@launch
            shoppingDao.deleteById(id)
            UndoHub.arm("«${row.item}» از لیست خرید حذف شد") {
                viewModelScope.launch { restore(row) }
            }
        }
    }

    private suspend fun restore(row: ShoppingItemEntity) {
        shoppingDao.restore(row)
    }

    /** Drop checked rows only; the exact rows return on undo (#127). */
    fun clearChecked() {
        viewModelScope.launch {
            val done = shoppingDao.allRows().filter { it.isChecked }
            if (done.isEmpty()) return@launch
            shoppingDao.clearChecked()
            UndoHub.arm("${done.size} قلم خریده‌شده حذف شد") {
                viewModelScope.launch { done.forEach { shoppingDao.restore(it) } }
            }
        }
    }

    /** Clear the whole list; the exact rows return on undo (#127). */
    fun clearAll() {
        viewModelScope.launch {
            val rows = shoppingDao.allRows()
            if (rows.isEmpty()) return@launch
            shoppingDao.clearAll()
            UndoHub.arm("لیست خرید پاک شد") {
                viewModelScope.launch { rows.forEach { shoppingDao.restore(it) } }
            }
        }
    }

    // ── Aisle manager (#108) ─────────────────────────────────────────
    // SettingsStore owns the persisted config; the grouping pass in the screen
    // reads it through the pure AislePlanner below.

    /** Reorder aisles: the first persisted list wins; new aisles append later. */
    fun moveAisle(before: String, after: String) {
        val known = SettingsStore.aisleOrder.value.toMutableList()
        if (before !in known) known += before
        if (after !in known) known += after
        val next = known.filterNot { it == before }.toMutableList()
        next.add(next.indexOf(after) + 1, before)
        SettingsStore.setAisleConfig(next, SettingsStore.aisleRenames.value, SettingsStore.aisleHidden.value)
    }

    fun renameAisle(canonical: String, display: String) {
        val next = SettingsStore.aisleRenames.value.toMutableMap()
        if (display.isBlank() || display == canonical) next.remove(canonical) else next[canonical] = display
        SettingsStore.setAisleConfig(SettingsStore.aisleOrder.value, next, SettingsStore.aisleHidden.value)
    }

    /** Hide: view-only fold into «سایر» via AislePlanner — data never moves. */
    fun setAisleHidden(canonical: String, hidden: Boolean) {
        val next = SettingsStore.aisleHidden.value.toMutableSet()
        if (hidden) next += canonical else next -= canonical
        SettingsStore.setAisleConfig(SettingsStore.aisleOrder.value, SettingsStore.aisleRenames.value, next)
    }

    // ── Trip mode (#108) ────────────────────────────────────────────
    // Trip mode is a LIGHT flag: the snapshot is taken at END time from the
    // live rows, because Room already holds the truth — checkoffs during the
    // trip just accumulate in place.

    /** Past trips, newest first — read-only history for the history view. */
    val trips = tripDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startTrip() = SettingsStore.setTripActive(true)

    /** Archive the current list (date + counts + rows) into history (#108). */
    fun endTrip() {
        viewModelScope.launch {
            val rows = shoppingDao.allRows()
            if (rows.isNotEmpty()) {
                val (total, bought) = AislePlanner.tripCounts(rows.map { it.item to it.isChecked })
                val snap = jx.encodeToString(
                    ListSerializer(TripRow.serializer()),
                    rows.map { TripRow(it.item, it.isChecked) },
                )
                tripDao.insert(ShoppingTripEntity(endedAt = System.currentTimeMillis(), total = total, bought = bought, itemsJson = snap))
            }
            SettingsStore.setTripActive(false)
        }
    }

    fun clearTripHistory() {
        viewModelScope.launch { tripDao.clearAll() }
    }

    /** One archived row — mirrors the JSON keys the file has always used. */
    @kotlinx.serialization.Serializable
    data class TripRow(val item: String, val checked: Boolean)

    /** Add every ingredient of [ingredients] (comma/،-separated) as a list item. */
    fun addIngredients(foodId: Long, ingredients: String) {
        viewModelScope.launch { mergeInto(split(ingredients), foodId) }
    }

    /** Items with no single source dish — a pantry row being replaced (#106). */
    fun addItems(ingredients: String) {
        viewModelScope.launch { mergeInto(split(ingredients), null) }
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
