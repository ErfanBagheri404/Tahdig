package com.erfanbagheri.tahdig.util

import android.content.Context
import android.net.Uri
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import java.io.File
import java.io.FileOutputStream

/**
 * Whole-file backup/restore of the Room DB via SAF.
 * ponytail: copies the DB file rather than using `VACUUM INTO` — fine at this size
 * (543 seed rows + a few user tables). Switch to `VACUUM INTO` if the DB grows large.
 */
object BackupRestore {
    private const val DB_NAME = "tahdig.db"

    /** SQLite file magic — used to reject non-DB files before touching live data. */
    private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    /**
     * Write a consistent copy of the DB to [uri].
     * The WAL is checkpointed first: in WAL mode the newest writes live in `-wal`, so
     * copying only the main file would silently drop recent favorites/ratings/history.
     */
    fun backup(context: Context, uri: Uri) {
        // Flush the WAL into the main DB file so the copy is complete.
        runCatching {
            TahdigDatabase.getInstance(context).openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)")
                .use { it.moveToFirst() }
        }

        val dbFile = context.getDatabasePath(DB_NAME)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            dbFile.inputStream().use { it.copyTo(out) }
        } ?: throw IllegalStateException("Could not open backup destination")
    }

    /**
     * Replace the live DB with the contents of [uri].
     * The incoming file is validated into a temp file first, so a bad pick (empty file,
     * wrong file type) leaves the existing DB untouched instead of destroying it.
     *
     * @return true when the DB was replaced; false when [uri] is not a usable SQLite file.
     */
    fun restore(context: Context, uri: Uri): Boolean {
        val dbFile = context.getDatabasePath(DB_NAME)
        val staged = File(dbFile.parentFile, "$DB_NAME.restore-tmp")

        // Stage + validate before touching the live database.
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(staged).use { out -> input.copyTo(out) }
            } ?: return false
        } catch (e: Exception) {
            staged.delete()
            return false
        }

        if (!looksLikeSqlite(staged)) {
            staged.delete()
            return false
        }

        TahdigDatabase.closeInstance()
        listOf(dbFile, dbFile.resolveSibling("$DB_NAME-wal"), dbFile.resolveSibling("$DB_NAME-shm"))
            .forEach { it.delete() }

        return if (staged.renameTo(dbFile)) {
            true
        } else {
            staged.delete()
            false
        }
    }

    /** Cheap sanity check: SQLite files start with a known 16-byte magic string. */
    private fun looksLikeSqlite(file: File): Boolean {
        if (!file.isFile || file.length() < SQLITE_MAGIC.size) return false
        val head = ByteArray(SQLITE_MAGIC.size)
        return file.inputStream().use { it.read(head) == head.size } &&
            head.contentEquals(SQLITE_MAGIC)
    }
}
