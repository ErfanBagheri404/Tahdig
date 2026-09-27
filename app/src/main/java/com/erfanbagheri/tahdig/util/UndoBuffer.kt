package com.erfanbagheri.tahdig.util

/**
 * One-shot undo for destructive actions.
 *
 * The app's destructive operations (delete recipe, clear shopping list, clear history,
 * end a cook session) were irreversible; the only safety net was a full backup. A short
 * window to take it back beats a confirm dialog for both speed and trust — the action
 * happens now, the user can reverse it, and after the window the undo path is gone.
 *
 * Single pending entry, not a stack: this is "undo the last destructive thing", not a
 * general history. A second destructive action replaces the first's undo, which matches
 * how a snackbar queue replaces one message with the next.
 *
 * ponytail: the undo is a closure ([undo]) the caller supplies, so the buffer owns no
 * domain knowledge — the screen captures whatever state it deleted and closes over
 * re-inserting it. [push] returns a handle whose [expired] flag the tests flip on
 * expiry, which is the only cross-window signal a caller ever needs.
 */
object UndoBuffer {

    /** How long a destructive action stays undoable, in milliseconds. */
    const val WINDOW_MS = 5_000L

    /** A pending undo. [expired] flips true when the window closes without it firing. */
    class Entry(
        val label: String,
        val expiresAt: Long,
        val undo: () -> Unit,
    ) {
        @Volatile var expired = false; private set
        fun expire() { expired = true }
    }

    @Volatile
    private var pending: Entry? = null

    /**
     * Record a destructive action. Replaces any earlier pending undo: only the most
     * recent destructive thing can be taken back.
     *
     * The clock is injectable so tests can step the window deterministically.
     */
    fun push(
        label: String,
        undo: () -> Unit,
        nowMs: Long = System.currentTimeMillis(),
    ): Entry {
        pending?.expire()
        val e = Entry(label, nowMs + WINDOW_MS, undo)
        pending = e
        return e
    }

    /** The pending undo, or null once the window has closed. */
    fun current(nowMs: Long = System.currentTimeMillis()): Entry? {
        val e = pending ?: return null
        if (nowMs > e.expiresAt) {
            e.expire()
            pending = null
            return null
        }
        return e
    }

    /**
     * Run the pending undo if one exists. Returns true when something was reversed.
     * Pops the entry — an undo is single-use, like the action it undoes.
     */
    fun pop(nowMs: Long = System.currentTimeMillis()): Boolean {
        val e = current(nowMs) ?: return false
        pending = null
        e.undo()
        return true
    }

    /** Drop any pending undo without running it. */
    fun clear() {
        pending?.expire()
        pending = null
    }
}
