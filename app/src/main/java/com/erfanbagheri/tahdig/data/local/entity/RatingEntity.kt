package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One row per dish: how many stars (1-5) the user assigned. */
@Entity(
    tableName = "ratings",
    indices = [Index("food_id", unique = true)],
)
data class RatingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "food_id") val foodId: Long,
    /** 1-5 stars; 0 = no rating. */
    val stars: Int,
)
