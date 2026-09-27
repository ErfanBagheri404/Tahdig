package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * #80 — the user's own vocabulary over dishes («سریع»، «مهمونی»).
 *
 * [name] is stored already [com.erfanbagheri.tahdig.util.PersianText.normalize]-d
 * so «سریع» with and without ZWNJ is one row. The UNIQUE index is the real
 * guarantee — a Kotlin check alone would race; [dao.getOrCreate] re-reads on
 * conflict for the same reason.
 */
@Entity(
    tableName = "user_tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

/** Dish ↔ tag join. Deleting either side cascades the join only, never a dish. */
@Entity(
    tableName = "food_tags",
    primaryKeys = ["food_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tag_id")],
)
data class FoodTagJoin(
    @ColumnInfo(name = "food_id") val foodId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)
