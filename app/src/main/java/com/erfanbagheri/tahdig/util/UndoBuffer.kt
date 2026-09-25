package com.erfanbagheri.tahdig.util

/**
 * One-shot undo slot (#127).
 *
 * A destructive action is a closure that undoes it — a real inverse operation,
 * not a snapshot of the UI. The buffer holds at most one entry: the last
 * destructive action is the one a user would want to reverse, and holding a
 * queue would promise an "undo history" that never ships.
 *
 * [pending] stays set until [undo] or [clear] runs, so the UI can render a
 * snackbar against a real window instead of a guessed timeout.
 *
 * The window is deliberately LONGER than the snackbar it drives: Material's
 * SnackbarDuration.Short is ~4s, so a matching 5s buffer would leave the final
 * second looking undoable while the action button was already gone. 10s pairs
 * with SnackbarDuration.Long, and the extra seconds cost nothing because the
 * inverse only runs if the user actually taps it.
 */
class UndoBuffer(
    private val windowMs: Long = DEFAULT_WINDOW_MS,
    /** Injectable clock so the window is unit-tested without sleeping. */
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    private var action: (() -> Unit)? = null
    private var expiresAt: Long = 0L

    /** Label of the pending action, or null when there is nothing to undo. */
    var pending: String? = null
        private set

    /**
     * Record a destructive action. Any previous entry is dropped — the newest
     * action wins, matching how a snackbar queue of one behaves.
     */
    fun arm(label: String, undo: () -> Unit) {
        action = undo
        pending = label
        expiresAt = clock() + windowMs
    }

    /**
     * Run the pending inverse if the window is still open. Returns true when it
     * ran, so the caller can skip a follow-up action.
     */
    fun undo(): Boolean {
        val current = action ?: return false
        if (clock() > expiresAt) {
            clear()
            return false
        }
        // Clear before running: a re-entrant arm from inside the inverse must
        // survive, and a double-tap must not run the inverse twice.
        clear()
        current()
        return true
    }

    /** True once the window has elapsed, for the snackbar timeout. */
    fun isExpired(): Boolean = pending != null && clock() > expiresAt

    /** Milliseconds left, never negative. */
    fun remainingMs(): Long =
        if (pending == null) 0L else (expiresAt - clock()).coerceAtLeast(0L)

    fun clear() {
        action = null
        pending = null
        expiresAt = 0L
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 10_000L
    }
}
