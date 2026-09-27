package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A saved filter+sort preset (#87).
 *
 * [payload] is a serialized [com.erfanbagheri.tahdig.util.FilterPresetPayload] rather
 * than columns: the filter surface grows (a new axis must not need a DB migration),
 * and nothing ever queries inside a preset — it is read whole and applied whole.
 */
@Entity(
    tableName = "filter_presets",
    indices = [Index("name", unique = true)],
)
data class FilterPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Farsi name the user typed; unique so re-saving under the same name overwrites. */
    val name: String,
    val payload: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
