package com.erfanbagheri.tahdig.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The app's single undo slot (#127).
 *
 * One global buffer, because one snackbar is what the user can act on: a
 * second concurrent destructive action replaces the first, exactly like a
 * snackbar queue of one. [pending] is a flow so the host composable can show
 * the snackbar the moment any ViewModel arms it, without every ViewModel
 * needing its own host.
 */
object UndoHub {
    private val buffer = UndoBuffer()
    private val _pending = MutableStateFlow<String?>(null)

    /** Label of the action awaiting undo, or null when idle. */
    val pending: StateFlow<String?> = _pending.asStateFlow()

    /** Record a destructive action and make it visible to the snackbar host. */
    fun arm(label: String, undo: () -> Unit) {
        buffer.arm(label, undo)
        _pending.value = label
    }

    /** Run the pending inverse. Safe to call twice — the second is a no-op. */
    fun undo(): Boolean {
        val ran = buffer.undo()
        _pending.value = null
        return ran
    }

    /**
     * Drop the entry without running it. Called on snackbar timeout/dismiss so
     * the buffer cannot be revived long after the user forgot the action.
     */
    fun clear() {
        buffer.clear()
        _pending.value = null
    }
}
