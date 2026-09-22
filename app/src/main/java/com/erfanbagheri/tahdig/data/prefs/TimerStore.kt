package com.erfanbagheri.tahdig.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.erfanbagheri.tahdig.notify.TimerScheduler
import com.erfanbagheri.tahdig.util.TimerMath
import com.erfanbagheri.tahdig.util.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persistent named timers (#95). SharedPreferences JSON, not Room: a timer
 * list of ≤5 rows never needs a query, and a prefs row cannot drag a schema
 * bump / destructive migration behind it.
 *
 * Every mutation re-arms the matching AlarmManager alarm in the same call, so
 * the scheduled world can never drift from the stored one.
 */
object TimerStore {
    private const val PREFS_NAME = "tahdig_timers"
    private const val KEY = "timers"

    /** AC: max 5 concurrent — the UI disables add beyond this. */
    const val MAX = 5

    private lateinit var prefs: SharedPreferences
    private val _timers = MutableStateFlow<List<TimerState>>(emptyList())
    val timers: StateFlow<List<TimerState>> = _timers

    fun isInitialized(): Boolean = ::prefs.isInitialized

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _timers.value = TimerState.decode(prefs.getString(KEY, null))
    }

    fun get(id: Long): TimerState? = _timers.value.firstOrNull { it.id == id }

    /** Returns false when [MAX] is already running — the UI shows the Farsi hint. */
    fun add(context: Context, name: String, totalMs: Long): Boolean {
        if (_timers.value.size >= MAX || totalMs <= 0L) return false
        val id = (_timers.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val timer = TimerState(
            id = id,
            name = name.ifBlank { "تایمر" },
            totalMs = totalMs,
            running = true,
            endsAtMs = TimerMath.resetEnd(totalMs, System.currentTimeMillis()),
        )
        write(_timers.value + timer)
        TimerScheduler.schedule(context, timer)
        return true
    }

    fun pause(context: Context, id: Long) = update(id) { t ->
        if (!t.running) t else t.copy(
            running = false,
            pausedRemainingMs = TimerMath.pauseRemaining(
                t.endsAtMs, System.currentTimeMillis(), t.totalMs,
            ),
        ).also { TimerScheduler.cancel(context, id) }
    }

    fun resume(context: Context, id: Long) = update(id) { t ->
        if (t.running) t else t.copy(
            running = true,
            fired = false,
            endsAtMs = TimerMath.resumeEnd(t.pausedRemainingMs, System.currentTimeMillis()),
        ).also { TimerScheduler.schedule(context, it) }
    }

    fun reset(context: Context, id: Long) = update(id) { t ->
        t.copy(
            running = true,
            fired = false,
            endsAtMs = TimerMath.resetEnd(t.totalMs, System.currentTimeMillis()),
        ).also { TimerScheduler.schedule(context, it) }
    }

    /** Rename only — the alarm's epoch does not move. */
    fun rename(id: Long, name: String) = update(id) { t ->
        if (name.isBlank()) t else t.copy(name = name)
    }

    fun remove(context: Context, id: Long) {
        TimerScheduler.cancel(context, id)
        write(_timers.value.filterNot { it.id == id })
    }

    /** Receiver marks completion; no alarm work (it just fired). */
    fun markFired(id: Long) = update(id) { it.copy(running = false, fired = true, pausedRemainingMs = 0L) }

    /** Re-arm every still-running timer (boot, app start — alarms don't outlive reboots). */
    fun rearmAll(context: Context) {
        _timers.value.filter { it.running && !it.fired }.forEach { TimerScheduler.schedule(context, it) }
    }

    private fun update(id: Long, fn: (TimerState) -> TimerState) {
        val cur = _timers.value
        val i = cur.indexOfFirst { it.id == id }
        if (i < 0) return
        val next = fn(cur[i])
        if (next == cur[i]) return
        write(cur.toMutableList().also { it[i] = next })
    }

    private fun write(list: List<TimerState>) {
        _timers.value = list
        if (isInitialized()) prefs.edit().putString(KEY, TimerState.encode(list)).apply()
    }
}
