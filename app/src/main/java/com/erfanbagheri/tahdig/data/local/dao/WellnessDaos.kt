package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.CaffeineLogEntity
import com.erfanbagheri.tahdig.data.local.entity.WaterLogEntity
import com.erfanbagheri.tahdig.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {

    @Query("SELECT * FROM water_log WHERE epochDay = :epochDay LIMIT 1")
    fun observeDay(epochDay: Long): Flow<WaterLogEntity?>

    @Query("SELECT * FROM water_log WHERE epochDay >= :fromEpochDay ORDER BY epochDay")
    fun observeFrom(fromEpochDay: Long): Flow<List<WaterLogEntity>>

    /** Upsert: REPLACE so a step on an existing day overwrites its total. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: WaterLogEntity)
}

@Dao
interface CaffeineDao {

    @Query("SELECT * FROM caffeine_log WHERE epochDay >= :fromEpochDay ORDER BY loggedAt DESC")
    fun observeFrom(fromEpochDay: Long): Flow<List<CaffeineLogEntity>>

    @Query("SELECT * FROM caffeine_log WHERE epochDay = :epochDay ORDER BY loggedAt DESC")
    fun observeDay(epochDay: Long): Flow<List<CaffeineLogEntity>>

    @Insert
    suspend fun insert(row: CaffeineLogEntity): Long

    @Query("DELETE FROM caffeine_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM caffeine_log WHERE id = :id")
    suspend fun byId(id: Long): CaffeineLogEntity?
}

@Dao
interface WeightDao {

    @Query("SELECT * FROM weight_log ORDER BY epochDay")
    fun observeAll(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_log ORDER BY epochDay DESC LIMIT 1")
    fun observeLatest(): Flow<WeightLogEntity?>

    /** One weight per day: logging again the same day replaces it. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: WeightLogEntity)
}
