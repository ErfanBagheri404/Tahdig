package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.RatingEntity
import kotlinx.coroutines.flow.Flow

/** Food rating (1-5 stars) plus the user's private note, one row per dish. */
@Dao
interface RatingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rating: RatingEntity)

    @Query("SELECT stars FROM ratings WHERE food_id = :foodId LIMIT 1")
    suspend fun getStars(foodId: Long): Int?

    @Query("SELECT * FROM ratings WHERE food_id = :foodId LIMIT 1")
    fun observe(foodId: Long): Flow<RatingEntity?>

    // -- #134 -----------------------------------------------------------------
    // Both writers touch ONE column on purpose. A read-modify-write through
    // upsert() would silently drop the note the moment the user changed a star
    // (and vice versa), which is exactly the data loss this feature must not
    // have. One statement per field makes that impossible by construction.

    /**
     * Set stars without disturbing the note.
     *
     * UPDATE first, then INSERT if nothing was there: a plain upsert would need
     * the whole row, and this path deliberately does not have it.
     */
    @Query("UPDATE ratings SET stars = :stars WHERE food_id = :foodId")
    suspend fun updateStars(foodId: Long, stars: Int): Int

    @Query("UPDATE ratings SET note = :note WHERE food_id = :foodId")
    suspend fun updateNote(foodId: Long, note: String): Int

    /** Stars for a dish with no row yet; the note column starts empty. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(rating: RatingEntity)

    @Query("SELECT * FROM ratings WHERE note != '' ORDER BY food_id")
    fun observeNoted(): Flow<List<RatingEntity>>

    /**
     * Search across notes only — the user's own words, not dish names.
     *
     * A Flow, not a suspend call, because the search pipeline combines it with
     * the food query: a suspend lookup inside `combine` would block and could
     * not be re-run when the food side emits again.
     */
    @Query("SELECT * FROM ratings WHERE note LIKE '%' || :query || '%' AND note != ''")
    fun searchNotesFlow(query: String): Flow<List<RatingEntity>>

    @Query("SELECT * FROM ratings WHERE note LIKE '%' || :query || '%' AND note != ''")
    suspend fun searchNotes(query: String): List<RatingEntity>

    @Query("SELECT COUNT(*) FROM ratings WHERE note != ''")
    suspend fun noteCount(): Int
}
