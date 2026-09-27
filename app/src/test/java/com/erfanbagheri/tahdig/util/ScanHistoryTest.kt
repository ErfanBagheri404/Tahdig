package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #116 acceptance: scan-history cap and cache-hit logic.
 *
 * These two are the parts the AC names explicitly, and both are pure, so they
 * are pinned here rather than left to an on-device ritual.
 */
class ScanHistoryTest {

    // ── History cap: last 20 scans ───────────────────────────────────

    @Test
    fun emptyHistoryStartsWithOneScan() {
        assertEquals(listOf("111"), ScanHistory.push(emptyList(), "111"))
    }

    @Test
    fun newestScanGoesFirst() {
        val after = ScanHistory.push(listOf("111", "222"), "333")
        assertEquals(listOf("333", "111", "222"), after)
    }

    @Test
    fun historyIsCappedAtTwenty() {
        var history: List<String> = emptyList()
        for (i in 1..45) history = ScanHistory.push(history, "code$i")
        assertEquals(ScanHistory.MAX_ENTRIES, history.size)
        assertEquals(20, history.size)
    }

    @Test
    fun capKeepsTheNewestNotTheOldest() {
        var history: List<String> = emptyList()
        for (i in 1..45) history = ScanHistory.push(history, "code$i")
        // code45 is the last scan taken; it must still be at the front.
        assertEquals("code45", history.first())
        // A 20-wide window over 45 pushes keeps code45..code26, so code26 is
        // the oldest survivor (45 - 20 + 1 = 26).
        assertEquals("code26", history.last())
        // code1..code25 fell out of the window.
        assertFalse("code1" in history)
        assertFalse("code25" in history)
        assertTrue("code26" in history)
    }

    @Test
    fun repeatedScanMovesToFrontWithoutDuplicating() {
        val one = ScanHistory.push(listOf("111", "222", "333"), "222")
        assertEquals(listOf("222", "111", "333"), one)
        assertEquals(1, one.count { it == "222" })
    }

    @Test
    fun blankScanIsIgnored() {
        val history = listOf("111")
        assertEquals(history, ScanHistory.push(history, "   "))
        assertEquals(history, ScanHistory.push(history, ""))
    }

    @Test
    fun paddingWithBlanksNeverInflatesTheList() {
        var history: List<String> = emptyList()
        repeat(50) { history = ScanHistory.push(history, "  ") }
        assertEquals(emptyList<String>(), history)
    }

    // ── Cache hit: offline scan resolves without network ─────────────

    @Test
    fun previouslyScannedBarcodeIsACacheHit() {
        val cached = setOf("737628064502", "5000112637922")
        assertTrue(ScanHistory.isCached(cached, "737628064502"))
    }

    @Test
    fun unseenBarcodeIsNotACacheHit() {
        val cached = setOf("737628064502")
        assertFalse(ScanHistory.isCached(cached, "4007680000000"))
    }

    @Test
    fun emptyCacheIsNeverAHit() {
        assertFalse(ScanHistory.isCached(emptySet(), "737628064502"))
    }

    @Test
    fun blankBarcodeIsNeverAHit() {
        // Guard against a camera misread producing "" and hitting a cached "".
        assertFalse(ScanHistory.isCached(emptySet(), ""))
        assertFalse(ScanHistory.isCached(setOf(""), "   "))
    }

    @Test
    fun whitespacePaddedScanStillFindsItsCacheEntry() {
        assertTrue(ScanHistory.isCached(setOf("737628064502"), "  737628064502 "))
    }

    @Test
    fun pushingThenLookingUpIsAConsistentRoundTrip() {
        val after = ScanHistory.push(emptyList(), "737628064502")
        assertTrue(ScanHistory.isCached(after.toSet(), "737628064502"))
    }
}
