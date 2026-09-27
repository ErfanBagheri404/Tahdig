package com.erfanbagheri.tahdig.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Multi-timer math (#95). Pure so pause/resume/clock-skew is unit-testable
 * without a clock or a Context: every function takes `now` explicitly.
 *
 * A timer stores EITHER `endsAtMs` (running) OR `pausedRemainingMs` (paused) —
 * the same (remaining, updatedAt, paused) contract as [CookSessionMath], but
 * re-based on resume so a running timer is always a single wall-clock
 * subtraction away from its display.
 */
object TimerMath {

    /**
     * Remaining ms of a running timer ending at [endsAtMs].
     * Clamped to [0, totalMs]: a clock rolled BACK past the start would
     * otherwise report more time than was ever set (the skew ceiling).
     */
    fun remaining(endsAtMs: Long, nowMs: Long, totalMs: Long): Long =
        (endsAtMs - nowMs).coerceIn(0L, totalMs.coerceAtLeast(0L))

    /** Pause captures what is left — the value resume must restore exactly. */
    fun pauseRemaining(endsAtMs: Long, nowMs: Long, totalMs: Long): Long =
        remaining(endsAtMs, nowMs, totalMs)

    /** Resume re-bases: paused remaining survives untouched, however long the pause. */
    fun resumeEnd(pausedRemainingMs: Long, nowMs: Long): Long =
        nowMs + pausedRemainingMs.coerceAtLeast(0L)

    /** Reset/refill starts a full run from now. */
    fun resetEnd(totalMs: Long, nowMs: Long): Long = nowMs + totalMs.coerceAtLeast(0L)
}

/**
 * One countdown in the center (#95). Serialized to SharedPreferences as JSON —
 * a timer must survive process death, which is the whole point of the issue.
 */
@Serializable
data class TimerState(
    val id: Long,
    val name: String,
    val totalMs: Long,
    val running: Boolean,
    /** Valid while [running]; the alarm is armed at this epoch. */
    val endsAtMs: Long = 0L,
    /** Valid while paused — preserved exactly across an arbitrarily long pause. */
    val pausedRemainingMs: Long = 0L,
    /** True after the alarm fired; reset clears it. */
    val fired: Boolean = false,
) {
    // Fired rows freeze at ۰۰:۰۰ — a recomposed tick past the end never shows ۵۹:۵۹.
    fun remainingMs(nowMs: Long): Long = when {
        fired -> 0L
        running -> TimerMath.remaining(endsAtMs, nowMs, totalMs)
        else -> pausedRemainingMs.coerceAtLeast(0L)
    }

    /** Minutes+seconds display, Persian digits (۰۵:۱۲). */
    fun display(nowMs: Long): String =
        com.erfanbagheri.tahdig.util.PersianText.toPersianDigits(
            DurationParser.mmss(remainingMs(nowMs) / 1000L),
        )

    companion object {
        fun encode(list: List<TimerState>): String =
            kotlinx.serialization.json.Json.encodeToString(list)

        fun decode(raw: String?): List<TimerState> =
            if (raw.isNullOrBlank()) emptyList()
            else runCatching {
                kotlinx.serialization.json.Json.decodeFromString<List<TimerState>>(raw)
            }.getOrDefault(emptyList())
    }
}
