package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.ColumnInfo

/** One cook: which dish, when. The badge engine's (#121) raw material. */
data class CookRow(
    @ColumnInfo(name = "foodId")
    val foodId: Long,
    val timestamp: Long,
)
