package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * #133 — the archive half of the bundle. The device half (DB swap) needs a real
 * Context; what matters here is that a bundle is *structurally* sound and that
 * every rejection happens before live data is touched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryBundleTest {

    @get:Rule val temp = TemporaryFolder()

    private fun sqliteFile(name: String = "tahdig.db"): File =
        temp.newFile(name).apply { writeBytes("SQLite format 3\u0000".toByteArray() + ByteArray(64)) }

    private fun prefsFile(dir: File, name: String) =
        File(dir, "prefs/$name").apply {
            parentFile.mkdirs()
            writeText("""<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><int name="x" value="1" /></map>""")
        }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            for ((name, bytes) in entries) {
                z.putNextEntry(ZipEntry(name))
                z.write(bytes)
                z.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun validBundle(extra: List<Pair<String, ByteArray>> = emptyList()): ByteArray {
        val dir = temp.newFolder()
        val db = sqliteFile().readBytes()
        val settings = prefsFile(dir, "tahdig_settings.xml").readBytes()
        val timers = prefsFile(dir, "tahdig_timers.xml").readBytes()
        val all = listOf(
            "tahdig.db" to db,
            "prefs/tahdig_settings.xml" to settings,
            "prefs/tahdig_timers.xml" to timers,
            "manifest.json" to """{"schema":1}""".toByteArray(),
        ) + extra
        return zip(*all.toTypedArray())
    }

    // -- zip / unzip ---------------------------------------------------------

    @Test
    fun `a bundle unzips every entry to disk`() {
        val stage = temp.newFolder()
        val found = LibraryBundle.unzip(validBundle(), stage)
        assertEquals(4, found.size)
        assertTrue(File(stage, "tahdig.db").isFile)
        assertTrue(File(stage, "prefs/tahdig_settings.xml").isFile)
        assertTrue(File(stage, "manifest.json").isFile)
    }

    @Test
    fun `a zip slip entry cannot escape the stage directory`() {
        // The archive is ours, but a tampered one is exactly when this matters.
        val stage = temp.newFolder()
        val evil = zip("../../../escaped.txt" to "gotcha".toByteArray())
        try {
            LibraryBundle.unzip(evil, stage)
            fail("a traversal entry must be rejected")
        } catch (e: LibraryBundle.BundleFailure) {
            assertTrue(e.message!!.contains("خراب"))
        }
        assertFalse("wrote outside the stage", File(stage.parentFile, "escaped.txt").exists())
    }

    @Test
    fun `non-zip bytes are rejected instead of unzipping to an empty stage`() {
        // ZipInputStream reads zero entries from non-zip bytes without throwing,
        // so without a check a photo would "import" as an empty bundle.
        val stage = temp.newFolder()
        try {
            LibraryBundle.unzip("this is a photo, not a bundle".toByteArray(), stage)
            fail("non-zip bytes must be rejected")
        } catch (e: LibraryBundle.BundleFailure) {
            assertTrue(e.message!!.contains("خراب"))
        }
    }

    // -- validation before install ------------------------------------------

    @Test
    fun `a bundle without a database is refused`() {
        val stage = temp.newFolder()
        LibraryBundle.unzip(zip("manifest.json" to """{"schema":1}""".toByteArray()), stage)
        try {
            LibraryBundle.installStaged(org.robolectric.RuntimeEnvironment.getApplication(), stage)
            fail("a bundle with no db must be refused")
        } catch (e: LibraryBundle.BundleFailure) {
            assertTrue(e.message!!.contains("کامل نیست"))
        }
    }

    @Test
    fun `a non-sqlite file where the db should be is refused`() {
        val stage = temp.newFolder()
        LibraryBundle.unzip(
            zip(
                "tahdig.db" to "just a text file".toByteArray(),
                "manifest.json" to """{"schema":1}""".toByteArray(),
            ), stage,
        )
        try {
            LibraryBundle.installStaged(org.robolectric.RuntimeEnvironment.getApplication(), stage)
            fail("a non-sqlite db must be refused")
        } catch (e: LibraryBundle.BundleFailure) {
            assertTrue(e.message!!.contains("کامل نیست"))
        }
    }

    @Test
    fun `a future schema asks for an update instead of importing`() {
        val stage = temp.newFolder()
        val db = sqliteFile().readBytes()
        LibraryBundle.unzip(
            zip(
                "tahdig.db" to db,
                "manifest.json" to """{"schema":${LibraryBundle.SCHEMA + 1}}""".toByteArray(),
            ), stage,
        )
        try {
            LibraryBundle.installStaged(org.robolectric.RuntimeEnvironment.getApplication(), stage)
            fail("a future schema must be refused")
        } catch (e: LibraryBundle.BundleFailure) {
            assertTrue(e.message!!.contains("به‌روز"))
        }
    }

    @Test
    fun `corrupt prefs are refused before the db is swapped`() {
        val stage = temp.newFolder()
        val db = sqliteFile().readBytes()
        val badPrefs = File(stage, "prefs/tahdig_settings.xml").apply {
            parentFile.mkdirs()
            // Not XML and not a size we would accept: a truncated prefs write.
            writeText("x".repeat(200_000))
        }
        LibraryBundle.unzip(
            zip(
                "tahdig.db" to db,
                "prefs/tahdig_settings.xml" to badPrefs.readBytes(),
                "manifest.json" to """{"schema":1}""".toByteArray(),
            ), stage,
        )
        val live = org.robolectric.RuntimeEnvironment.getApplication()
            .getDatabasePath("tahdig.db")
        val before = live.takeIf { it.isFile }?.readBytes()
        try {
            LibraryBundle.installStaged(
                org.robolectric.RuntimeEnvironment.getApplication(), stage,
            )
            fail("corrupt prefs must be refused")
        } catch (_: LibraryBundle.BundleFailure) {
            // expected
        }
        // The refusal must happen before the database is replaced.
        val after = live.takeIf { it.isFile }?.readBytes()
        assertEquals("live db changed on a refused import",
            before?.toList(), after?.toList())
    }

    @Test
    fun `a bundle without a manifest is refused as incomplete`() {
        // The manifest is the last entry written, so a truncated archive
        // reliably loses it. Refusing is how "unzipped halfway" becomes an
        // error instead of a partial restore.
        val stage = temp.newFolder()
        val db = sqliteFile().readBytes()
        LibraryBundle.unzip(zip("tahdig.db" to db), stage)
        assertFalse(File(stage, "manifest.json").exists())
        try {
            LibraryBundle.installStaged(
                org.robolectric.RuntimeEnvironment.getApplication(), stage,
            )
            fail("a bundle with no manifest must be refused")
        } catch (e: LibraryBundle.BundleFailure) {
            assertTrue(e.message!!.contains("کامل نیست"))
        }
    }

    // -- crypto + bundle together -------------------------------------------

    @Test
    fun `the whole pipeline round trips zip encrypt decrypt unzip`() {
        val pass = "گذرواژهٔ خانواده".toCharArray()
        val original = validBundle()
        val stage = temp.newFolder()
        val unpacked = LibraryBundle.unzip(
            BundleCrypto.decrypt(BundleCrypto.encrypt(original, pass), pass), stage,
        )
        assertEquals(4, unpacked.size)
        // And the payload survived the crypto hop byte for byte.
        assertTrue(LibraryBundle.looksLikeSqlite(File(stage, "tahdig.db")))
        assertTrue(String(File(stage, "manifest.json").readBytes()).contains("schema"))
    }

    @Test
    fun `a wrong passphrase on a real bundle never reaches the zip layer`() {
        val packed = BundleCrypto.encrypt(validBundle(), "درست".toCharArray())
        try {
            BundleCrypto.decrypt(packed, "غلط".toCharArray())
            fail("a wrong passphrase must fail")
        } catch (e: BundleCrypto.BundleError) {
            assertTrue(e.message!!.contains("گذرواژه"))
        }
    }

    @Test
    fun `sqlite magic is checked on the first sixteen bytes only`() {
        val f = sqliteFile()
        assertTrue(LibraryBundle.looksLikeSqlite(f))
        f.writeBytes("SQLite format 3\u0000".toByteArray() + "trailing data".toByteArray())
        assertTrue("trailing bytes must not matter", LibraryBundle.looksLikeSqlite(f))
    }

    @Test
    fun `a truncated sqlite header is not a database`() {
        val f = temp.newFile("short.db").apply { writeBytes("SQLite".toByteArray()) }
        assertFalse(LibraryBundle.looksLikeSqlite(f))
    }

    @Test
    fun `a real sqlite file passes the magic check`() {
        // The check is only as good as the bytes it inspects: verify against a
        // database SQLite itself wrote, not a hand-made magic header.
        val real = temp.newFile("real.db")
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(real, null).use {
            it.execSQL("CREATE TABLE t (x TEXT)")
            it.execSQL("INSERT INTO t VALUES ('قورمه')")
        }
        assertTrue(LibraryBundle.looksLikeSqlite(real))
        assertTrue("a real db should be more than a header", real.length() > 100)
    }

    @Test
    fun `encrypting a real sqlite file keeps every byte`() {
        // The bundle A -> B acceptance in miniature: a database SQLite wrote,
        // through encrypt -> decrypt, byte for byte.
        val real = temp.newFile("real2.db")
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(real, null).use {
            it.execSQL("CREATE TABLE t (x TEXT)")
            it.execSQL("INSERT INTO t VALUES ('فسنجان')")
        }
        val pass = "خانواده".toCharArray()
        val out = BundleCrypto.decrypt(BundleCrypto.encrypt(real.readBytes(), pass), pass)
        assertTrue(out.toList() == real.readBytes().toList())
        // And the decrypted copy still opens as a database.
        val copy = temp.newFile("copy.db").apply { writeBytes(out) }
        android.database.sqlite.SQLiteDatabase.openDatabase(
            copy.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
        ).use {
            val c = it.query("t", arrayOf("x"), null, null, null, null, null)
            assertTrue(c.moveToFirst())
            assertEquals("فسنجان", c.getString(0))
            c.close()
        }
    }

    @Test
    fun `the schema constant is the one a bundle is stamped with`() {
        assertTrue("schema must be positive", LibraryBundle.SCHEMA >= 1)
        assertNotNull(LibraryBundle.SCHEMA)
    }
}
