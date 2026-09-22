package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.FilterPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterPresetDao {

    @Query("SELECT * FROM filter_presets ORDER BY created_at")
    fun observeAll(): Flow<List<FilterPresetEntity>>

    @Query("SELECT COUNT(*) FROM filter_presets")
    suspend fun count(): Int

    /** Re-saving under an existing name replaces that preset (unique name index). */
    @Query(
        """
        INSERT OR REPLACE INTO filter_presets (id, name, payload, created_at)
        VALUES (
            COALESCE((SELECT id FROM filter_presets WHERE name = :name), 0),
            :name, :payload, :at
        )
        """
    )
    suspend fun save(name: String, payload: String, at: Long = System.currentTimeMillis())

    @Query("UPDATE filter_presets SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM filter_presets WHERE id = :id")
    suspend fun delete(id: Long)
}
