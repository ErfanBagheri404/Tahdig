package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.FavoriteEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE food_id = :foodId LIMIT 1")
    suspend fun getByFoodId(foodId: Long): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE food_id = :foodId")
    suspend fun deleteByFoodId(foodId: Long)

    /** Every blocked row, for the clear-blocked undo (#127). */
    @Query("SELECT * FROM favorites WHERE is_blocked = 1")
    suspend fun blockedRows(): List<FavoriteEntity>

    @Query("SELECT COUNT(*) FROM favorites WHERE is_blocked = 1")
    fun observeBlockedCount(): Flow<Int>

    // ── List queries ─────────────────────────────────────────────

    /** All favorited foods (is_blocked=false) with food details. */
    @Query(
        """
        SELECT f.* FROM foods f
        INNER JOIN favorites fav ON f.id = fav.food_id
        WHERE fav.is_blocked = 0
        ORDER BY f.name
        """
    )
    fun observeFavoritedFoods(): Flow<List<FoodEntity>>

    /** All blocked foods (is_blocked=true) with food details. */
    @Query(
        """
        SELECT f.* FROM foods f
        INNER JOIN favorites fav ON f.id = fav.food_id
        WHERE fav.is_blocked = 1
        ORDER BY f.name
        """
    )
    fun observeBlockedFoods(): Flow<List<FoodEntity>>

    /** Remove all blocked entries. */
    @Query("DELETE FROM favorites WHERE is_blocked = 1")
    suspend fun clearBlocked()

    // -- #92 taste profile --------------------------------------------------

    /** Favorited dish ids, for the scorer's weak favorite signal. */
    @Query("SELECT food_id FROM favorites WHERE is_blocked = 0")
    suspend fun favoritedIds(): List<Long>

    /**
     * Blocked (hidden) dish ids. Kept as ids and not joined dishes: the scorer
     * only needs to know the dish is off-limits, and a hidden dish may well
     * have been deleted from `foods` since.
     */
    @Query("SELECT food_id FROM favorites WHERE is_blocked = 1")
    suspend fun blockedIds(): List<Long>
}
