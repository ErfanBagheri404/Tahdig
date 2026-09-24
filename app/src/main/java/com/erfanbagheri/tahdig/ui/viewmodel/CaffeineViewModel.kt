package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.CaffeineLogEntity
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.util.Caffeine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Caffeine log + pregnancy mode (#119).
 *
 * The accumulation and the cap decision both live in [Caffeine] as pure
 * functions; this class only moves rows in and out of Room. The cap shown
 * is [Caffeine.effectiveCap] — pregnancy mode overrides the manual slider,
 * so the 200 mg line stays honest when a 600 mg cap is stored.
 */
class CaffeineViewModel(app: Application) : AndroidViewModel(app) {

    private val db = TahdigDatabase.getInstance(app)
    private val dao = db.caffeineDao()

    private fun today(): LocalDate = LocalDate.now()

    /** Today's rows, newest first. */
    val todayRows: StateFlow<List<CaffeineLogEntity>> = dao
        .observeDay(today().toEpochDay())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Milligrams accumulated today. The DAO already scopes the query to
     * today; the sum still goes through [Caffeine.todayTotal] so there is
     * one definition of "today's total" rather than a second one here.
     */
    val todayMg: StateFlow<Int> = todayRows
        .map { rows ->
            Caffeine.todayTotal(
                rows.map { Caffeine.Entry(LocalDate.ofEpochDay(it.epochDay), it.mg) },
                today(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pregnancyMode: StateFlow<Boolean> = SettingsStore.pregnancyMode
    val customCap: StateFlow<Int> = SettingsStore.caffeineCap

    /**
     * The cap in force, combining the manual slider with the mode. A
     * `combine` rather than two StateFlows so the value is a single fact
     * the card can read — recomputed on either input changing.
     */
    val effectiveCap: StateFlow<Int> = combine(customCap, pregnancyMode) { cap, mode ->
        Caffeine.effectiveCap(cap.takeIf { it > 0 }, mode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Caffeine.DEFAULT_CAP_MG)

    /** Seven-day accumulation, oldest first, for the history row. */
    val weekMg: StateFlow<List<Int>> = dao
        .observeFrom(today().minusDays(6).toEpochDay())
        .map { rows ->
            (6 downTo 0).map { offset ->
                val day = today().minusDays(offset.toLong()).toEpochDay()
                rows.filter { it.epochDay == day }.sumOf { it.mg }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Last removed row for the one-shot undo. */
    private val _undo = MutableStateFlow<CaffeineLogEntity?>(null)
    val undo: StateFlow<CaffeineLogEntity?> = _undo.asStateFlow()

    /** Log a preset chip: label + its standard mg. */
    fun logPreset(label: String, mg: Int) {
        log(CaffeineLogEntity(epochDay = today().toEpochDay(), mg = mg, label = label))
    }

    /** Log a custom amount, clamped to a sane single-hit range. */
    fun logCustom(mg: Int, label: String = "سفارشی") {
        if (mg <= 0) return
        log(
            CaffeineLogEntity(
                epochDay = today().toEpochDay(),
                mg = mg.coerceAtMost(2000),
                label = label,
            ),
        )
    }

    private fun log(row: CaffeineLogEntity) {
        viewModelScope.launch { withContext(Dispatchers.IO) { dao.insert(row) } }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val row = dao.byId(id)
                dao.delete(id)
                _undo.value = row
            }
        }
    }

    fun undoRemove() {
        val row = _undo.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.insert(row.copy(id = 0))
                _undo.value = null
            }
        }
    }

    fun clearUndo() {
        _undo.value = null
    }
}
