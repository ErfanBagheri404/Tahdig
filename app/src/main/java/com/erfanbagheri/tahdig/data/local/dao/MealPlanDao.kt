package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.MealPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {
    /** Set (or replace) the dish for a day+slot. */
    @Query("INSERT OR REPLACE INTO meal_plan (dayIndex, mealSlot, foodId) VALUES (:dayIndex, :slot, :foodId)")
    suspend fun setSlot(dayIndex: Int, slot: String, foodId: Long)

    @Query("DELETE FROM meal_plan WHERE dayIndex = :dayIndex AND mealSlot = :slot")
    suspend fun clearSlot(dayIndex: Int, slot: String)

    @Query("SELECT * FROM meal_plan")
    fun observePlan(): Flow<List<MealPlanEntity>>

    /** Slot rows for one day (avoids pushing the whole-week plan through every day view). */
    @Query("SELECT * FROM meal_plan WHERE dayIndex = :dayIndex")
    fun observeSlotsForDay(dayIndex: Int): Flow<List<MealPlanEntity>>

    /** Planned dishes for a day, joined with their food rows. */
    @Query("SELECT * FROM foods WHERE id IN (SELECT foodId FROM meal_plan WHERE dayIndex = :dayIndex)")
    fun foodsForDay(dayIndex: Int): Flow<List<FoodEntity>>

    /** Every distinct dish planned for the week — the source for the shopping list. */
    @Query("SELECT DISTINCT foodId FROM meal_plan")
    suspend fun allFoodIds(): List<Long>
}
