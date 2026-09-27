package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodTagJoin
import com.erfanbagheri.tahdig.data.local.entity.TagEntity
import com.erfanbagheri.tahdig.util.PersianText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** User tags (#80): the dish sheet + the Settings Tags manager. */
class TagViewModel(app: Application) : AndroidViewModel(app) {
    internal val dao = TahdigDatabase.getInstance(app).tagDao()

    val allTags: Flow<List<TagEntity>> = dao.observeAll()

    private val _sheetFoodId = MutableStateFlow<Long?>(null)
    val sheetFoodId: StateFlow<Long?> = _sheetFoodId.asStateFlow()

    fun observeFoodTags(foodId: Long): Flow<List<TagEntity>> = dao.observeForFood(foodId)

    /** Autocomplete over existing tags, matched on the normalized name. */
    fun suggestions(all: List<TagEntity>, typed: String): List<TagEntity> {
        val q = PersianText.normalize(typed)
        if (q.isEmpty()) return all
        return all.filter { PersianText.normalize(it.name).contains(q) }
    }

    fun openSheet(foodId: Long) { _sheetFoodId.value = foodId }
    fun closeSheet() { _sheetFoodId.value = null }

    /** Normalized get-or-create, then attach to the dish. */
    fun addTag(foodId: Long, raw: String) {
        if (PersianText.normalize(raw).isEmpty()) return
        viewModelScope.launch { dao.assign(FoodTagJoin(foodId, dao.getOrCreate(raw))) }
    }

    fun removeTag(foodId: Long, tagId: Long) {
        viewModelScope.launch { dao.unassign(foodId, tagId) }
    }

    /** Delete drops joins only (CASCADE); dishes are never touched. */
    fun deleteTag(tagId: Long) {
        viewModelScope.launch { dao.delete(tagId) }
    }

    /** Rename; a collision with an existing spelling merges the two instead. */
    fun renameTag(tagId: Long, raw: String) {
        val name = PersianText.normalize(raw)
        if (name.isEmpty()) return
        viewModelScope.launch {
            val existing = dao.byName(name)
            if (existing != null && existing.id != tagId) dao.merge(tagId, existing.id)
            else dao.rename(tagId, name)
        }
    }
}
