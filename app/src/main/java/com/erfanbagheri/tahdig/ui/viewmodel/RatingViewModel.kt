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

    /** Current star rating for [foodId], observed as a flow. */
    fun stars(foodId: Long): StateFlow<Int> = ratingDao.observe(foodId)
        .map { it?.stars ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setStars(foodId: Long, stars: Int) {
        viewModelScope.launch {
            ratingDao.upsert(RatingEntity(foodId = foodId, stars = stars))
        }
    }
}
