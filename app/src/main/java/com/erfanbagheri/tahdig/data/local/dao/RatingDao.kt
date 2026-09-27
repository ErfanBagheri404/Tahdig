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
    /**
     * Set stars without disturbing the note — and stamp [now] so the #92 decay
     * knows how fresh the opinion is.
     *
     * Still one statement for the rating side: `note` is deliberately absent
     * from the SET list, which is what keeps a star tap from erasing a note.
     */
    @Query("UPDATE ratings SET stars = :stars, updated_at = :now WHERE food_id = :foodId")
    suspend fun updateStars(foodId: Long, stars: Int, now: Long): Int

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

    // -- #92 taste profile --------------------------------------------------

    /**
     * Every rating joined to its dish's category, newest first per dish.
     *
     * The join is what lets the scorer learn a *category* preference from three
     * individually rated dishes (#92's acceptance), and `rated_at` is the decay
     * input — without it every opinion would be treated as brand new.
     */
    @Query(
        """
        SELECT r.food_id AS foodId, r.stars AS stars, r.updated_at AS ratedAt,
               f.category_id AS categoryId
        FROM ratings r
        JOIN foods f ON f.id = r.food_id
        ORDER BY r.updated_at DESC
        """
    )
    suspend fun allRatings(): List<RatingSignals>
}
