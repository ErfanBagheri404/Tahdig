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

class RatingViewModel(app: Application) : AndroidViewModel(app) {
    private val ratingDao = TahdigDatabase.getInstance(app).ratingDao()

    // Cache one flow per food so recomposition reuses the same stateIn subscription
    private val cache = mutableMapOf<Long, StateFlow<Int>>()

    /** Current star rating for [foodId], observed as a flow. */
    fun stars(foodId: Long): StateFlow<Int> = cache.getOrPut(foodId) {
        ratingDao.observe(foodId)
            .map { it?.stars ?: 0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    }

    /** Accepts 0 (clear) or 1-5; anything else is ignored. */
    fun setStars(foodId: Long, stars: Int) {
        if (stars !in 0..5) return
        viewModelScope.launch {
            ratingDao.upsert(RatingEntity(foodId = foodId, stars = stars))
        }
    }
}
