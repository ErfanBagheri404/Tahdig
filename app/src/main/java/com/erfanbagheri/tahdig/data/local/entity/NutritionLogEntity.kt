package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One «پختم» stamped with the dish's per-serving nutrition (#110) — the only
 * log source: no manual food entry, exactly as the issue specifies.
 *
 * Numbers are snapshotted, not joined: if the estimate improves later, the
 * day you actually ate stays what it was.
 */
@Entity(tableName = "nutrition_log")
data class NutritionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "food_id")
    val foodId: Long,

    @ColumnInfo(name = "food_name")
    val foodName: String,

    /** Local-day key (yyyy-MM-dd) so "today" is a simple equality. */
    @ColumnInfo(name = "day")
    val day: String,

    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int,

    @ColumnInfo(name = "logged_at")
    val loggedAt: Long = System.currentTimeMillis(),
)
