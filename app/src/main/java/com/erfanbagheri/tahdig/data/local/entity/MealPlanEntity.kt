package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One planned dish cell: a (week, day, meal) triple holding one food.
 *
 * [weekStartEpochDay] is the epochDay of that week's شنبه (Iranian week start).
 * It exists because #83's «تکرار هفتهٔ قبل» needs "last week" to be addressable —
 * with only (day, meal) as the key there is exactly one plan and no previous
 * week to copy from. dayIndex 0=شنبه .. 6=جمعه.
 */
@Entity(
    tableName = "meal_plan",
    indices = [Index(value = ["weekStartEpochDay", "dayIndex", "mealSlot"], unique = true)],
)
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekStartEpochDay: Long,
    val dayIndex: Int,
    val mealSlot: String,
    val foodId: Long,
)
