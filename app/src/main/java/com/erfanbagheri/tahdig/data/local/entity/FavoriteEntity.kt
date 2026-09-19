package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "favorites",
    primaryKeys = ["food_id"],
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("food_id")],
)
data class FavoriteEntity(
    @ColumnInfo(name = "food_id")
    val foodId: Long,

    /** true = user explicitly blocked this food; false = user favorited */
    @ColumnInfo(name = "is_blocked")
    val isBlocked: Boolean = false,
)
