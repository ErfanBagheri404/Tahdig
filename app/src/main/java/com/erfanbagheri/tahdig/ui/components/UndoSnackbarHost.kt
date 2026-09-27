package com.erfanbagheri.tahdig.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.erfanbagheri.tahdig.util.UndoBuffer
import com.erfanbagheri.tahdig.util.UndoHostState

/**
 * The undo snackbar, hoisted into the root Scaffold's `snackbarHost` slot so it sits
 * above every screen.
 *
 * [UndoHostState] carries the pending label; the material snackbar renders it with a
 * «بازگردانی» action. The window is bounded by the snackbar's own Short duration (~4s),
 * inside [UndoBuffer.WINDOW_MS] — dismissing the snackbar clears the buffer, so the two
 * can never disagree about whether an undo is still available.
 *
 * Not exercised by a test directly; the buffer semantics it depends on are covered by
 * UndoBufferTest, and this composable holds no logic beyond collecting state and
 * mapping the snackbar result onto the buffer.
 */
@Composable
fun UndoSnackbarHost() {
    val label by UndoHostState.label.collectAsState()
    val hostState = remember { SnackbarHostState() }

    LaunchedEffect(label) {
        val pending = label ?: return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = pending,
            actionLabel = "بازگردانی",
            duration = SnackbarDuration.Short,
        )
        when (result) {
            SnackbarResult.ActionPerformed -> UndoBuffer.pop()
            SnackbarResult.Dismissed -> UndoBuffer.clear()
        }
        UndoHostState.clear()
    }

    SnackbarHost(hostState = hostState)
}
