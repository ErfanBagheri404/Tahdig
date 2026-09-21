package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One pantry staple the user keeps on hand (برنج، پیاز، روغن…).
 *
 * Deliberately a plain string table, not normalized against a fixed list —
 * the user types what they actually have, Farsi or otherwise.
 */
@Entity(
    tableName = "pantry",
    indices = [Index(value = ["item"], unique = true)],
)
data class PantryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Ingredient name, e.g. "برنج" — unique, trimmed, normalized at insert. */
    val item: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
