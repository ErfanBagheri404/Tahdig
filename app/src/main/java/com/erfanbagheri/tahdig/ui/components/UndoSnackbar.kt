package com.erfanbagheri.tahdig.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import com.erfanbagheri.tahdig.util.UndoHub

/**
 * The one undo snackbar every destructive action uses (#127).
 *
 * One host for the whole app: any ViewModel arms [UndoHub], this reacts. A
 * confirm dialog is the wrong tool here — it costs a tap to confirm, then a
 * second flow to recover. Undo is one gesture, and the snackbar disappears on
 * its own if ignored.
 */
@Composable
fun UndoSnackbarHost(modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    val pending by UndoHub.pending.collectAsStateWithLifecycle()

    // Keyed on the label: a second destructive action while the first snackbar
    // is up cancels the old one and shows the new, never both.
    LaunchedEffect(pending) {
        val label = pending ?: return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = label,
            actionLabel = "بازگردانی",
            // Long, not Indefinite: the window is bounded at 10s so the buffer
            // can never outlive the affordance, but the snackbar has to stay up
            // at least as long as the buffer or the last seconds of the window
            // look undoable with no button left to press.
            duration = SnackbarDuration.Long,
            withDismissAction = false,
        )
        if (result == SnackbarResult.ActionPerformed) {
            UndoHub.undo()
        } else {
            // Dismissed without action. The buffer expires on its own, so drop
            // it here too — otherwise a stale entry could be revived by the
            // next snackbar's action.
            UndoHub.clear()
        }
    }

    SnackbarHost(hostState = hostState, modifier = modifier)
}
