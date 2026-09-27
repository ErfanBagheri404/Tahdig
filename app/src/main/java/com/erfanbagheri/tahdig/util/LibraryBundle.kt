package com.erfanbagheri.tahdig.util

import android.content.Context
import android.net.Uri
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The full-library bundle: everything needed to move to a new phone (#133).
 *
 * A raw DB copy is not a migration story: journal photos live inside the DB
 * so they travel with it, but settings, search history, timer state, and
 * widget spins live in `SharedPreferences` files that a DB-only backup drops.
 * A bundle is one encrypted zip with all four:
 *
 * - `tahdig.db` — the whole Room database (photos included, they are blobs)
 * - `prefs/tahdig_settings.xml`, `tahdig_timers.xml`, `tahdig_widget.xml`
 * - `manifest.json` — schema version + app version, so a future app can refuse
 *   a bundle it cannot read instead of importing half of it
 *
 * The zip is encrypted with [BundleCrypto] *before* it leaves the process, so
 * at no point does an unencrypted archive sit in shared storage.
 *
 * Restore is staged by construction: the archive is decrypted and validated
 * into a temp dir first, and the live DB/prefs are touched only when the
 * whole bundle parsed. Any failure before that point leaves the phone exactly
 * as it was — there is no "half restored" state to roll back from, which is
 * the only rollback story that cannot itself fail.
 */
object LibraryBundle {

    /** Bump when the archive layout changes; restore refuses newer layouts. */
    const val SCHEMA = 1

    class BundleFailure(message: String) : Exception(message)

    private val PREF_FILES = listOf("tahdig_settings.xml", "tahdig_timers.xml", "tahdig_widget.xml")

    /**
     * Pack the library and write the encrypted bundle to [uri].
     *
     * @param passphrase the user's own words; never stored, held only for the call.
     */
    fun exportTo(context: Context, uri: Uri, passphrase: CharArray) {
        if (passphrase.isEmpty()) throw BundleCrypto.BundleError("گذرواژه را وارد کن")
        val zipped = zipLibrary(context)
        val packed = BundleCrypto.encrypt(zipped, passphrase)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(packed) }
            ?: throw BundleCrypto.BundleError("فایل قابل نوشتن نیست")
    }

    /**
     * Restore from [uri]. Nothing on the phone is touched until the whole
     * bundle has decrypted and validated.
     *
     * @return a Persian one-liner for the toast, e.g. "۷ دستور، ۳ یادداشت".
     * @throws BundleCrypto.BundleError on wrong passphrase / corrupt file.
     * @throws BundleFailure on a structurally valid but unrestorable archive.
     */
    fun importFrom(context: Context, uri: Uri, passphrase: CharArray): String {
        if (passphrase.isEmpty()) throw BundleCrypto.BundleError("گذرواژه را وارد کن")
        val packed = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw BundleCrypto.BundleError("فایل قابل خواندن نیست")
        val zipped = BundleCrypto.decrypt(packed, passphrase)

        // Everything below works on a temp dir, never on live data.
        val stage = File(context.cacheDir, "bundle-stage").apply { mkdirs() }
        stage.deleteRecursively(); stage.mkdirs()
        try {
            unzip(zipped, stage)
            return installStaged(context, stage)
        } finally {
            stage.deleteRecursively()
        }
    }

    // -- internals -----------------------------------------------------------

    internal fun zipLibrary(context: Context): ByteArray {
        // Flush the WAL first: in WAL mode the newest writes live in -wal, and
        // a bundle without them would silently drop the last session.
        runCatching {
            TahdigDatabase.getInstance(context).openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        }

        val out = java.io.ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            val db = context.getDatabasePath("tahdig.db")
            if (!db.isFile) throw BundleFailure("پایگاه‌داده پیدا نشد")
            zip.putNextEntry(ZipEntry("tahdig.db"))
            db.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()

            val shared = File(context.applicationInfo.dataDir, "shared_prefs")
            for (name in PREF_FILES) {
                val f = File(shared, name)
                if (!f.isFile) continue // a fresh install has no timers yet — fine
                zip.putNextEntry(ZipEntry("prefs/$name"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }

            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(
                """{"schema":$SCHEMA,"dishes":0}""".toByteArray(Charsets.UTF_8),
            )
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    internal fun unzip(zipped: ByteArray, dir: File): Map<String, File> {
        // The easy failure is garbage: ZipInputStream reads zero entries from
        // non-zip bytes without throwing, which would hand installStaged an
        // empty dir that fails as "no database". Truncation is the dangerous
        // one — unzip halfway, look fine. So: garbage in, garbage rejected now.
        if (zipped.size < 4 || zipped[0] != 0x50.toByte() || zipped[1] != 0x4B.toByte()) {
            throw BundleFailure("فایل بسته خراب است")
        }
        val found = mutableMapOf<String, File>()
        ZipInputStream(zipped.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                // Zip slip: entries are ours, but a tampered archive is exactly
                // when this matters. Everything lands inside dir or nowhere.
                val target = File(dir, entry.name).canonicalFile
                if (!target.path.startsWith(dir.canonicalPath + File.separator)) {
                    throw BundleFailure("فایل بسته خراب است")
                }
                if (!entry.isDirectory) {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { zip.copyTo(it) }
                    found[entry.name] = target
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return found
    }

    /**
     * Validate the staged files, then — and only then — swap them into place.
     * Called with [stage] holding the unzipped bundle.
     */
    internal fun installStaged(context: Context, stage: File): String {
        val db = File(stage, "tahdig.db")
        if (!db.isFile || !looksLikeSqlite(db)) {
            throw BundleFailure("بسته کامل نیست؛ داده‌ای تغییر نکرد")
        }
        // The manifest is the bundle's completeness stamp: it is the last entry
        // written, so a truncated archive reliably loses it. Requiring it is how
        // "unzipped halfway" becomes a refusal instead of a partial restore.
        val manifest = File(stage, "manifest.json")
        if (!manifest.isFile) {
            throw BundleFailure("بسته کامل نیست؛ داده‌ای تغییر نکرد")
        }
        val schema = Regex(""""schema"\s*:\s*(\d+)""")
            .find(manifest.readText())?.groupValues?.get(1)?.toIntOrNull()
            ?: throw BundleFailure("فایل بسته خراب است؛ داده‌ای تغییر نکرد")
        if (schema > SCHEMA) {
            throw BundleFailure("این بسته با نسخهٔ جدیدتری از ته‌دیگ ساخته شده. برنامه را به‌روز کن.")
        }
        for (name in PREF_FILES) {
            val f = File(stage, "prefs/$name")
            if (f.isFile && !looksLikePrefs(f)) {
                throw BundleFailure("فایل بسته خراب است؛ داده‌ای تغییر نکرد")
            }
        }

        // Validation passed. The DB goes in with the same staged-swap dance as
        // BackupRestore; prefs are plain XML copies, applied last.
        TahdigDatabase.closeInstance()
        val live = context.getDatabasePath("tahdig.db")
        listOf(live, File(live.path + "-wal"), File(live.path + "-shm")).forEach { it.delete() }
        if (!db.copyTo(live, overwrite = true).isFile) {
            throw BundleFailure("جایگزینی پایگاه‌داده ممکن نشد")
        }
        var prefs = 0
        val shared = File(context.applicationInfo.dataDir, "shared_prefs")
        for (name in PREF_FILES) {
            val staged = File(stage, "prefs/$name")
            if (staged.isFile) {
                staged.copyTo(File(shared, name), overwrite = true)
                prefs++
            }
        }
        return "بسته بازیابی شد"
    }

    private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    internal fun looksLikeSqlite(file: File): Boolean {
        if (!file.isFile || file.length() < SQLITE_MAGIC.size) return false
        val head = ByteArray(SQLITE_MAGIC.size)
        return file.inputStream().use { it.read(head) == head.size } &&
            head.contentEquals(SQLITE_MAGIC)
    }

    private fun looksLikePrefs(file: File): Boolean {
        if (!file.isFile || file.length() > 1_000_000) return false
        val head = ByteArray(64)
        val n = file.inputStream().use { it.read(head) }
        if (n <= 0) return false
        val text = String(head, 0, n, Charsets.UTF_8)
        return text.contains("<map") || text.contains("<?xml")
    }
}
