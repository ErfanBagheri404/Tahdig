package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.JournalEntity
import kotlinx.coroutines.flow.Flow

/** Cooking journal (#124): one row per cook, photo and note optional. */
@Dao
interface JournalDao {

    /** Timeline, newest first — the journal view's source. */
    @Query("SELECT * FROM journal ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<JournalEntity>>

    /**
     * Journal rows for one dish, newest first — the «۳ بار پختی» stack on the
     * history detail. Photos are excluded: this list only counts and shows
     * notes, and pulling blobs for every instance would be wasteful.
     */
    @Query(
        """
        SELECT id, food_id, timestamp, note, NULL AS photo
        FROM journal WHERE food_id = :foodId ORDER BY timestamp DESC
        """
    )
    fun observeForFood(foodId: Long): Flow<List<JournalEntity>>

    @Query("SELECT COUNT(*) FROM journal WHERE food_id = :foodId")
    fun observeCountForFood(foodId: Long): Flow<Int>

    /** Timestamps of cooks that carry a note — the «نویسنده» badge (#121). */
    @Query("SELECT timestamp FROM journal WHERE note != ''")
    suspend fun notedTimestamps(): List<Long>

    @Insert
    suspend fun insert(entry: JournalEntity): Long

    @Query("UPDATE journal SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)

    @Query("UPDATE journal SET photo = :photo WHERE id = :id")
    suspend fun updatePhoto(id: Long, photo: ByteArray?)

    @Query("DELETE FROM journal WHERE id = :id")
    suspend fun delete(id: Long)
}
