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

    /** When this item was stocked — the fall-back anchor for default shelf life (#106). */
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),

    /** Optional expiry epoch ms (#106); null = undated (most legacy rows). */
    @ColumnInfo(name = "expires_at")
    val expiresAt: Long? = null,

    /**
     * Tap-count stock (#109). The issue's "existing entity fields" premise was
     * wrong — no quantity existed — so this Int is the whole model: staples are
     * countable, and 0 is a valid shown state (the row stays removable by X).
     */
    val quantity: Int = 1,
)
