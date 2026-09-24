package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.util.DailyBudget
import com.erfanbagheri.tahdig.util.NutritionLog
import com.erfanbagheri.tahdig.util.WeightTrend
import com.erfanbagheri.tahdig.data.local.entity.NutritionLogEntity
import com.erfanbagheri.tahdig.util.MealDiary
import com.erfanbagheri.tahdig.util.NutritionLogEntityView
import com.erfanbagheri.tahdig.util.WeeklyReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Meal diary + weekly report (#114).
 *
 * The slot is stored ON THE ROW at log time and never re-derived from the
 * clock when the diary is opened — a 23:00 dinner still reads as dinner at
 * the next breakfast, and moving a row writes the new slot back.
 */
class DiaryViewModel(app: Application) : AndroidViewModel(app) {

    private val db = TahdigDatabase.getInstance(app)
    private val dao = db.nutritionLogDao()

    private fun today(): LocalDate = LocalDate.now()
    private fun key(d: LocalDate): String = d.toString()

    /** Today's rows, grouped into meal slots for the diary. */
    val todayBySlot: StateFlow<Map<String, List<NutritionLogEntity>>> = dao
        .observeDay(key(today()))
        .map { rows -> MealDiary.groupBySlot(rows) { it.mealSlot } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** This week's rows, inclusive of today and the six days before it. */
    private val weekRows: StateFlow<List<NutritionLogEntity>> = dao
        .observeRange(key(today().minusDays(6)), key(today()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Per-day totals over the seven-day window, unlogged days included as 0. */
    val weekDays: StateFlow<List<WeeklyReport.DayTotals>> = weekRows
        .map { rows ->
            val totals = WeeklyReport.totalsByDay(
                rows.map { LocalDate.parse(it.day) to NutritionLogEntityView(it.calories, it.protein, it.fat, it.carbs) },
            ).associateBy { it.date }
            (6 downTo 0).map { offset ->
                val d = today().minusDays(offset.toLong())
                totals[d] ?: WeeklyReport.DayTotals(d, 0, 0, 0, 0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<WeeklyReport.Summary?> = weekDays
        .map { WeeklyReport.summarize(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Three most-logged dishes this week, ties broken alphabetically. */
    val topDishes: StateFlow<List<Pair<String, Int>>> = weekRows
        .map { rows -> WeeklyReport.topDishes(rows.groupingBy { it.foodName }.eachCount().toList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Consecutive logged days; today-not-yet-logged does not break it. */
    val loggingStreak: StateFlow<Int> = weekDays
        .map { WeeklyReport.loggingStreak(it, today()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- editing -----------------------------------------------------------

    /**
     * Weight change over the same seven-day window, or null when the week
     * holds fewer than two entries — a delta off one weighing is noise, and
     * rendering it as 0.0 kg would read like "you held steady".
     */
    val weightDeltaKg: StateFlow<Double?> = db.weightDao().observeAll()
        .map { rows ->
            val entries = rows
                .map { WeightTrend.Entry(LocalDate.ofEpochDay(it.epochDay), it.kg) }
                .filter { !it.date.isBefore(today().minusDays(6)) }
            WeightTrend.deltaKg(entries)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Profile + carry-over toggle, read straight from SettingsStore's
     * companion flows. The progress strip needs the goal to compute the
     * target, and 0 = «بدون هدف» must not render as a failed 0% bar.
     */
    val profile: StateFlow<DailyBudget.Profile> = SettingsStore.profile
    val carryOver: StateFlow<Boolean> = SettingsStore.carryOver

    /** Last removed row, so the diary can offer a one-shot undo. */
    private val _undo = MutableStateFlow<NutritionLogEntity?>(null)
    val undo: StateFlow<NutritionLogEntity?> = _undo.asStateFlow()

    fun moveToSlot(id: Long, slot: String) {
        viewModelScope.launch { withContext(Dispatchers.IO) { dao.updateSlot(id, slot) } }
    }

    /**
     * Shift a row [deltaMinutes] (#114). The row's OWN instant moves, so
     * 00:05 − 15 becomes yesterday 23:50 — wrapping for free in epoch math,
     * where an atTime(folded) rebuild would jump forward to today 23:50.
     * Slot and day key both re-derive from the new instant in one statement.
     */
    fun setTime(id: Long, deltaMinutes: Int) {
        if (deltaMinutes == 0) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val row = dao.byId(id) ?: return@withContext
                val at = row.loggedAt + deltaMinutes * 60_000L
                val slot = MealDiary.slotForMinutes(java.time.Instant.ofEpochMilli(at)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalTime()
                    .let { it.hour * 60 + it.minute })
                dao.updateTime(id, at, slot, NutritionLog.dayKey(at))
            }
        }
    }

    /**
     * Re-scale a row to [servings]. The macro factor is relative to what the
     * row already holds, so 1 -> 2 doubles and 2 -> 1 halves.
     */
    fun setServings(id: Long, servings: Double) {
        if (servings <= 0 || servings > 20) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val row = dao.byId(id) ?: return@withContext
                val factor = servings / row.servings
                dao.rescale(id, servings, factor)
            }
        }
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

    /** Re-insert the last removed row verbatim, macros included. */
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
