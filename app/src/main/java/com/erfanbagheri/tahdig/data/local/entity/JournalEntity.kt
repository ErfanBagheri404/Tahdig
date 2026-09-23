package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cooking journal entry (#124) — the optional photo/note a user attaches to a
 * cook. One row per cook, so a dish cooked three times keeps three memories.
 *
 * The photo lives IN the row as a JPEG blob rather than a file on disk:
 * [com.erfanbagheri.tahdig.util.BackupRestore] copies the DB wholesale, so
 * blobs are backed up and restored for free — a file-based design would need
 * its own archive format and path-traversal guard for no gain at this size.
 * The image is downscaled before it is ever stored (see `JournalPhoto`).
 */
@Entity(tableName = "journal")
data class JournalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "food_id")
    val foodId: Long,

    /** Mirrors `history.timestamp` for the same cook — the join key. */
    val timestamp: Long,

    /** Optional one-line memory. Empty = none written, never a placeholder. */
    val note: String = "",

    /** Optional downscaled JPEG. Null = no photo attached. */
    @ColumnInfo(name = "photo")
    val photo: ByteArray? = null,
) {
    // ByteArray gives identity equals/hashCode; Room needs the value semantics
    // for the entity to behave in lists and state holders.
    override fun equals(other: Any?): Boolean =
        this === other || (other is JournalEntity &&
            id == other.id &&
            foodId == other.foodId &&
            timestamp == other.timestamp &&
            note == other.note &&
            java.util.Arrays.equals(photo, other.photo))

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + foodId.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + note.hashCode()
        result = 31 * result + (photo?.contentHashCode() ?: 0)
        return result
    }
}
