package com.erfanbagheri.tahdig.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Daily photo prompt + weekly photo counter (#125). */
class PhotoChallengeTest {

    private val zone = ZoneId.of("Asia/Tehran")
    private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)

    private fun ts(y: Int, m: Int, d: Int, h: Int = 12) =
        ZonedDateTime.of(y, m, d, h, 0, 0, 0, zone).toInstant().toEpochMilli()

    // ── Prompt gate ─────────────────────────────────────────────────

    @Test fun enabled_noPhotoToday_outsideQuiet_posts() {
        assertEquals(
            PhotoChallenge.Verdict.POST,
            PhotoChallenge.verdict(
                enabled = true, now = t(13, 0),
                quietOn = false, quietFrom = t(22, 0), quietUntil = t(7, 0),
                hasPhotoToday = false,
            ),
        )
    }

    @Test fun disabled_neverPosts() {
        assertEquals(
            PhotoChallenge.Verdict.DISABLED,
            PhotoChallenge.verdict(
                enabled = false, now = t(13, 0),
                quietOn = false, quietFrom = t(22, 0), quietUntil = t(7, 0),
                hasPhotoToday = false,
            ),
        )
    }

    @Test fun photoAlreadyToday_suppressed() {
        assertEquals(
            PhotoChallenge.Verdict.PHOTO_ALREADY_TODAY,
            PhotoChallenge.verdict(
                enabled = true, now = t(13, 0),
                quietOn = false, quietFrom = t(22, 0), quietUntil = t(7, 0),
                hasPhotoToday = true,
            ),
        )
    }

    @Test fun photoAlreadyToday_winsOverQuietHours() {
        // AC: "suppressed during quiet hours AND if a photo already exists
        // today". A user who already posted must never be told they are
        // missing something, even at 23:00.
        assertEquals(
            PhotoChallenge.Verdict.PHOTO_ALREADY_TODAY,
            PhotoChallenge.verdict(
                enabled = true, now = t(23, 0),
                quietOn = true, quietFrom = t(22, 0), quietUntil = t(7, 0),
                hasPhotoToday = true,
            ),
        )
    }

    @Test fun quietHours_suppressPrompt() {
        assertEquals(
            PhotoChallenge.Verdict.QUIET_HOURS,
            PhotoChallenge.verdict(
                enabled = true, now = t(23, 30),
                quietOn = true, quietFrom = t(22, 0), quietUntil = t(7, 0),
                hasPhotoToday = false,
            ),
        )
    }

    @Test fun quietOff_insideWindow_stillPosts() {
        // The quiet window only matters when the user turned it on.
        assertEquals(
            PhotoChallenge.Verdict.POST,
            PhotoChallenge.verdict(
                enabled = true, now = t(23, 30),
                quietOn = false, quietFrom = t(22, 0), quietUntil = t(7, 0),
                hasPhotoToday = false,
            ),
        )
    }

    // ── Week math (Saturday start, matching the streak) ─────────────

    @Test fun weekStart_isSaturday() {
        // 2026-09-24 is a Thursday; its week starts Sat 2026-09-19.
        assertEquals(
            LocalDate.of(2026, 9, 19),
            PhotoChallenge.weekStart(LocalDate.of(2026, 9, 24)),
        )
    }

    @Test fun weekStart_onSaturday_isItself() {
        assertEquals(
            LocalDate.of(2026, 9, 19),
            PhotoChallenge.weekStart(LocalDate.of(2026, 9, 19)),
        )
    }

    @Test fun weeklyCount_countsOnlyThisWeek() {
        val photos = listOf(
            ts(2026, 9, 19), // Sat — in
            ts(2026, 9, 24), // Thu — in
            ts(2026, 9, 18), // Fri — previous week, out
            ts(2026, 9, 26), // next Sat — out
        )
        assertEquals(2, PhotoChallenge.weeklyCount(photos, LocalDate.of(2026, 9, 24), zone))
    }

    @Test fun weeklyCount_includesBothEdges() {
        // Saturday and the following Friday are both inside the week.
        val photos = listOf(ts(2026, 9, 19), ts(2026, 9, 25))
        assertEquals(2, PhotoChallenge.weeklyCount(photos, LocalDate.of(2026, 9, 24), zone))
    }

    @Test fun weeklyCount_emptyIsZero() {
        assertEquals(0, PhotoChallenge.weeklyCount(emptyList(), LocalDate.of(2026, 9, 24), zone))
    }

    @Test fun hasPhotoOn_matchesSameLocalDay() {
        val photos = listOf(ts(2026, 9, 24, 8), ts(2026, 9, 23, 23))
        assertTrue(PhotoChallenge.hasPhotoOn(photos, LocalDate.of(2026, 9, 24), zone))
        assertFalse(PhotoChallenge.hasPhotoOn(photos, LocalDate.of(2026, 9, 22), zone))
    }

    @Test fun hasPhotoOn_lateNightStaysOnItsOwnDay() {
        // 23:59 local is that day, not the next.
        val photos = listOf(ts(2026, 9, 24, 23))
        assertTrue(PhotoChallenge.hasPhotoOn(photos, LocalDate.of(2026, 9, 24), zone))
        assertFalse(PhotoChallenge.hasPhotoOn(photos, LocalDate.of(2026, 9, 25), zone))
    }

    // ── The challenge line ──────────────────────────────────────────

    @Test fun weeklyLine_zeroStateShowsGoal() {
        assertTrue(PhotoChallenge.weeklyLine(0).contains("۳"))
    }

    @Test fun weeklyLine_partialShowsProgress() {
        val line = PhotoChallenge.weeklyLine(2)
        assertTrue(line.contains("۲"))
        assertFalse(line.contains("کامل شد"))
    }

    @Test fun weeklyLine_atGoalIsComplete() {
        assertTrue(PhotoChallenge.weeklyLine(3).contains("کامل شد"))
    }

    @Test fun weeklyLine_overGoalStillComplete() {
        assertTrue(PhotoChallenge.weeklyLine(7).contains("کامل شد"))
    }
}
