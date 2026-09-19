package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.Embedded

/** History row joined with its food. Used by the history list. */
data class HistoryWithFood(
    @Embedded(prefix = "h_")
    val history: HistoryEntity,

    @Embedded(prefix = "f_")
    val food: FoodEntity,
)
