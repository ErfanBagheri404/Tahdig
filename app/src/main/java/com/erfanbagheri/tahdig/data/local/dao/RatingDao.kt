package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.RatingEntity
import kotlinx.coroutines.flow.Flow

/** Food rating (1-5 stars), one row per dish. */
@Dao
interface RatingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rating: RatingEntity)

    @Query("SELECT stars FROM ratings WHERE food_id = :foodId LIMIT 1")
    suspend fun getStars(foodId: Long): Int?

    @Query("SELECT * FROM ratings WHERE food_id = :foodId LIMIT 1")
    fun observe(foodId: Long): Flow<RatingEntity?>

    /** All ratings, mapped to foodId -> stars for sorting search results. */
    @Query("SELECT * FROM ratings")
    suspend fun allRatings(): List<RatingEntity>
}
