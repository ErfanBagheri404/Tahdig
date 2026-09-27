package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
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

    @Query("UPDATE shopping_list SET is_checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_list WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM shopping_list WHERE is_checked = 1")
    suspend fun clearChecked()

    @Query("DELETE FROM shopping_list")
    suspend fun clearAll()

    /** Checked rows as entities, so a clear can be undone by re-inserting them. */
    @Query("SELECT * FROM shopping_list WHERE is_checked = 1")
    suspend fun checkedRows(): List<ShoppingItemEntity>

    @Query("SELECT COUNT(*) FROM shopping_list WHERE food_id = :foodId")
    suspend fun countForFood(foodId: Long): Int

    /** All rows with ids — needed to merge quantities without losing checked state. */
    @Query("SELECT * FROM shopping_list ORDER BY created_at")
    suspend fun allRows(): List<ShoppingItemEntity>

    @Query("UPDATE shopping_list SET item = :text WHERE id = :id")
    suspend fun updateText(id: Long, text: String)
}
