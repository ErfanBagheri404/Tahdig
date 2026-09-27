package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    /** Total cooks ever — the #122 primer's "first action" gate. */
    @Query("SELECT COUNT(*) FROM journal")
    fun observeCount(): Flow<Int>

    /** Timestamps of cooks that carry a note — the «نویسنده» badge (#121). */
    @Query("SELECT timestamp FROM journal WHERE note != ''")
    suspend fun notedTimestamps(): List<Long>

    /** Timestamps of cooks that carry a photo — photo challenge + badges (#125). */
    @Query("SELECT timestamp FROM journal WHERE photo IS NOT NULL")
    suspend fun photoTimestamps(): List<Long>

    /** Continuous stream of photo timestamps — for the journal's weekly counter. */
    @Query("SELECT timestamp FROM journal WHERE photo IS NOT NULL ORDER BY timestamp")
    fun observePhotoTimestamps(): Flow<List<Long>>

    @Insert
    suspend fun insert(entry: JournalEntity): Long

    /**
     * Exact-restore insert for undo (#127). REPLACE, not IGNORE: the row's own
     * id, timestamp and photo must come back, or an undone delete reorders the
     * timeline and loses the picture.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restore(entry: JournalEntity)

    /** Capture a row before deleting it, so undo can put it back verbatim. */
    @Query("SELECT * FROM journal WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): JournalEntity?

    @Query("UPDATE journal SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)

    @Query("UPDATE journal SET photo = :photo WHERE id = :id")
    suspend fun updatePhoto(id: Long, photo: ByteArray?)

    @Query("DELETE FROM journal WHERE id = :id")
    suspend fun delete(id: Long)
}
