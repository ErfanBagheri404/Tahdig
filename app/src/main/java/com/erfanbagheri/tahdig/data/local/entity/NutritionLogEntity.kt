package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.erfanbagheri.tahdig.util.MealTimeHelper
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

    /**
     * Meal slot this row was logged into (#114), one of [MealTimeHelper]'s
     * bucket keys. Assigned from the clock AT LOG TIME and then never
     * recomputed: a 23:00 dinner stays dinner even when the report runs at
     * breakfast, and the user can move it afterwards.
     */
    @ColumnInfo(name = "meal_slot")
    val mealSlot: String = MealTimeHelper.currentBucket(),

    /**
     * Servings as logged (#114), 1.0 for the normal «one serving» stamp.
     * Re-scaling a row multiplies its macros rather than logging a second
     * row, so editing servings never inflates the day's log count.
     */
    @ColumnInfo(name = "servings")
    val servings: Double = 1.0,
)
