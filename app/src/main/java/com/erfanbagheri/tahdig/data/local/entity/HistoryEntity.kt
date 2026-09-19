package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "food_id")
    val foodId: Long,

    /** ISO-8601 UTC timestamp of when this food was suggested/picked */
    val timestamp: Long,

    /** The meal-time bucket at the moment of the pick */
    @ColumnInfo(name = "meal_time")
    val mealTime: String,
)
