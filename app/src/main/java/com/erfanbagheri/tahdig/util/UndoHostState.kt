package com.erfanbagheri.tahdig.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide bridge between a destructive action and the root undo snackbar.
 *
 * A ViewModel cannot talk to a composable, so it records the undo in [UndoBuffer] and
 * publishes the label here; the snackbar in the root Scaffold observes that and shows.
 * The state lives at the application level rather than on a screen so an undo survives
 * navigation away from the action that triggered it — deleting a row and switching tabs
 * must not silently drop the ability to take it back.
 *
 * ponytail: one label in flight, replaced on each destructive action, matching
 * [UndoBuffer]'s single-pending-entry rule. No queue — a second destructive action
 * legitimately discards the first's undo.
 */
object UndoHostState {

    private val _label = MutableStateFlow<String?>(null)

    /** Label of the pending undo, or null when nothing is pending. */
    val label: StateFlow<String?> = _label.asStateFlow()

    /**
     * Record a destructive action and publish its undo snackbar in one call — the two
     * must always move together, so callers get a single entry point rather than a
     * push-then-show pair they could desynchronize.
     */
    fun push(label: String, undo: () -> Unit) {
        UndoBuffer.push(label, undo)
        _label.value = label
    }

    /** The snackbar has resolved (acted on or dismissed); drop the label. */
    fun clear() { _label.value = null }
}
