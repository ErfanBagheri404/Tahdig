package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.JournalEntity
import com.erfanbagheri.tahdig.util.JournalPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Cooking journal (#124): the «خاطرات پخت» timeline and the per-dish stack.
 *
 * A row is created the moment a cook is logged, with no photo and no note —
 * the capture prompt then fills it in. That way a user who skips the prompt
 * still gets the memory, and the entry never blocks the cook flow.
 */
class JournalViewModel(app: Application) : AndroidViewModel(app) {
    private val db = TahdigDatabase.getInstance(app)
    private val journalDao = db.journalDao()

    val entries: StateFlow<List<JournalEntity>> = journalDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Journal rows for one dish — the «۳ بار پختی» stack. */
    fun forFood(foodId: Long): StateFlow<List<JournalEntity>> =
        journalDao.observeForFood(foodId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun stampCookAsync(foodId: Long, timestamp: Long, onStamped: (Long) -> Unit) {
        viewModelScope.launch {
            val id = journalDao.insert(
                JournalEntity(foodId = foodId, timestamp = timestamp),
            )
            onStamped(id)
        }
    }

    fun setNote(id: Long, note: String) {
        viewModelScope.launch { journalDao.updateNote(id, note) }
    }

    /** Decode off the main thread — a full-size camera shot takes real time. */
    fun setPhoto(id: Long, uri: Uri) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) { JournalPhoto.read(getApplication(), uri) }
            if (bytes != null) journalDao.updatePhoto(id, bytes)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { journalDao.delete(id) }
    }
}
