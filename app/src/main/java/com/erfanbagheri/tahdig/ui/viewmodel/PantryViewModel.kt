package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.PantryItemEntity
import com.erfanbagheri.tahdig.util.PantryMatcher
import com.erfanbagheri.tahdig.util.ExpiryMath
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.StapleCounter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A dish, how much of it the pantry covers, and the expiry lift (#106).
 * [score] is what the list sorts by — coverage × strongest urgency boost.
 */
data class CookableDish(
    val food: FoodEntity,
    val coverage: Float,
    val expiryBoost: Double = 1.0,
) {
    val score: Float get() = coverage * expiryBoost.toFloat()
}

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
            val now = System.currentTimeMillis()
            foods.map { food ->
                val dish = CookableDish(food, PantryMatcher.coverage(food.ingredients, have))
                // Use-it-up (#106): lift the score by the strongest expiry among
                // the pantry items this dish actually uses.
                val boost = pantry.maxOfOrNull { p ->
                    if (usesKey(food, p.item)) ExpiryMath.urgencyBoost(ExpiryMath.daysTo(p.expiresAt, now))
                    else 1.0
                } ?: 1.0
                dish.copy(expiryBoost = boost)
            }
            .filter { it.coverage > 0f }
            .sortedWith(compareByDescending<CookableDish> { it.score }
                .thenBy { it.food.prepTimeMin })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** True when the dish mentions [item] — matcher's own containment rule. */
    private fun usesKey(food: FoodEntity, item: String): Boolean {
        val k = PersianText.normalize(item).filterNot { it.isWhitespace() }
        if (k.isEmpty()) return false
        return food.ingredients.split(',', '،').any { raw ->
            val n = PersianText.normalize(raw).filterNot { it.isWhitespace() }
            n.contains(k) || k.contains(n)
        }
    }

    /** Expiring staples, soonest first — the «رو به اتمام» section (#106). */
    val expiring: StateFlow<List<PantryItemEntity>> = pantryDao.observeAll()
        .map { list ->
            val now = System.currentTimeMillis()
            list.filter { it.expiresAt != null }
                .sortedBy { it.expiresAt }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** «tap -> edit date» — null clears the date back to undated. */
    fun setExpiry(id: Long, expiresAt: Long?) = viewModelScope.launch {
        pantryDao.setExpiry(id, expiresAt)
    }

    fun onDraftChange(text: String) { _draft.value = text }

    /** Adds the draft, splitting on comma / Farsi comma so a pasted list works. */
    fun addDraft() {
        val parts = _draft.value.split(',', '،')
            .map { PersianText.normalize(it).trim() }
            .filter { it.isNotEmpty() }
        if (parts.isEmpty()) return
        viewModelScope.launch {
            parts.forEach { part ->
                val now = System.currentTimeMillis()
                // Default by shelf life for known staples (#106); unknown → undated.
                pantryDao.insert(
                    PantryItemEntity(
                        item = part,
                        addedAt = now,
                        expiresAt = ExpiryMath.defaultExpiry(part, now),
                    ),
                )
            }
            _draft.value = ""
        }
    }

    fun remove(id: Long) = viewModelScope.launch { pantryDao.deleteById(id) }

    // ── tap-counters (#109) ────────────────────────────────────────────

    /**
     * One −/+ tap. Bounds live in [StapleCounter] (pure, unit-tested); 0 is a
     * legal shown value — the row stays visible and removable, just stockless.
     */
    fun stepQuantity(id: Long, current: Int, delta: Int) = viewModelScope.launch {
        pantryDao.setQuantity(id, StapleCounter.step(current, delta))
    }

    /** «همه پر شد»: every staple back to exactly one — "stocked", not MAX. */
    fun fillAll() = viewModelScope.launch { pantryDao.fillAll() }

    /** Armed undo snapshot after a bulk clear; null/empty = no undo to show. */
    private val _clearUndo = MutableStateFlow<List<PantryItemEntity>>(emptyList())
    val clearUndo: StateFlow<List<PantryItemEntity>> = _clearUndo.asStateFlow()

    /** Bulk clear: snapshot first so undo restores ids + counts byte-for-byte. */
    fun clearAll() = viewModelScope.launch {
        _clearUndo.value = StapleCounter.snapshotForClear(items.value)
        pantryDao.clearAll()
    }

    /** «بازگردانی»: exact re-insert of the snapshot, then disarm. */
    fun restoreClear() = viewModelScope.launch {
        val snap = _clearUndo.value
        if (snap.isNotEmpty()) pantryDao.insertAll(snap)
        _clearUndo.value = emptyList()
    }
}
