package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.ShoppingTripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingTripDao {

    @Insert
    suspend fun insert(trip: ShoppingTripEntity): Long

    /** Newest trip first — history reads top-down. */
    @Query("SELECT * FROM shopping_trips ORDER BY ended_at DESC")
    fun observeAll(): Flow<List<ShoppingTripEntity>>

    @Query("DELETE FROM shopping_trips")
    suspend fun clearAll()
}
