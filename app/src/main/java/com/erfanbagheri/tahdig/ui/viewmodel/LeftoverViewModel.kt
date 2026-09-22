package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.util.LeftoverRanker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * «غذای مونده دارم» (#107) — a single-shot session over already-cooked components.
 *
 * Nothing is persisted: the pantry («چی دارم؟») is the staple list that survives
 * restarts, this is the one-off "use up what's in the fridge tonight" flow. Closing
 * the screen forgets the session by design (AC: "single-shot session, not persisted").
 */
class LeftoverViewModel(app: Application) : AndroidViewModel(app) {

    private companion object {
        const val DAY_MS = 86_400_000L
    }

    private val db = TahdigDatabase.getInstance(app)

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft

    private val _items = MutableStateFlow<List<LeftoverRanker.Leftover>>(emptyList())
    val items: StateFlow<List<LeftoverRanker.Leftover>> = _items

    // #107 (c): a single-shot session has no stored cook time, so the one real
    // urgency signal the user can give is "I cooked this yesterday" — it back-dates
    // every chip by a day, which feeds the ranker's consume-by (+2 days) curve.
    private val _yesterday = MutableStateFlow(false)
    val yesterday: StateFlow<Boolean> = _yesterday

    fun setYesterday(value: Boolean) {
        _yesterday.value = value
        // Re-stamp: the chips share one base, and flipping the toggle means the
        // user's answer changed for the whole session.
        _items.value = _items.value.map { it.copy(cookedAtMs = base()) }
    }

    private fun base(): Long =
        System.currentTimeMillis() - if (_yesterday.value) DAY_MS else 0L

    /** Ranked against the live food table — a new dish appears without a reopen. */
    val results: StateFlow<List<LeftoverRanker.Scored>> =
        combine(_items, db.foodDao().observeAll()) { items, foods ->
            LeftoverRanker.rank(items, foods, System.currentTimeMillis())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onDraftChange(value: String) = run { _draft.value = value }

    /** Commit the draft; commas add several components at once (quick multi-entry). */
    fun add() {
        val parts = _draft.value.split(',', '،')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.isEmpty()) return
        val now = base()
        _items.value = _items.value + parts
            .filterNot { p -> _items.value.any { it.text == p } }
            .map { LeftoverRanker.Leftover(it, cookedAtMs = now) }
        _draft.value = ""
    }

    /** Voice result lands in the draft, same as the pantry field. */
    fun addSpoken(spoken: String) {
        _draft.value = com.erfanbagheri.tahdig.util.VoiceInput.append(_draft.value, spoken)
    }

    fun remove(text: String) {
        _items.value = _items.value.filterNot { it.text == text }
    }

    fun clearAll() {
        _items.value = emptyList()
    }
}
