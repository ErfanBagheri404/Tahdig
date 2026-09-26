package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.MessageDigest

/** #133 — the encryption layer, tested on its own. */
class BundleCryptoTest {

    private val plain = "قورمه سبزی".repeat(64).toByteArray()
    private val pass = "گذرواژهٔ درست".toCharArray()

    @Test
    fun `a bundle survives a round trip`() {
        assertArrayEquals(plain, BundleCrypto.decrypt(BundleCrypto.encrypt(plain, pass), pass))
    }

    @Test
    fun `empty input round trips`() {
        assertArrayEquals(
            ByteArray(0), BundleCrypto.decrypt(BundleCrypto.encrypt(ByteArray(0), pass), pass),
        )
    }

    @Test
    fun `a wrong passphrase fails with a persian message`() {
        try {
            BundleCrypto.decrypt(BundleCrypto.encrypt(plain, pass), "اشتباه".toCharArray())
            fail("a wrong passphrase must not decrypt")
        } catch (e: BundleCrypto.BundleError) {
            assertTrue("not user-facing: ${e.message}", e.message!!.contains("گذرواژه"))
        }
    }

    @Test
    fun `a passphrase differing by one character fails`() {
        val almost = "گذرواژهٔ درسا".toCharArray()
        try {
            BundleCrypto.decrypt(BundleCrypto.encrypt(plain, pass), almost)
            fail("one character off must not decrypt")
        } catch (_: BundleCrypto.BundleError) {
            // expected
        }
    }

    @Test
    fun `two packs of the same data differ`() {
        // A fixed salt or IV would leak that two archives hold the same recipes.
        val a = BundleCrypto.encrypt(plain, pass)
        val b = BundleCrypto.encrypt(plain, pass)
        assertNotEquals(a.toList(), b.toList())
        assertArrayEquals(BundleCrypto.decrypt(a, pass), BundleCrypto.decrypt(b, pass))
    }

    @Test
    fun `a flipped ciphertext bit fails`() {
        val packed = BundleCrypto.encrypt(plain, pass)
        // Flip a bit in the body, past the header.
        packed[packed.size - 3] = (packed[packed.size - 3].toInt() xor 0x01).toByte()
        try {
            BundleCrypto.decrypt(packed, pass)
            fail("tampered data must not decrypt")
        } catch (_: BundleCrypto.BundleError) {
            // expected
        }
    }

    @Test
    fun `a truncated bundle fails instead of returning partial data`() {
        val packed = BundleCrypto.encrypt(plain, pass)
        val cut = packed.copyOfRange(0, packed.size - 40)
        try {
            BundleCrypto.decrypt(cut, pass)
            fail("a truncated bundle must not decrypt")
        } catch (_: BundleCrypto.BundleError) {
            // expected
        }
    }

    @Test
    fun `a foreign file is rejected as not-a-bundle`() {
        try {
            BundleCrypto.decrypt("this is a photo, not a bundle".toByteArray(), pass)
            fail("a foreign file must be rejected")
        } catch (e: BundleCrypto.BundleError) {
            assertTrue("wrong message: ${e.message}", e.message!!.contains("بستهٔ ته‌دیگ نیست"))
        }
    }

    @Test
    fun `a zero-length file is rejected, not read out of bounds`() {
        try {
            BundleCrypto.decrypt(ByteArray(0), pass)
            fail("an empty file must be rejected")
        } catch (_: BundleCrypto.BundleError) {
            // expected
        }
    }

    @Test
    fun `a future version asks for an update rather than a passphrase`() {
        val packed = BundleCrypto.encrypt(plain, pass)
        packed[3] = (BundleCrypto.VERSION_NUMBER + 1).toByte()
        try {
            BundleCrypto.decrypt(packed, pass)
            fail("a future version must be rejected")
        } catch (e: BundleCrypto.BundleError) {
            assertTrue("wrong message: ${e.message}", e.message!!.contains("به‌روز"))
        }
    }

    @Test
    fun `the header is long enough to be safe to parse`() {
        // magic(3) + version(1) + salt(16) + iv(12) + at least one tag byte
        val packed = BundleCrypto.encrypt(plain, pass)
        assertTrue("header too short", packed.size >= 3 + 1 + 16 + 12 + 16)
    }

    @Test
    fun `a passphrase is not recoverable from the bundle`() {
        val packed = BundleCrypto.encrypt(plain, pass)
        val bytes = packed.toList()
        val needle = "گذرواژه".toByteArray(Charsets.UTF_8).toList()
        val found = bytes.windowed(needle.size).any { it == needle }
        assertFalse("passphrase leaks into the bundle", found)
    }

    @Test
    fun `two different passphrases give unrelated ciphertexts`() {
        val a = BundleCrypto.encrypt(plain, pass)
        val b = BundleCrypto.encrypt(plain, "دیگر".toCharArray())
        val sameBody = a.copyOfRange(3 + 1 + 16 + 12, a.size)
            .contentEquals(b.copyOfRange(3 + 1 + 16 + 12, b.size))
        assertEquals("bodies under different passphrases must differ", false, sameBody)
    }

    @Test
    fun `large data round trips without corruption`() {
        // A real bundle with photos is megabytes; small-input tests miss size bugs.
        val big = MessageDigest.getInstance("SHA-256")
            .digest(plain).let { seed -> ByteArray(3_000_000) { seed[it % seed.size] } }
        val out = BundleCrypto.decrypt(BundleCrypto.encrypt(big, pass), pass)
        assertEquals(big.size, out.size)
        assertTrue(MessageDigest.getInstance("SHA-256").digest(big)
            .contentEquals(MessageDigest.getInstance("SHA-256").digest(out)))
    }
}
