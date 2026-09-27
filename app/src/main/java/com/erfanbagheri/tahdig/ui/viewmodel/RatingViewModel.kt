package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.RatingEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Stars and the private note for a dish (#134).
 *
 * Both setters write a single column. That is not an optimisation: building a
 * whole [RatingEntity] to change one field would blank the other, so tapping a
 * star would silently erase the user's note. See [RatingDao] for the queries.
 */
class RatingViewModel(app: Application) : AndroidViewModel(app) {
    private val ratingDao = TahdigDatabase.getInstance(app).ratingDao()

    // Cache one flow per food so recomposition reuses the same stateIn subscription
    private val cache = mutableMapOf<Long, StateFlow<Int>>()
    private val noteCache = mutableMapOf<Long, StateFlow<String>>()

    /** Current star rating for [foodId], observed as a flow. */
    fun stars(foodId: Long): StateFlow<Int> = cache.getOrPut(foodId) {
        ratingDao.observe(foodId)
            .map { it?.stars ?: 0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    }

    /** The private note for [foodId]; "" when none was written. */
    fun note(foodId: Long): StateFlow<String> = noteCache.getOrPut(foodId) {
        ratingDao.observe(foodId)
            .map { it?.note.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    }

    /** Accepts 0 (clear) or 1-5; anything else is ignored. Keeps the note. */
    fun setStars(foodId: Long, stars: Int) {
        if (stars !in 0..5) return
        viewModelScope.launch {
            // Ensure a row exists, then set only the stars. On a fresh dish the
            // INSERT is what lands; afterwards the UPDATE is. Either way the
            // note column is never touched.
            val now = System.currentTimeMillis()
            ratingDao.insertIfAbsent(RatingEntity(foodId = foodId, stars = stars))
            // #92: stamp the time so the taste scorer can decay an old opinion.
            ratingDao.updateStars(foodId, stars, now)
        }
    }

    /**
     * Save the note. Trimmed, and truncated at [RatingEntity.MAX_NOTE_CHARS] so
     * a paste-bomb cannot bloat the DB. Keeps the stars.
     */
    fun setNote(foodId: Long, note: String) {
        val clean = note.trim().take(RatingEntity.MAX_NOTE_CHARS)
        viewModelScope.launch {
            ratingDao.insertIfAbsent(RatingEntity(foodId = foodId, stars = 0))
            ratingDao.updateNote(foodId, clean)
        }
    }

    /** Delete the note, keep the stars — the undo path's inverse. */
    fun clearNote(foodId: Long) = setNote(foodId, "")
}
