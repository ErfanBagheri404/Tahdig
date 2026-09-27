package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.WaterLogEntity
import com.erfanbagheri.tahdig.data.local.entity.WeightLogEntity
import com.erfanbagheri.tahdig.util.WaterMath
import com.erfanbagheri.tahdig.util.WeightTrend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Water + body-weight (#115).
 *
 * Dates are read from the SYSTEM clock at the ViewModel edge, never from the
 * math objects — that is the whole reason the midnight reset and the
 * sparse-data average are unit-testable.
 */
class WellnessViewModel(app: Application) : AndroidViewModel(app) {

    private val db = TahdigDatabase.getInstance(app)

    /** Today in the device's own zone. */
    private fun today(): LocalDate = LocalDate.now()

    val todayWater: StateFlow<Int?> = db.waterDao()
        .observeDay(today().toEpochDay())
        .map { it?.ml }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Seven-day bar row, newest last; a missing day contributes 0. */
    val weekWater: StateFlow<List<Pair<LocalDate, Int>>> = db.waterDao()
        .observeFrom(today().minusDays(6).toEpochDay())
        .map { rows ->
            val byDay = rows.associate { it.epochDay to it.ml }
            WaterMath.weekBuckets(today()).map { d -> d to (byDay[d.toEpochDay()] ?: 0) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightEntries: StateFlow<List<WeightLogEntity>> = db.weightDao()
        .observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestWeight: StateFlow<WeightLogEntity?> = db.weightDao()
        .observeLatest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addWater(stepMultiplier: Double) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val day = today().toEpochDay()
                val current = db.waterDao().observeDay(day).first()?.ml ?: 0
                // Glass size lives in settings; the VM holds a copy so the
                // stepper amount is stable between a settings write and a
                // recomposition.
                val glass = glassSizeMl.value
                db.waterDao().upsert(
                    WaterLogEntity(
                        epochDay = day,
                        ml = current + WaterMath.stepMl(glass, stepMultiplier),
                    ),
                )
            }
        }
    }

    fun logWeight(kg: Double) {
        if (kg <= 0 || kg > 500) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.weightDao().upsert(WeightLogEntity(today().toEpochDay(), kg))
            }
        }
    }

    // --- settings-mirrored state -------------------------------------------

    /**
     * Glass size and manual target come from SettingsStore, not local state:
     * they must survive a restart and be shared with any Settings screen.
     * `goalWeightKg` stays VM-local because it is the weight the calorie goal
     * was computed from, which lives with the goal (#110) rather than here.
     */
    val glassSizeMl: StateFlow<Int> = com.erfanbagheri.tahdig.data.prefs.SettingsStore.glassSizeMl

    /** User-set target override; null means derive it from weight. */
    val targetOverrideMl: StateFlow<Int?> = com.erfanbagheri.tahdig.data.prefs.SettingsStore.waterTargetMl

    /** The weight the calorie goal was computed from — for the BMR prompt. */
    private val _goalWeightKg = MutableStateFlow<Double?>(null)
    val goalWeightKg: StateFlow<Double?> = _goalWeightKg

    fun setGlassSize(ml: Int) {
        if (ml <= 0 || ml > 2000) return
        com.erfanbagheri.tahdig.data.prefs.SettingsStore.setGlassSizeMl(ml)
    }

    fun setTargetOverride(ml: Int?) {
        com.erfanbagheri.tahdig.data.prefs.SettingsStore.setWaterTargetMl(ml)
    }

    fun setGoalWeight(kg: Double?) {
        _goalWeightKg.value = kg?.takeIf { it > 0 }
    }

    /**
     * Accept the BMR-recalculation prompt: adopt the latest logged weight as
     * the goal's basis, which is what silences [goalStale].
     */
    fun setGoalWeightFromLatest() {
        latestWeight.value?.kg?.let { setGoalWeight(it) }
    }

    // --- derived ------------------------------------------------------------

    /** Effective target for today. */
    fun targetMl(): Int? =
        WaterMath.targetMl(latestWeight.value?.kg, glassSizeMl.value, targetOverrideMl.value)

    /** Series for the trend chart. */
    fun trendSeries(): List<WeightTrend.Point> =
        WeightTrend.series(weightEntries.value.map { WeightTrend.Entry(LocalDate.ofEpochDay(it.epochDay), it.kg) })

    /** Whether the calorie goal's stored weight has drifted ≥2kg. */
    fun goalStale(): Boolean =
        WeightTrend.goalStale(_goalWeightKg.value, latestWeight.value?.kg)
}
