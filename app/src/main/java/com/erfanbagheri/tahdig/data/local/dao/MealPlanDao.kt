package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.MealPlanEntity
import kotlinx.coroutines.flow.Flow

/**
 * Week-anchored plan access (#83).
 *
 * Every cell write names its week: the unique index is (week, day, slot), so
 * INSERT OR REPLACE lands on exactly one row and a copy from last week cannot
 * collide with this week's own cells.
 */
@Dao
interface MealPlanDao {
    /** Set (or replace) the dish for one cell of one week. */
    @Query(
        """
        INSERT OR REPLACE INTO meal_plan (weekStartEpochDay, dayIndex, mealSlot, foodId)
        VALUES (:weekStartEpochDay, :dayIndex, :slot, :foodId)
        """,
    )
    suspend fun setSlot(weekStartEpochDay: Long, dayIndex: Int, slot: String, foodId: Long)

    /** Drop one cell. */
    @Query(
        """
        DELETE FROM meal_plan
        WHERE weekStartEpochDay = :weekStartEpochDay AND dayIndex = :dayIndex AND mealSlot = :slot
        """,
    )
    suspend fun clearSlot(weekStartEpochDay: Long, dayIndex: Int, slot: String)

    /** Every row of every week. */
    @Query("SELECT * FROM meal_plan")
    fun observePlan(): Flow<List<MealPlanEntity>>

    /** All rows of one week — the source for duplicate-day and repeat-last-week. */
    @Query("SELECT * FROM meal_plan WHERE weekStartEpochDay = :weekStartEpochDay ORDER BY dayIndex, mealSlot")
    suspend fun rowsForWeek(weekStartEpochDay: Long): List<MealPlanEntity>

    /** Slot rows for one day of one week. */
    @Query(
        """
        SELECT * FROM meal_plan
        WHERE weekStartEpochDay = :weekStartEpochDay AND dayIndex = :dayIndex
        """,
    )
    fun observeSlotsForDay(weekStartEpochDay: Long, dayIndex: Int): Flow<List<MealPlanEntity>>

    /** Planned dishes for a day, joined with their food rows. */
    @Query("SELECT * FROM foods WHERE id IN (SELECT foodId FROM meal_plan WHERE dayIndex = :dayIndex)")
    fun foodsForDay(dayIndex: Int): Flow<List<FoodEntity>>

    /** Every distinct dish planned — the source for the shopping list. */
    @Query("SELECT DISTINCT foodId FROM meal_plan")
    suspend fun allFoodIds(): List<Long>
}
