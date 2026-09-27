package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.util.BadgeEngine
import com.erfanbagheri.tahdig.util.StreakMath
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Badge collection (#121).
 *
 * The unlocked set is NOT stored: it is recomputed from history every time the
 * screen opens, which makes "unlock fires once" a property of the data rather
 * than a bookkeeping table that can drift. [lastSeen] is the only persisted
 * state, and it exists purely to decide which badges are NEW since the last
 * look — the snackbar's job, not the unlock's.
 */
class BadgeViewModel(app: Application) : AndroidViewModel(app) {

    private val db = TahdigDatabase.getInstance(app)

    private val _states = MutableStateFlow<List<BadgeEngine.State>>(emptyList())
    val states: StateFlow<List<BadgeEngine.State>> = _states.asStateFlow()

    private val _newlyUnlocked = MutableStateFlow<List<BadgeEngine.Def>>(emptyList())

    /** Badges unlocked since the last time the user saw the collection. */
    val newlyUnlocked: StateFlow<List<BadgeEngine.Def>> = _newlyUnlocked.asStateFlow()

    init {
        refresh()
    }

    /**
     * Recompute from the DB. Safe to call on every app open and after every
     * cook — the engine is pure, so a repeat call with unchanged history
     * produces the same set and cannot double-fire the snackbar.
     */
    fun refresh() {
        viewModelScope.launch {
            val history = buildHistory()
            val states = withContext(Dispatchers.Default) { BadgeEngine.states(history) }
            _states.value = states

            val unlocked = states.filter { it.unlocked }.map { it.def.id }.toSet()
            val seen = SettingsStore.badgesSeen.value
            // Only diff when we HAVE a previous snapshot: on a first ever run
            // `seen` is empty, and reporting all 30 as new would be a lie.
            if (seen.isNotEmpty()) {
                _newlyUnlocked.value = BadgeEngine.newlyUnlocked(seen, unlocked)
            }
            SettingsStore.setBadgesSeen(unlocked)
        }
    }

    /** The snapshot the engine judges, read from what the app already stores. */
    private suspend fun buildHistory(): BadgeEngine.History {
        val rows = db.historyDao().allCookRows()
        val dishes = db.historyDao().distinctCookedDishes()
        val prepById = dishes.associate { it.id to it.prepTimeMin }
        val noted = db.journalDao().notedTimestamps().toSet()
        val zone = ZoneId.systemDefault()

        val cooks = rows.map { r ->
            val at = Instant.ofEpochMilli(r.timestamp).atZone(zone)
            BadgeEngine.Cook(
                foodId = r.foodId,
                at = at.toLocalDate(),
                hour = at.hour,
                prepTimeMin = prepById[r.foodId] ?: 0,
                // A note counts only if the journal row for THAT cook has text.
                hasNote = r.timestamp in noted,
            )
        }

        val cookedDays = cooks.map { it.at }.toSet()
        val streak = StreakMath.compute(
            cookedDays = cookedDays,
            today = LocalDate.now(zone),
            freezesLeft = SettingsStore.freezes.value,
            weeklyFloor = SettingsStore.weeklyFloor.value,
        )

        return BadgeEngine.History(
            cooks = cooks,
            distinctDishes = dishes.map {
                BadgeEngine.DistinctDish(
                    id = it.id,
                    categoryId = it.categoryId,
                    cuisine = it.cuisine,
                    prepTimeMin = it.prepTimeMin,
                )
            },
            totalCategories = db.categoryDao().count(),
            totalCuisines = 0, // seed has no cuisine column on every dish
            longestStreak = streak.longest,
            freezesGranted = SettingsStore.freezes.value,
            waterGoalDayStreak = waterGoalStreak(),
        )
    }

    /**
     * Consecutive days meeting the water goal, counted backwards from today.
     * The streak breaks on the first day that missed — and a day with no
     * water logged at all is a miss, not a pass.
     */
    private suspend fun waterGoalStreak(): Int {
        // Weight is a per-day log, not a profile value: the latest row
        // decides the target, exactly as the wellness card computes it.
        val latest = db.weightDao().latest()
        val targetMl = com.erfanbagheri.tahdig.util.WaterMath.targetMl(
            weightKg = latest?.kg,
            glassSizeMl = SettingsStore.glassSizeMl.value,
            overrideMl = SettingsStore.waterTargetMl.value,
        ) ?: return 0
        if (targetMl <= 0) return 0

        val byDay = db.waterDao().allTotals().associate { it.epochDay to it.ml }
        val zone = ZoneId.systemDefault()
        var day = LocalDate.now(zone)
        var run = 0
        // Cap the walk at a year so a corrupted table cannot loop forever.
        repeat(366) {
            val ml = byDay[day.toEpochDay()] ?: return run
            if (ml < targetMl) return run
            run++
            day = day.minusDays(1)
        }
        return run
    }

    /** Clear the "new" list once the snackbar has shown. */
    fun acknowledge() {
        _newlyUnlocked.value = emptyList()
    }
}
