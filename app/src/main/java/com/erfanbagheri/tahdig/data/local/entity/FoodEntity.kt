package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "foods",
    indices = [
        Index("category_id"),
        Index("meal_time"),
        Index("cuisine"),
    ],
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Persian name — e.g. "قورمه‌سبزی" */
    val name: String,

    /** English transliteration — e.g. "Ghormeh Sabzi" */
    @ColumnInfo(name = "name_en")
    val nameEn: String,

    /** FK to categories */
    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    /**
     * Meal-time bucket(s), comma-delimited, e.g. "LUNCH,DINNER".
     * Values: BREAKFAST | LUNCH | DINNER | SNACK | LIGHT_DINNER
     * Delimited rather than a join table so one column + one index covers it.
     */
    @ColumnInfo(name = "meal_time")
    val mealTime: String,

    /** Cuisine group — IRANI | ITALIAN | CHINESE | TURKISH | ... */
    val cuisine: String,

    /** Ease of cooking */
    val difficulty: String,

    /** Preparation time in minutes */
    @ColumnInfo(name = "prep_time_min")
    val prepTimeMin: Int,

    /** Main ingredients — comma-separated for seed; JSON array in later versions */
    val ingredients: String,

    /** Comma-separated tag strings, e.g. "بدون_گوشت,وگن,بدون_گلوتن" */
    val tags: String = "",

    /** Short user-facing description of the dish */
    val description: String = "",

    /** Image URL (remote). Null = no image yet. */
    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    /** True if this food is disabled / hidden by the user (soft-delete) */
    @ColumnInfo(name = "is_blocked")
    val isBlocked: Boolean = false,

    /**
     * Comma-separated equipment labels («قابلمه، فر»), baked into seed for known
     * dishes; empty falls back to keyword inference at render time (#100).
     */
    val equipment: String = "",

    /** Sort priority (lower = more likely to be picked) */
    val priority: Int = 0,
)
