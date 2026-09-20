package com.erfanbagheri.tahdig.util

import android.content.Context
import android.net.Uri
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import java.io.FileOutputStream

/**
 * Whole-file backup/restore of the Room DB via SAF.
 * ponytail: copies the main DB file only (no WAL) — fine for a read-mostly seeded DB.
 * Switch to `VACUUM INTO` if write-heavy tables ever need a consistent snapshot.
 */
object BackupRestore {
    private const val DB_NAME = "tahdig.db"

    fun backup(context: Context, uri: Uri) {
        val dbFile = context.getDatabasePath(DB_NAME)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            dbFile.inputStream().use { it.copyTo(out) }
        }
    }

    /** Close the live DB, overwrite the file, then force a restart so Room re-opens it. */
    fun restore(context: Context, uri: Uri) {
        TahdigDatabase.closeInstance()
        val dbFile = context.getDatabasePath(DB_NAME)
        listOf(dbFile, dbFile.resolveSibling("$DB_NAME-wal"), dbFile.resolveSibling("$DB_NAME-shm"))
            .forEach { it.delete() }

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dbFile).use { out -> input.copyTo(out) }
        }
    }
}
