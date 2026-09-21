package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.PantryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {

    @Query("SELECT * FROM pantry ORDER BY item")
    fun observeAll(): Flow<List<PantryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: PantryItemEntity): Long

    @Query("DELETE FROM pantry WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pantry")
    suspend fun clearAll()

    /** Raw strings for in-memory filtering, no Flow needed for one-shot checks. */
    @Query("SELECT item FROM pantry")
    suspend fun allItems(): List<String>
}
