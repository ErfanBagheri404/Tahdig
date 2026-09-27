package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.CollectionCount
import com.erfanbagheri.tahdig.data.local.entity.CollectionEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.util.UndoHub
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Named dish groups (#79). Collections are labels on top of favorites: a dish
 * can sit in several, in one, or in none — deleting a collection only drops
 * its joins, never the dishes.
 */
class CollectionViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = TahdigDatabase.getInstance(app).collectionDao()

    /** Every collection with its dish count — the manage screen's row model. */
    val counts: StateFlow<List<CollectionCount>> = dao.observeCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Dish id the «افزودن به مجموعه» sheet is open for, null = closed. */
    private val _sheetFoodId = MutableStateFlow<Long?>(null)
    val sheetFoodId: StateFlow<Long?> = _sheetFoodId.asStateFlow()

    fun openSheet(foodId: Long) { _sheetFoodId.value = foodId }
    fun closeSheet() { _sheetFoodId.value = null }

    /** Ids of the collections holding one dish — the sheet's checkmarks. */
    fun memberIds(foodId: Long): Flow<List<Long>> =
        dao.observeCollectionIdsForFood(foodId)

    /** Ids of the dishes inside one collection — the Favorites filter. */
    fun foodIdsIn(collectionId: Long): Flow<List<Long>> =
        dao.observeFoodIdsIn(collectionId)

    fun foodsIn(collectionId: Long): Flow<List<FoodEntity>> =
        dao.observeFoods(collectionId)

    /** -1 when blank or the name is taken (UNIQUE index). */
    suspend fun create(name: String): Long {
        val clean = name.trim()
        if (clean.isEmpty() || dao.nameExists(clean) != null) return -1L
        return try {
            dao.insert(CollectionEntity(name = clean, createdAt = System.currentTimeMillis()))
        } catch (_: SQLiteConstraintException) {
            -1L
        }
    }

    /** The sheet's «ساخت و افزودن»: new collection + this dish in one go. */
    fun createAndAdd(name: String, foodId: Long) {
        viewModelScope.launch {
            val id = create(name)
            if (id > 0) dao.addFood(id, foodId)
        }
    }

    /** False when blank, taken, or the row is gone. */
    suspend fun rename(id: Long, name: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        return try {
            dao.rename(id, clean) > 0
        } catch (_: SQLiteConstraintException) {
            false
        }
    }

    /** Drops the collection and its joins; the dishes survive, with undo. */
    fun delete(id: Long) {
        viewModelScope.launch {
            val row = dao.all().firstOrNull { it.id == id } ?: return@launch
            val members = dao.foodIdsIn(id)
            dao.delete(id)
            UndoHub.arm("مجموعه حذف شد") {
                viewModelScope.launch {
                    val fresh = dao.insert(
                        CollectionEntity(name = row.name, createdAt = row.createdAt),
                    )
                    if (fresh > 0) members.forEach { dao.addFood(fresh, it) }
                }
            }
        }
    }

    /** Toggles one membership — the sheet taps straight through, no confirm. */
    fun toggle(collectionId: Long, foodId: Long) {
        viewModelScope.launch {
            if (dao.contains(collectionId, foodId) > 0) dao.removeFood(collectionId, foodId)
            else dao.addFood(collectionId, foodId)
        }
    }
}
