package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentViewDao {
    /** Record a view; re-viewing bumps the timestamp (unique food_id). */
    @Query("INSERT OR REPLACE INTO recent_views (food_id, viewed_at) VALUES (:foodId, :at)")
    suspend fun recordView(foodId: Long, at: Long = System.currentTimeMillis())

    @Query(
        """
        SELECT f.* FROM foods f
        INNER JOIN recent_views rv ON f.id = rv.food_id
        ORDER BY rv.viewed_at DESC
        LIMIT :limit
        """
    )
    fun recentFoods(limit: Int = 10): Flow<List<FoodEntity>>
}
