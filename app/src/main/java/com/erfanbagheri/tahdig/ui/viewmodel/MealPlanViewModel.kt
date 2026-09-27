package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.CalendarExport
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.MealPlanEntity
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.util.PlanEditMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Weekly plan (#83) — the plan is editable, not just generated.
 *
 * The week is anchored to its شنبه so «هفتهٔ قبل» and «تکرار هفتهٔ قبل» have a
 * previous week to name at all. Every mutation is *computed* by [PlanEditMath]
 * — the math is the only place that decides what a move or a copy means — and
 * then written back as plain cell upserts.
 */
class MealPlanViewModel(app: Application) : AndroidViewModel(app) {
    private val db = TahdigDatabase.getInstance(app)
    private val mealPlanDao = db.mealPlanDao()
    private val foodDao = db.foodDao()

    init {
        if (!SettingsStore.isInitialized()) SettingsStore.init(app)
    }

    private val _currentDay = MutableStateFlow(0)
    val currentDay: StateFlow<Int> = _currentDay

    private val _weekStart = MutableStateFlow(currentWeekStart())
    val weekStart: StateFlow<Long> = _weekStart

    /** Plain-Farsi outcome of the last edit; null until the user acts. */
    private val _lastAction = MutableStateFlow<String?>(null)
    val lastAction: StateFlow<String?> = _lastAction

    fun setDay(day: Int) {
        _currentDay.value = day
    }

    /** Step to the neighbouring week, keeping the selected day. */
    fun stepWeek(weeks: Int) {
        _weekStart.value += 7L * weeks
    }

    val foods: StateFlow<List<FoodEntity>> = foodDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // One cached flow per (week, day) so flipping days reuses the subscription.
    private val slotCache = mutableMapOf<Pair<Long, Int>, StateFlow<List<MealPlanEntity>>>()

    fun observeSlots(weekStart: Long, dayIndex: Int): StateFlow<List<MealPlanEntity>> {
        val key = weekStart to dayIndex
        return slotCache.getOrPut(key) {
            mealPlanDao.observeSlotsForDay(key.first, key.second)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    fun assignFood(dayIndex: Int, slot: String, foodId: Long) {
        viewModelScope.launch {
            val week = _weekStart.value
            val rows = rowsOf(week) + PlanEditMath.Row(
                PlanEditMath.Slot(week, dayIndex, slot), foodId,
            )
            write(rows, week)
        }
    }

    fun clearFood(dayIndex: Int, slot: String) {
        viewModelScope.launch {
            val week = _weekStart.value
            write(rowsOf(week).filterNot { it.slot.dayIndex == dayIndex && it.slot.mealSlot == slot }, week)
        }
    }

    /**
     * Move a dish to another day, swapping with whatever sits in the target
     * cell. A drag is a move the user can take back, so the displaced dish is
     * never dropped — it returns to where the dragged one came from.
     */
    fun moveSlot(fromDay: Int, fromMeal: String, toDay: Int, toMeal: String) {
        viewModelScope.launch {
            val week = _weekStart.value
            val rows = rowsOf(week)
            val source = rows.firstOrNull { it.slot.dayIndex == fromDay && it.slot.mealSlot == fromMeal }
            if (source == null) {
                _lastAction.value = "این خانه خالی است."
                return@launch
            }
            val occupant = rows.firstOrNull { it.slot.dayIndex == toDay && it.slot.mealSlot == toMeal }
            val move = PlanEditMath.planMove(source, toDay, toMeal, occupant)
            val next = rows
                .filterNot { it.slot == source.slot || it.slot == move.target.slot }
                .plus(move.target)
            val restored = move.displaced?.let { d ->
                next + PlanEditMath.Row(PlanEditMath.Slot(week, fromDay, fromMeal), d.foodId)
            } ?: next
            write(restored, week)
            _lastAction.value = "به $toMeal منتقل شد."
        }
    }

    /** Swap two cells of the same day — the no-library reorder. */
    fun swapSlots(dayIndex: Int, fromMeal: String, toMeal: String) {
        viewModelScope.launch {
            val week = _weekStart.value
            val rows = rowsOf(week)
            val a = rows.firstOrNull { it.slot.dayIndex == dayIndex && it.slot.mealSlot == fromMeal }
            val b = rows.firstOrNull { it.slot.dayIndex == dayIndex && it.slot.mealSlot == toMeal }
            if (a == null || b == null) {
                _lastAction.value = "هر دو خانه باید پر باشند."
                return@launch
            }
            val next = rows.mapNotNull { row ->
                when {
                    row.slot == a.slot -> PlanEditMath.Row(row.slot, b.foodId)
                    row.slot == b.slot -> PlanEditMath.Row(row.slot, a.foodId)
                    else -> row
                }
            }
            write(next, week)
            _lastAction.value = "جابه‌جا شد."
        }
    }

    /** «تکرار روز» — copy this day's dishes onto the next day. */
    fun duplicateDay(dayIndex: Int) {
        viewModelScope.launch {
            val week = _weekStart.value
            val rows = rowsOf(week)
            val next = PlanEditMath.duplicateDay(rows, dayIndex, MEALS)
            val copied = next.size - rows.size
            write(next, week)
            _lastAction.value = if (copied > 0) "$copied خانه کپی شد." else "روز بعد خالی نیست."
        }
    }

    /**
     * «تکرار هفتهٔ قبل» — fill this week's EMPTY cells from the previous week.
     * Cells already planned here are left alone, so a copy can never silently
     * drop a dish the user chose.
     */
    fun repeatLastWeek() {
        viewModelScope.launch {
            val week = _weekStart.value
            val lastWeek = rowsOf(week - 7L)
            if (lastWeek.isEmpty()) {
                _lastAction.value = "هفتهٔ قبل چیزی ثبت نشده."
                return@launch
            }
            val current = rowsOf(week)
            val copies = PlanEditMath.repeatLastWeek(lastWeek, current, week)
            write(current + copies, week)
            _lastAction.value = if (copies.isEmpty()) {
                "همهٔ خانه‌های این هفته پر است."
            } else {
                "${copies.size} خانه از هفتهٔ قبل آمد."
            }
        }
    }

    /** Rows of a week as [PlanEditMath] values — the shape the math speaks. */
    private suspend fun rowsOf(weekStart: Long): List<PlanEditMath.Row> =
        mealPlanDao.rowsForWeek(weekStart).map {
            PlanEditMath.Row(PlanEditMath.Slot(it.weekStartEpochDay, it.dayIndex, it.mealSlot), it.foodId)
        }

    /**
     * Make the stored cells of [weekStart] match [rows] exactly. `setSlot` is
     * INSERT OR REPLACE on the (week, day, slot) key, so a cell that changed
     * dish is overwritten in place — only cells the edit emptied need a delete.
     */
    private suspend fun write(rows: List<PlanEditMath.Row>, weekStart: Long) {
        val desired = rows.map { it.slot.dayIndex to it.slot.mealSlot }.toSet()
        for (existing in mealPlanDao.rowsForWeek(weekStart)) {
            if ((existing.dayIndex to existing.mealSlot) !in desired) {
                mealPlanDao.clearSlot(weekStart, existing.dayIndex, existing.mealSlot)
            }
        }
        for (row in rows) {
            mealPlanDao.setSlot(weekStart, row.slot.dayIndex, row.slot.mealSlot, row.foodId)
        }
    }

    companion object {
        /** The meal cells the plan grid draws, in order. */
        val MEALS = listOf("صبحانه", "ناهار", "شام")

        /** epochDay of the شنبه opening the week containing [day]. */
        fun weekStartOf(day: LocalDate): LocalDate = PlanEditMath.weekStartOf(day)

        /** The week the app opens on. */
        fun currentWeekStart(): Long = PlanEditMath.weekStartOffset(LocalDate.now(), 0)
    }

    val week: StateFlow<List<MealPlanEntity>> = mealPlanDao.observePlan()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // #84 auto-sync: «همگام‌سازی خودکار برنامه با تقویم» on ⇒ every plan
    // change rewrites that week's events. Idempotent via SYNC_ID dedupe, and
    // a missing permission degrades to nothing — the manual export button on
    // the plan screen is the path that asks, with a Farsi reason.
    val calendarSync: StateFlow<Boolean> = SettingsStore.calendarSync

    init {
        viewModelScope.launch {
            combine(week, calendarSync) { plan, on -> plan to on }
                .collect { (plan, on) -> if (on) autoExport(plan) }
        }
    }

    private suspend fun autoExport(plan: List<MealPlanEntity>) {
        if (!CalendarExport.hasPermission(getApplication())) return
        val foodMap = foods.value.associateBy { it.id }
        val items = plan.mapNotNull { row ->
            foodMap[row.foodId]?.let { CalendarExport.Item(row.dayIndex, row.mealSlot, it.name) }
        }
        if (items.isEmpty()) return
        withContext(Dispatchers.IO) { CalendarExport.exportWeek(getApplication(), items) }
    }
}
