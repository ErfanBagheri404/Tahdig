package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.HistoryEntity
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
}
