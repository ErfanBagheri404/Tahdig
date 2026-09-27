package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.CollectionCount
import com.erfanbagheri.tahdig.data.local.entity.CollectionEntity
import com.erfanbagheri.tahdig.data.local.entity.CollectionFoodEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

/** #79 — named groups of saved dishes. */
@Dao
interface CollectionDao {

    // -- collections ---------------------------------------------------------

    /**
     * IGNORE, not REPLACE: the name carries a UNIQUE index, so re-typing a name
     * the user already has must keep the existing collection (and its dishes)
     * instead of silently overwriting it with a brand-new empty id.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(collection: CollectionEntity): Long

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String): Int

    /** #79 — deletes the joins by CASCADE, never the dishes. */
    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM collections ORDER BY created_at, name")
    suspend fun all(): List<CollectionEntity>

    @Query("SELECT * FROM collections ORDER BY created_at, name")
    fun observeAll(): Flow<List<CollectionEntity>>

    @Query("SELECT name FROM collections WHERE name = :name LIMIT 1")
    suspend fun nameExists(name: String): String?

    /** Every collection with its dish count — the manage screen's row model. */
    @Query(
        """
        SELECT c.id AS id, c.name AS name, COUNT(cf.food_id) AS count
        FROM collections c
        LEFT JOIN collection_food cf ON cf.collection_id = c.id
        GROUP BY c.id
        ORDER BY c.created_at, c.name
        """
    )
    fun observeCounts(): Flow<List<CollectionCount>>

    // -- joins ---------------------------------------------------------------

    @Query(
        """
        INSERT OR IGNORE INTO collection_food (collection_id, food_id)
        VALUES (:collectionId, :foodId)
        """
    )
    suspend fun addFood(collectionId: Long, foodId: Long)

    @Query(
        "DELETE FROM collection_food WHERE collection_id = :collectionId AND food_id = :foodId"
    )
    suspend fun removeFood(collectionId: Long, foodId: Long)

    /** Toggles one join and reports the membership it ended on. */
    @Query(
        "SELECT COUNT(*) FROM collection_food WHERE collection_id = :collectionId AND food_id = :foodId"
    )
    suspend fun contains(collectionId: Long, foodId: Long): Int

    /** Ids of every collection holding this dish — the sheet's checkmarks. */
    @Query("SELECT collection_id FROM collection_food WHERE food_id = :foodId")
    fun observeCollectionIdsForFood(foodId: Long): Flow<List<Long>>

    @Query("SELECT food_id FROM collection_food WHERE collection_id = :collectionId")
    suspend fun foodIdsIn(collectionId: Long): List<Long>

    @Query("SELECT food_id FROM collection_food WHERE collection_id = :collectionId")
    fun observeFoodIdsIn(collectionId: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM collection_food WHERE collection_id = :collectionId")
    suspend fun countIn(collectionId: Long): Int

    // -- the filter ----------------------------------------------------------

    /** #79 — the dishes of one collection, joined to their food rows. */
    @Query(
        """
        SELECT f.* FROM foods f
        INNER JOIN collection_food cf ON f.id = cf.food_id
        WHERE cf.collection_id = :collectionId
        ORDER BY f.name
        """
    )
    fun observeFoods(collectionId: Long): Flow<List<FoodEntity>>
}
