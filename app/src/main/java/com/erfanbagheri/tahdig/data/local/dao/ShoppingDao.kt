package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    @Query("SELECT * FROM shopping_list ORDER BY is_checked, created_at DESC")
    fun observeAll(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT COUNT(*) FROM shopping_list WHERE is_checked = 0")
    fun observePendingCount(): Flow<Int>

    @Insert
    suspend fun insertAll(items: List<ShoppingItemEntity>)

    /**
     * Exact-restore insert for undo (#127). REPLACE, not IGNORE: the row's own
     * id and created_at must come back, or an undone delete silently reorders
     * the list it was deleted from.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restore(item: ShoppingItemEntity)

    @Query("UPDATE shopping_list SET is_checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_list WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM shopping_list WHERE is_checked = 1")
    suspend fun clearChecked()

    @Query("DELETE FROM shopping_list")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM shopping_list WHERE food_id = :foodId")
    suspend fun countForFood(foodId: Long): Int

    /** All rows with ids — needed to merge quantities without losing checked state. */
    @Query("SELECT * FROM shopping_list ORDER BY created_at")
    suspend fun allRows(): List<ShoppingItemEntity>

    /** Single row by id — captured before a delete so undo can restore it (#127). */
    @Query("SELECT * FROM shopping_list WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): ShoppingItemEntity?

    @Query("UPDATE shopping_list SET item = :text WHERE id = :id")
    suspend fun updateText(id: Long, text: String)
}
