package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel

/** Persists the last 10 unique search queries in SharedPreferences (comma-joined). */
class SearchHistory(app: Application) : AndroidViewModel(app) {
    private val prefs: SharedPreferences =
        app.getSharedPreferences("tahdig_settings", Application.MODE_PRIVATE)
    private val KEY = "search_history_list"

    val queries: List<String>
        get() = (prefs.getString(KEY, "") ?: "")
            .split('\u001F').filter { it.isNotEmpty() }

    fun record(query: String) {
        if (query.isBlank()) return
        val list = queries.toMutableList().apply {
            remove(query)
            add(0, query)
        }.take(10)
        prefs.edit().putString(KEY, list.joinToString("\u001F")).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }
}
