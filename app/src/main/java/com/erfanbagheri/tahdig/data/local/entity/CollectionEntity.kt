package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * #79 — a named group of saved dishes («عید»، «ناهار سریع»، «رژیمی»).
 *
 * Favorites stay a flat flag on `favorites`; a collection is an ADDITIONAL
 * label, so a dish can be in several of them and in none.
 */
@Entity(
    tableName = "collections",
    indices = [Index(value = ["name"], unique = true)],
)
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0,
)

/**
 * #79 — the join. A dish may sit in several collections, and deleting either
 * side must clean up the join only: CASCADE on BOTH keys means a deleted dish
 * drops its joins and a deleted collection drops its joins, and neither touches
 * the other table. The composite primary key already forbids a double-add;
 * the UNIQUE index makes that explicit for the query planner.
 */
@Entity(
    tableName = "collection_food",
    primaryKeys = ["collection_id", "food_id"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["collection_id", "food_id"], unique = true)],
)
data class CollectionFoodEntity(
    @ColumnInfo(name = "collection_id") val collectionId: Long,
    @ColumnInfo(name = "food_id") val foodId: Long,
)

/** #79 — a collection plus how many dishes it holds. */
data class CollectionCount(
    val id: Long,
    val name: String,
    val count: Int,
)
