package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One in-flight cook-along session (#93).
 *
 * Keyed by foodId — reopening the SAME dish offers «ادامه بده»; a different dish
 * has its own (or none). Written only on state changes (step move / pause /
 * resume), never per timer tick: [com.erfanbagheri.tahdig.util.CookSessionMath]
 * reconstructs the live countdown from [updatedAt].
 */
@Entity(tableName = "cook_sessions")
data class CookSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "food_id")
    val foodId: Long,
    @ColumnInfo(name = "step_index") val stepIndex: Int,
    /** Milliseconds left on this step's countdown at [updatedAt]. */
    @ColumnInfo(name = "remaining_ms") val remainingMs: Long,
    /** True = the countdown was stopped; restore does not advance it. */
    val paused: Boolean,
    /** Wall-clock of the write — restore subtracts the dead time. */
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
