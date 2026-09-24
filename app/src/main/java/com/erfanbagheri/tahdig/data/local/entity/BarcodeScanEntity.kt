package com.erfanbagheri.tahdig.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One scanned barcode plus the OFF product it resolved to (#116).
 *
 * ONE table serves both roles the issue asks for. The 20-row history is just
 * "the last 20 scans", and the offline cache is "the products we have already
 * fetched" — the same rows, ordered newest first. A second table would have
 * meant keeping two copies of the same list in sync with no extra capability.
 *
 * The product's nutrients are stored as nullable columns with the SAME meaning
 * as everywhere else in this app: null is "OFF never assayed it", which is not
 * the same claim as zero.
 */
@Entity(tableName = "barcode_scans")
data class BarcodeScanEntity(
    /** The barcode itself is the key — one row per product, re-scanned = refreshed. */
    @PrimaryKey
    val barcode: String,

    val name: String,

    val brands: String = "",

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    @ColumnInfo(name = "kcal_100g")
    val kcal100g: Double? = null,

    @ColumnInfo(name = "protein_100g")
    val protein100g: Double? = null,

    @ColumnInfo(name = "carb_100g")
    val carb100g: Double? = null,

    @ColumnInfo(name = "fat_100g")
    val fat100g: Double? = null,

    @ColumnInfo(name = "salt_100g")
    val salt100g: Double? = null,

    @ColumnInfo(name = "sugars_100g")
    val sugars100g: Double? = null,

    /** Comma-joined OFF allergen tags, deduped at parse time. */
    val allergens: String = "",

    @ColumnInfo(name = "nutriscore")
    val nutriscore: String? = null,

    /** When this was last scanned — drives the newest-first history order. */
    val timestamp: Long,
)
