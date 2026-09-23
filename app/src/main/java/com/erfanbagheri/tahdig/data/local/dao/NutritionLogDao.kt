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
}

/** Row shape of [NutritionLogDao.observeTotals] — plain ints, no entity. */
data class NutritionTotals(
    val cal: Int,
    val pro: Int,
    val fat: Int,
    val carb: Int,
)
