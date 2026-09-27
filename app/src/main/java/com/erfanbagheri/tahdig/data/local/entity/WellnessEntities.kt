package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per local date. Water accumulates in [ml]; weight is written once
 * per day. Two tables rather than one because their shapes differ — water
 * is a running total with no weight, weight is a scalar with no accumulator
 * — and a nullable pair of columns would let a missing reading read as
 * either zero, which is exactly the "absent is not zero" trap.
 */
@Entity(tableName = "water_log")
data class WaterLogEntity(
    /** Local date as epochDay — the key the midnight reset buckets on. */
    @PrimaryKey val epochDay: Long,
    val ml: Int,
)

@Entity(tableName = "weight_log")
data class WeightLogEntity(
    /** Local date as epochDay: one weight per day, re-logging overwrites. */
    @PrimaryKey val epochDay: Long,
    val kg: Double,
)
