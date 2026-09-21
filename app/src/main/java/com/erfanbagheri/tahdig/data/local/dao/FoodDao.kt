package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods WHERE is_blocked = 0 ORDER BY name")
    fun observeAll(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE is_blocked = 0 AND (',' || meal_time || ',') LIKE '%,' || :mealTime || ',%' ORDER BY name")
    fun observeByMealTime(mealTime: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FoodEntity?

    @Query("SELECT COUNT(*) FROM foods")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    /** Random pick, filtered by meal time and optional category. Excludes blocked foods. */
    @Query(
        """
        SELECT * FROM foods
        WHERE is_blocked = 0
          AND (',' || meal_time || ',') LIKE '%,' || :mealTime || ',%'
          AND (:categoryId IS NULL OR category_id = :categoryId)
        ORDER BY RANDOM()
        LIMIT 1
        """
    )
    suspend fun randomByMealTime(mealTime: String, categoryId: Long?): FoodEntity?

    @Query("SELECT * FROM foods WHERE is_blocked = 0 ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomAny(limit: Int): List<FoodEntity>

    /**
     * Deterministic "dish of the day": the same row for the whole day.
     * OFFSET makes it stable across widget refreshes and app restarts, unlike RANDOM().
     */
    @Query("SELECT * FROM foods WHERE is_blocked = 0 ORDER BY id LIMIT 1 OFFSET :index")
    suspend fun byIndex(index: Int): FoodEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(foods: List<FoodEntity>)

    @Update
    suspend fun update(food: FoodEntity)

    @Query("UPDATE foods SET is_blocked = :blocked WHERE id = :id")
    suspend fun setBlocked(id: Long, blocked: Boolean)

    // ── Search & filter ──────────────────────────────────────────

    /** Search by name (Farsi or English) + optional category filter. */
    @Query(
        """
        SELECT * FROM foods
        WHERE is_blocked = 0
          AND (:query = '' OR name LIKE '%' || :query || '%' OR name_en LIKE '%' || :query || '%')
          AND (:categoryId IS NULL OR category_id = :categoryId)
        ORDER BY name
        """
    )
    fun search(query: String, categoryId: Long?): Flow<List<FoodEntity>>

    /** Search by name only (no category filter). */
    @Query(
        """
        SELECT * FROM foods
        WHERE is_blocked = 0
          AND (:query = '' OR name LIKE '%' || :query || '%' OR name_en LIKE '%' || :query || '%')
        ORDER BY name
        """
    )
    fun searchByName(query: String): Flow<List<FoodEntity>>
}
