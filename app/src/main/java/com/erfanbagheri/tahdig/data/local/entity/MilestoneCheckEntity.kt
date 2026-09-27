package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One checked mise-en-place row, keyed by (foodId, ingredientHash).
 * Hash is quantity-free (see [com.erfanbagheri.tahdig.util.MisePlace.hashOf]), so
 * checked state survives serving scaling and app restarts.
 */
@Entity(
    tableName = "milestone_checks",
    indices = [Index(value = ["foodId", "ingredientHash"], unique = true)],
)
data class MilestoneCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodId: Long,
    val ingredientHash: String,
    val checkedAt: Long = System.currentTimeMillis(),
)
