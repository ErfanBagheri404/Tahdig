package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One planned dish slot: a (day, meal) pair holding one food. dayIndex 0=شنبه .. 6=جمعه. */
@Entity(
    tableName = "meal_plan",
    indices = [Index(value = ["dayIndex", "mealSlot"], unique = true)],
)
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayIndex: Int,
    val mealSlot: String,
    val foodId: Long,
)
