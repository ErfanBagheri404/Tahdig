package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.MilestoneCheckEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Checked-state store for the mise-en-place checklist (#99).
 *
 * The detail screen and cook mode both observe [checkedHashes] for the dish they
 * show; [forFood] selects which dish's state is live.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MilestoneViewModel(app: Application) : AndroidViewModel(app) {

    private val db = TahdigDatabase.getInstance(app)
    private val dao = db.milestoneCheckDao()

    private val foodId = MutableStateFlow(-1L)

    val checkedHashes: StateFlow<Set<String>> = foodId
        .flatMapLatest { id ->
            if (id < 0) flowOf(emptySet())
            else dao.observeHashes(id).map { it.toSet() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** Point the shared state at [id] — call from LaunchedEffect on the screen. */
    fun forFood(id: Long) {
        if (foodId.value != id) foodId.value = id
    }

    fun toggle(foodId: Long, hash: String) = viewModelScope.launch {
        if (hash in dao.hashesFor(foodId)) dao.uncheck(foodId, hash)
        else dao.insert(MilestoneCheckEntity(foodId = foodId, ingredientHash = hash))
    }

    fun clear(foodId: Long) = viewModelScope.launch { dao.clearForFood(foodId) }
}
