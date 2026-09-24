package com.erfanbagheri.tahdig.util

/**
 * Scan-history rules for the barcode scanner (#116).
 *
 * Pure by design: the cap and the cache decision take plain inputs so they can
 * be pinned as tests without a Room instance or a camera.
 */
object ScanHistory {

    /** The AC's cap: the last 20 scans, nothing more. */
    const val MAX_ENTRIES = 20

    /**
     * Prepend [barcode] and drop anything past [MAX_ENTRIES].
     *
     * Newest first — the user scans, and the scan they want to re-read is the
     * one they just did, not the one from three scans ago. A repeat scan moves
     * to the front rather than duplicating, so the list never fills with one
     * barcode scanned five times.
     */
    fun push(barcodes: List<String>, barcode: String): List<String> {
        val code = barcode.trim()
        if (code.isEmpty()) return barcodes
        return (listOf(code) + barcodes.filter { it != code }).take(MAX_ENTRIES)
    }

    /**
     * The decision the scan flow needs: is this barcode already cached, and
     * therefore resolvable with no network at all?
     *
     * Cache hit means the product came from an earlier online scan and is
     * stored locally — the airplane-mode case the AC asks for. A miss is not an
     * error; it just means we have to ask OFF, which is exactly where the
     * honest offline hint belongs.
     */
    fun isCached(cached: Set<String>, barcode: String): Boolean {
        val code = barcode.trim()
        return code.isNotBlank() && cached.any { it.trim() == code }
    }
}
