package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.HistoryEntity
import com.erfanbagheri.tahdig.data.local.entity.HistoryWithFood
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<HistoryEntity>>

    @Query("SELECT food_id FROM history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentFoodIds(limit: Int): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE timestamp < :cutoff")
    suspend fun pruneOlderThan(cutoff: Long)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    /** Raw timestamps for heatmap aggregation — a few hundred rows, fits in memory. */
    @Query("SELECT timestamp FROM history ORDER BY timestamp")
    suspend fun allTimestamps(): List<Long>

    // ── History list with food details ───────────────────────────

    /** Most recent picks, newest first, joined with food rows. */
    @Query(
        """
        SELECT
            h.id        AS h_id,
            h.food_id   AS h_food_id,
            h.timestamp AS h_timestamp,
            h.meal_time AS h_meal_time,
            f.id        AS f_id,
            f.name      AS f_name,
            f.name_en   AS f_name_en,
            f.category_id AS f_category_id,
            f.meal_time AS f_meal_time,
            f.cuisine   AS f_cuisine,
            f.difficulty AS f_difficulty,
            f.prep_time_min AS f_prep_time_min,
            f.ingredients AS f_ingredients,
            f.tags      AS f_tags,
            f.description AS f_description,
            f.image_url AS f_image_url,
            f.is_blocked AS f_is_blocked,
            f.equipment AS f_equipment,
            f.flavors  AS f_flavors,
            f.priority  AS f_priority
        FROM history h
        INNER JOIN foods f ON f.id = h.food_id
        ORDER BY h.timestamp DESC
        LIMIT :limit
        """
    )
    fun observeHistoryWithFood(limit: Int = 100): Flow<List<HistoryWithFood>>
}
