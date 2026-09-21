package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class CookHeatmapViewModel(app: Application) : AndroidViewModel(app) {
    private val historyDao = TahdigDatabase.getInstance(app).historyDao()

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    /**
     * Day-of-month → number of meals cooked.
     * Populated from raw timestamps, grouped by local midnight.
     */
    private val _counts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val counts: StateFlow<Map<Int, Int>> = _counts.asStateFlow()

    private val _totalThisMonth = MutableStateFlow(0)
    val totalThisMonth: StateFlow<Int> = _totalThisMonth.asStateFlow()

    /** Longest streak (consecutive days with ≥1 cook) ending anywhere in history. */
    private val _longestStreak = MutableStateFlow(0)
    val longestStreak: StateFlow<Int> = _longestStreak.asStateFlow()

    // Cache: all history timestamps, fetched once per ViewModel lifetime.
    // 543 dishes × typical usage = a few hundred rows, fits in memory easily.
    private var allTimestamps: List<Long> = emptyList()

    init {
        viewModelScope.launch {
            allTimestamps = historyDao.allTimestamps()
            loadMonth(YearMonth.now())
        }
    }

    fun nextMonth() = loadMonth(_month.value.plusMonths(1))
    fun prevMonth() = loadMonth(_month.value.minusMonths(1))

    private fun loadMonth(ym: YearMonth) {
        _month.value = ym
        val zone = ZoneId.systemDefault()
        val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val bucket = mutableMapOf<Int, Int>()
        for (ts in allTimestamps) {
            if (ts in start until end) {
                val day = java.time.Instant.ofEpochMilli(ts).atZone(zone).dayOfMonth
                bucket[day] = (bucket[day] ?: 0) + 1
            }
        }
        _counts.value = bucket
        _totalThisMonth.value = bucket.values.sum()

        // Longest streak from all history
        if (allTimestamps.isNotEmpty()) {
            val days = allTimestamps
                .map { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                .toSortedSet()
            var max = 1; var cur = 1
            val sorted = days.toList()
            for (i in 1..sorted.lastIndex) {
                if (sorted[i] == sorted[i - 1].plusDays(1)) { cur++ } else { cur = 1 }
                if (cur > max) max = cur
            }
            _longestStreak.value = max
        }
    }
}
