package com.erfanbagheri.tahdig.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists the last 10 unique search queries in SharedPreferences.
 *
 * The list lives in a companion-level flow so every instance (the ViewModel writing,
 * the screen reading) observes the same value — a per-instance property would leave the
 * UI stale after [record] or [clear] until the next recreation.
 */
class SearchHistory(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Observed by the search screen; updates immediately on [record]/[clear]. */
    val queries: StateFlow<List<String>> = _queries

    init {
        // Seed from disk once per process.
        if (!loaded) {
            _queries.value = readFromPrefs()
            loaded = true
        }
    }

    fun record(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        val list = _queries.value.toMutableList().apply {
            remove(q)
            add(0, q)
        }.take(MAX_ITEMS)
        _queries.value = list
        prefs.edit().putString(KEY, list.joinToString(SEPARATOR.toString())).apply()
    }

    fun clear() {
        _queries.value = emptyList()
        prefs.edit().remove(KEY).apply()
    }

    private fun readFromPrefs(): List<String> =
        (prefs.getString(KEY, "") ?: "")
            .split(SEPARATOR)
            .filter { it.isNotEmpty() }

    companion object {
        private const val PREFS_NAME = "tahdig_settings"
        private const val KEY = "search_history_list"
        private const val SEPARATOR = '\u001F'
        private const val MAX_ITEMS = 10

        private val _queries = MutableStateFlow<List<String>>(emptyList())
        private var loaded = false
    }
}
