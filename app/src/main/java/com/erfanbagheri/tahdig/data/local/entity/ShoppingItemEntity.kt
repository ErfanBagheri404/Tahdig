package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One line item on the user's shopping list. */
@Entity(
    tableName = "shopping_list",
    indices = [Index("food_id")],
)
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Source dish, when the item came from a suggestion. Null for manually added items. */
    @ColumnInfo(name = "food_id")
    val foodId: Long? = null,

    /** Ingredient text, e.g. "لوبیا قرمز" */
    val item: String,

    @ColumnInfo(name = "is_checked")
    val isChecked: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
