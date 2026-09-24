package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.NutritionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionLogDao {

    @Insert
    suspend fun insert(entry: NutritionLogEntity)

    /** Everything eaten on [day] (yyyy-MM-dd), newest first. */
    @Query("SELECT * FROM nutrition_log WHERE day = :day ORDER BY logged_at DESC")
    fun observeDay(day: String): Flow<List<NutritionLogEntity>>

    /** Same-day totals in SQL — one row, no flow multiplication for the card. */
    @Query(
        "SELECT COALESCE(SUM(calories),0) AS cal, COALESCE(SUM(protein),0) AS pro, " +
            "COALESCE(SUM(fat),0) AS fat, COALESCE(SUM(carbs),0) AS carb " +
            "FROM nutrition_log WHERE day = :day",
    )
    fun observeTotals(day: String): Flow<NutritionTotals>

    @Query("DELETE FROM nutrition_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM nutrition_log")
    suspend fun clearAll()

    /** Rows in a date range (inclusive) for the weekly report (#114). */
    @Query("SELECT * FROM nutrition_log WHERE day >= :fromDay AND day <= :toDay ORDER BY logged_at DESC")
    fun observeRange(fromDay: String, toDay: String): Flow<List<NutritionLogEntity>>

    /** Re-slot a row after the user moves it in the diary (#114). */
    @Query("UPDATE nutrition_log SET meal_slot = :slot WHERE id = :id")
    suspend fun updateSlot(id: Long, slot: String)

    /**
     * Edit a row's time (#114). The slot AND the day key are written in the
     * SAME statement from the new timestamp — a row edited to 12:00 has to
     * become LUNCH too, and a 00:05 shift of −15m crosses midnight into
     * yesterday's bucket rather than sitting in the wrong day.
     */
    @Query("UPDATE nutrition_log SET logged_at = :loggedAt, meal_slot = :slot, day = :day WHERE id = :id")
    suspend fun updateTime(id: Long, loggedAt: Long, slot: String, day: String)

    /**
     * Re-scale a row (#114). The macros are multiplied HERE rather than by
     * reading, scaling and writing back in the ViewModel — one statement, and
     * no window where the row holds 2x calories and 1x protein.
     */
    @Query(
        "UPDATE nutrition_log SET servings = :servings, " +
            "calories = CAST(calories * :factor AS INTEGER), " +
            "protein = CAST(protein * :factor AS INTEGER), " +
            "fat = CAST(fat * :factor AS INTEGER), " +
            "carbs = CAST(carbs * :factor AS INTEGER) WHERE id = :id",
    )
    suspend fun rescale(id: Long, servings: Double, factor: Double)

    /** Today's full row, for a one-shot undo after a delete. */
    @Query("SELECT * FROM nutrition_log WHERE id = :id")
    suspend fun byId(id: Long): NutritionLogEntity?
}

/** Row shape of [NutritionLogDao.observeTotals] — plain ints, no entity. */
data class NutritionTotals(
    val cal: Int,
    val pro: Int,
    val fat: Int,
    val carb: Int,
)
