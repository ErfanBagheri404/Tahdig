package com.erfanbagheri.tahdig.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Daily photo prompt + photo-challenge counter (#125).
 *
 * Pure and clock-injected, like [NotifyMath]: the prompt's gate is a boolean
 * question about data the caller already has, so "fires only when no photo
 * today" and "suppressed in quiet hours" are unit tests rather than emulator
 * runs.
 *
 * Photos are ALWAYS optional. Nothing here ever blocks a cook, and a prompt
 * that fails to post must never surface an error.
 */
object PhotoChallenge {

    /** The weekly challenge target: «این هفته ۳ عکس گذاشتی». */
    const val WEEKLY_GOAL = 3

    /** Why a prompt was skipped — the receiver logs nothing, the tests read this. */
    enum class Verdict { POST, NO_PHOTO_TODAY_MISSING, PHOTO_ALREADY_TODAY, QUIET_HOURS, DISABLED }

    /**
     * The prompt decision. Order matters: a disabled toggle wins over
     * everything, and "a photo already exists today" wins over quiet hours so
     * the user is never told they are missing something they already did.
     */
    fun verdict(
        enabled: Boolean,
        now: LocalTime,
        quietOn: Boolean,
        quietFrom: LocalTime,
        quietUntil: LocalTime,
        hasPhotoToday: Boolean,
    ): Verdict = when {
        !enabled -> Verdict.DISABLED
        hasPhotoToday -> Verdict.PHOTO_ALREADY_TODAY
        quietOn && !NotifyMath.mayPost(now, NotifyMath.Kind.REMINDER, quietFrom, quietUntil) ->
            Verdict.QUIET_HOURS
        else -> Verdict.POST
    }

    /** Saturday-start week containing [day] — the same week the streak uses. */
    fun weekStart(day: LocalDate): LocalDate =
        day.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))

    /** Photos whose local date falls inside [day]'s week. */
    fun weeklyCount(photoTimestamps: List<Long>, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int {
        val start = weekStart(day)
        val end = start.plusDays(6)
        return photoTimestamps.count { ts ->
            val d = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()
            !d.isBefore(start) && !d.isAfter(end)
        }
    }

    /** Any photo stamped on [day] itself — the prompt's suppression test. */
    fun hasPhotoOn(photoTimestamps: List<Long>, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        photoTimestamps.any { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == day }

    /** The journal challenge line: «این هفته ۳ عکس گذاشتی». */
    fun weeklyLine(count: Int): String {
        val n = PersianText.toPersianDigits(count.toString())
        val goal = PersianText.toPersianDigits(WEEKLY_GOAL.toString())
        return when {
            count >= WEEKLY_GOAL -> "این هفته $n عکس گذاشتی — چالش کامل شد ✅"
            count == 0 -> "این هفته هنوز عکسی نگذاشتی — هدف: $goal عکس"
            else -> "این هفته $n عکس گذاشتی — $goal تا هدف"
        }
    }
}
