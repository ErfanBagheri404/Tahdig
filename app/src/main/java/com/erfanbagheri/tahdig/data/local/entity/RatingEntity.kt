package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per dish: how many stars (1-5) the user assigned, and — since #134 —
 * their own private note about it.
 *
 * The note rides on this table rather than in one of its own: a review is
 * worthless without its rating, and the two are written and deleted together.
 * One row means a half-written review cannot exist.
 *
 * @property note Farsi text; "" = never written. Not nullable, so the column
 *   stays simple and `isBlank()` is the only test needed anywhere.
 */
@Entity(
    tableName = "ratings",
    indices = [Index("food_id", unique = true)],
)
data class RatingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "food_id") val foodId: Long,
    /** 1-5 stars; 0 = no rating. */
    val stars: Int,
    /** #134 — the user's private «یادداشت من». """ when never written. */
    val note: String = "",
    /**
     * #92 — when the stars were last set, epoch ms. Drives the scorer's decay:
     * an opinion from three months ago should count for less than one from
     * yesterday, and without a timestamp there is no way to tell them apart.
     *
     * 0 = never rated (or written before this column existed), which the
     * scorer treats as fresh rather than ancient — a missing timestamp must
     * not silently erase a real opinion.
     */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = 0L,
) {
    companion object {
        /**
         * Ceiling for a stored note. 600 Persian characters is a generous
         * paragraph; past that it stops being a note and becomes a journal
         * entry, which #124 already covers.
         */
        const val MAX_NOTE_CHARS = 600
    }
}
