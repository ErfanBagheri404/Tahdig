package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.MilestoneCheckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneCheckDao {

    @Query("SELECT ingredientHash FROM milestone_checks WHERE foodId = :foodId")
    fun observeHashes(foodId: Long): Flow<List<String>>

    @Query("SELECT ingredientHash FROM milestone_checks WHERE foodId = :foodId")
    suspend fun hashesFor(foodId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(row: MilestoneCheckEntity)

    @Query("DELETE FROM milestone_checks WHERE foodId = :foodId AND ingredientHash = :hash")
    suspend fun uncheck(foodId: Long, hash: String)

    /** Reset via «پاک کردن» — per dish, never touches other dishes' prep state. */
    @Query("DELETE FROM milestone_checks WHERE foodId = :foodId")
    suspend fun clearForFood(foodId: Long)
}
