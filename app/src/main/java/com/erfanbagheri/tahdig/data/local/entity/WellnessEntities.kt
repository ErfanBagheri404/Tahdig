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

/**
 * One caffeine hit (#119) — an append-only log, NOT a daily total: three
 * coffees are three rows, so the history chart and the undo both work.
 * A daily sum would make a 20 mg custom entry impossible to remove.
 */
@Entity(tableName = "caffeine_log")
data class CaffeineLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Local date as epochDay — the key the accumulation buckets on. */
    val epochDay: Long,
    /** Milligrams for this hit; 0 is allowed (a decaf tap still happened). */
    val mg: Int,
    /** Chip label («چای») or «سفارشی» for a typed amount. */
    val label: String,
    val loggedAt: Long = System.currentTimeMillis(),
)
