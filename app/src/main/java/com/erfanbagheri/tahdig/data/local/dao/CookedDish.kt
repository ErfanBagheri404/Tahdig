package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.ColumnInfo

/**
 * One row per distinct dish ever cooked, for the badge engine (#121).
 * Snake-case columns match the Room field-name convention: a Room POJO maps
 * columns to fields by name, so camelCase aliases here would come back NULL.
 */
data class CookedDish(
    val id: Long,
    @ColumnInfo(name = "category_id")
    val categoryId: Int,
    val cuisine: String,
    @ColumnInfo(name = "prep_time_min")
    val prepTimeMin: Int,
    @ColumnInfo(name = "first_cook_at")
    val firstCookAt: Long,
)
