package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Persian name — e.g. "خورش" */
    val name: String,

    /** English name — e.g. "Stew" */
    @ColumnInfo(name = "name_en")
    val nameEn: String,

    /** Display sort order within UI */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    /** Emoji for visual representation */
    val emoji: String = "",
)
