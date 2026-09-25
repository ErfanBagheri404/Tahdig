package com.erfanbagheri.tahdig.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erfanbagheri.tahdig.data.local.entity.BarcodeScanEntity
import com.erfanbagheri.tahdig.util.ScanHistory
import kotlinx.coroutines.flow.Flow

/**
 * Barcode scan history + offline product cache (#116).
 *
 * The cap lives in SQL rather than in Kotlin: the ViewModel can never forget
 * to trim, because a row that is not in the newest 20 simply is not returned.
 * [ScanHistory.push] still owns the rule as a testable pure function; this
 * query is the same rule expressed where the data actually lives.
 */
@Dao
interface BarcodeScanDao {

    /**
     * The last [ScanHistory.MAX_ENTRIES] scans, newest first. `timestamp` ties
     * are broken by rowid so two scans in the same millisecond still order.
     */
    @Query(
        "SELECT * FROM barcode_scans ORDER BY timestamp DESC, rowid DESC LIMIT :limit"
    )
    fun observeRecent(limit: Int = ScanHistory.MAX_ENTRIES): Flow<List<BarcodeScanEntity>>

    /** The offline cache lookup — one row, no network. */
    @Query("SELECT * FROM barcode_scans WHERE barcode = :barcode")
    suspend fun byBarcode(barcode: String): BarcodeScanEntity?

    /**
     * Insert or refresh. REPLACE keeps one row per barcode, so re-scanning a
     * product updates its facts and its timestamp instead of duplicating.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(scan: BarcodeScanEntity)

    /**
     * Trim to the newest N. Called after each successful scan; a no-op when the
     * history has not outgrown the cap.
     */
    @Query(
        """
        DELETE FROM barcode_scans WHERE barcode NOT IN (
            SELECT barcode FROM barcode_scans
            ORDER BY timestamp DESC, rowid DESC LIMIT :keep
        )
        """
    )
    suspend fun trimTo(keep: Int = ScanHistory.MAX_ENTRIES)

    @Query("DELETE FROM barcode_scans")
    suspend fun clear()

    /**
     * Every cached row, for the clear-history undo (#127). The table is both
     * the scan history and the offline product cache — a bare DELETE makes
     * every known product look un-scanned and forces a fresh network lookup.
     */
    @Query("SELECT * FROM barcode_scans")
    suspend fun allRows(): List<BarcodeScanEntity>
}
