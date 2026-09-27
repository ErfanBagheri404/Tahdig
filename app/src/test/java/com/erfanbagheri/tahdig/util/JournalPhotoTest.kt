package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two size decisions behind journal photos (#124) are pure, so they are
 * pinned here — decoding and JPEG encoding need a device.
 */
class JournalPhotoTest {

    // ── sampleSize: power-of-two downscale, never below 1 ────────────

    @Test
    fun smallImage_isNotDownscaled() {
        assertEquals(1, JournalPhoto.sampleSize(640, maxEdge = 720))
        assertEquals(1, JournalPhoto.sampleSize(720, maxEdge = 720))
    }

    @Test
    fun largeImage_halvesUntilItFits() {
        // 4000 → /2=2000 ≥720 → /4=1000 ≥720 → /8=500 <720 → stop at 4.
        assertEquals(4, JournalPhoto.sampleSize(4000, maxEdge = 720))
        // 1440 → /2=720 ≥720 → /4=360 <720 → stop at 2.
        assertEquals(2, JournalPhoto.sampleSize(1440, maxEdge = 720))
    }

    @Test
    fun degenerateEdges_fallBackToNoScaling() {
        assertEquals(1, JournalPhoto.sampleSize(0))
        assertEquals(1, JournalPhoto.sampleSize(-5))
        assertEquals(1, JournalPhoto.sampleSize(1000, maxEdge = 0))
    }

    // ── scaledSize: aspect preserved, long edge capped ───────────────

    @Test
    fun landscape_longEdgeBecomesMax() {
        val (w, h) = JournalPhoto.scaledSize(1600, 900)
        assertEquals(720, w)
        assertEquals(405, h)
    }

    @Test
    fun portrait_capsHeightNotWidth() {
        val (w, h) = JournalPhoto.scaledSize(900, 1600)
        assertEquals(405, w)
        assertEquals(720, h)
    }

    @Test
    fun alreadySmall_isLeftAlone() {
        assertEquals(300 to 200, JournalPhoto.scaledSize(300, 200))
        assertEquals(720 to 720, JournalPhoto.scaledSize(720, 720))
    }

    @Test
    fun neverScalesBelowOnePixel() {
        val (w, h) = JournalPhoto.scaledSize(10000, 3)
        assertEquals(720, w)
        assertTrue("height collapsed to zero", h >= 1)
    }

    @Test
    fun degenerateInput_yieldsOneByOne() {
        assertEquals(1 to 1, JournalPhoto.scaledSize(0, 0))
    }

    @Test
    fun aspectRatio_isPreservedWithinRounding() {
        val (w, h) = JournalPhoto.scaledSize(4032, 3024)
        val original = 4032f / 3024f
        val scaled = w.toFloat() / h
        assertTrue("aspect drifted: $scaled vs $original", kotlin.math.abs(scaled - original) < 0.01f)
    }
}
