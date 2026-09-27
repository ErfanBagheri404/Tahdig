package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodTagJoin
import com.erfanbagheri.tahdig.data.local.entity.TagEntity
import com.erfanbagheri.tahdig.util.PersianText
import kotlinx.coroutines.flow.Flow

/** User tags (#80) — free-form Farsi labels over dishes, AND-filtered in search. */
@Dao
interface TagDao {
    /** IGNORE so a duplicate normalized name returns -1 instead of throwing. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM user_tags WHERE name = :name LIMIT 1")
    suspend fun byName(name: String): TagEntity?

    @Query("SELECT * FROM user_tags ORDER BY name")
    fun observeAll(): Flow<List<TagEntity>>

    @Query(
        """
        SELECT t.* FROM user_tags t
        JOIN food_tags j ON j.tag_id = t.id
        WHERE j.food_id = :foodId ORDER BY t.name
        """
    )
    fun observeForFood(foodId: Long): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun assign(join: FoodTagJoin)

    @Query("DELETE FROM food_tags WHERE food_id = :foodId AND tag_id = :tagId")
    suspend fun unassign(foodId: Long, tagId: Long)

    /** Drops joins via CASCADE; dishes are untouched. */
    @Query("DELETE FROM user_tags WHERE id = :tagId")
    suspend fun delete(tagId: Long)

    @Query("UPDATE user_tags SET name = :name WHERE id = :tagId")
    suspend fun rename(tagId: Long, name: String)

    @Query("DELETE FROM food_tags WHERE tag_id = :from AND food_id IN (SELECT food_id FROM food_tags WHERE tag_id = :to)")
    suspend fun dropOverlappingJoins(from: Long, to: Long)

    @Query("UPDATE food_tags SET tag_id = :to WHERE tag_id = :from")
    suspend fun repointJoins(from: Long, to: Long)

    /**
     * Rename onto an existing spelling: drop the pairs the target already has
     * (the join PK is (food_id, tag_id)), move the rest, then drop the source.
     */
    @Transaction
    suspend fun merge(from: Long, to: Long) {
        dropOverlappingJoins(from, to)
        repointJoins(from, to)
        delete(from)
    }

    @Query("SELECT COUNT(*) FROM user_tags")
    suspend fun count(): Int

    /**
     * Dishes carrying EVERY listed tag. HAVING COUNT = n is the AND — a plain
     * `IN` would OR them.
     */
    @Query(
        """
        SELECT f.* FROM foods f
        JOIN food_tags j ON j.food_id = f.id
        WHERE j.tag_id IN (:tagIds) AND f.is_blocked = 0
        GROUP BY f.id HAVING COUNT(DISTINCT j.tag_id) = :n
        ORDER BY f.name
        """
    )
    fun foodsWithAllTags(tagIds: List<Long>, n: Int): Flow<List<FoodEntity>>

    @Query(
        """
        SELECT f.* FROM foods f
        JOIN food_tags j ON j.food_id = f.id
        WHERE j.tag_id IN (:tagIds) AND f.is_blocked = 0
        GROUP BY f.id HAVING COUNT(DISTINCT j.tag_id) = :n
        ORDER BY f.name
        """
    )
    suspend fun foodsWithAllTagsOnce(tagIds: List<Long>, n: Int): List<FoodEntity>

    /**
     * Normalized get-or-create. Returns the existing row on any spelling that
     * normalizes the same way («سریع» ± ZWNJ); the UNIQUE index + re-read make
     * concurrent duplicates impossible, not just unlikely.
     */
    @Transaction
    suspend fun getOrCreate(raw: String): Long {
        val name = PersianText.normalize(raw)
        require(name.isNotEmpty()) { "empty tag" }
        byName(name)?.let { return it.id }
        val id = insertTag(TagEntity(name = name))
        if (id != -1L) return id
        return byName(name)!!.id
    }
}
