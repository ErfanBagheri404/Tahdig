package com.erfanbagheri.tahdig.util

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Passphrase encryption for the full-library bundle (#133).
 *
 * No existing crypto in the app to reuse, so this is the whole of it: one
 * object, AES/GCM with a PBKDF2-derived key. The key is never stored anywhere
 * — it is derived from the user's passphrase at pack/unpack time and dropped.
 *
 * Parameter choices, all standard:
 * - AES-256/GCM: authenticated encryption, so a wrong passphrase or a
 *   tampered/truncated archive fails in [decrypt], never in parsing.
 * - PBKDF2-HMAC-SHA256, 210k iterations, 16-byte salt: enough to make a weak
 *   passphrase expensive without making a phone wait. ([OWASP 2023] says
 *   600k for servers; a phone unlocking a one-off archive can afford less —
 *   and this layer exists to stop a casual reader, not a GPU farm.)
 * - 12-byte random IV per pack, prepended to the output with the salt.
 *
 * Wire format: `TG1` magic + version byte + salt(16) + iv(12) + ciphertext.
 * The magic is checked before decryption, so a user picking a random photo
 * gets "this is not a bundle" instead of "wrong passphrase".
 */
object BundleCrypto {

    const val EXTENSION = "tahdig-bundle"

    /** Human-readable Persian errors — never crypto jargon at the UI. */
    class BundleError(message: String) : Exception(message)

    private const val MAGIC = "TG1"
    private const val VERSION: Byte = 1
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128

    /** Bump when the wire format changes; the guard accepts older bundles. */
    const val VERSION_NUMBER = VERSION

    /** 210k iterations ≈ under a second on a mid-range phone (measured below in test). */
    private const val ITERATIONS = 210_000

    private val random = SecureRandom()

    fun encrypt(plain: ByteArray, passphrase: CharArray): ByteArray {
        require(passphrase.isNotEmpty()) { "empty passphrase" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
        }
        val body = cipher.doFinal(plain)
        return MAGIC.toByteArray(Charsets.US_ASCII) + byteArrayOf(VERSION) + salt + iv + body
    }

    /**
     * @throws BundleError with a user-facing Persian message — a wrong
     *   passphrase and a corrupt file fail in the same place (the GCM tag
     *   won't verify either way), so they share one handler by construction.
     */
    fun decrypt(packed: ByteArray, passphrase: CharArray): ByteArray {
        require(passphrase.isNotEmpty()) { "empty passphrase" }
        if (packed.size < MAGIC.length + 1 + SALT_BYTES + IV_BYTES + 1 ||
            !packed.copyOfRange(0, MAGIC.length).contentEquals(MAGIC.toByteArray(Charsets.US_ASCII))
        ) {
            throw BundleError("این فایل بستهٔ ته‌دیگ نیست")
        }
        val version = packed[MAGIC.length]
        if (version > VERSION) {
            throw BundleError(
                "این بسته با نسخهٔ جدیدتری از ته‌دیگ ساخته شده. برنامه را به‌روز کن.",
            )
        }
        val salt = packed.copyOfRange(MAGIC.length + 1, MAGIC.length + 1 + SALT_BYTES)
        val iv = packed.copyOfRange(
            MAGIC.length + 1 + SALT_BYTES, MAGIC.length + 1 + SALT_BYTES + IV_BYTES,
        )
        val body = packed.copyOfRange(MAGIC.length + 1 + SALT_BYTES + IV_BYTES, packed.size)
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
            }.doFinal(body)
        } catch (e: Exception) {
            // AEADFailure on wrong passphrase, truncation, or tampering alike.
            throw BundleError("گذرواژه درست نیست یا فایل خراب شده")
        }
    }

    private fun key(passphrase: CharArray, salt: ByteArray) = SecretKeySpec(
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)).encoded,
        "AES",
    )
}
