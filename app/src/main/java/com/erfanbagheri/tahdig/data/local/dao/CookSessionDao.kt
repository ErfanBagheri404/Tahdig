package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.erfanbagheri.tahdig.data.local.entity.CookSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CookSessionDao {

    @Query("SELECT * FROM cook_sessions WHERE food_id = :foodId")
    fun observe(foodId: Long): Flow<CookSessionEntity?>

    @Query("SELECT * FROM cook_sessions WHERE food_id = :foodId")
    suspend fun get(foodId: Long): CookSessionEntity?

    @Upsert
    suspend fun save(session: CookSessionEntity)

    /** Normal exit — clears the «ادامه بده» offer. */
    @Query("DELETE FROM cook_sessions WHERE food_id = :foodId")
    suspend fun clear(foodId: Long)
}
