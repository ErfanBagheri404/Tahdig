package com.erfanbagheri.tahdig.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh

/** One action a row offers, both as a swipe direction and as a menu entry. */
data class RowAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    /** Destructive actions get the error colour in the background reveal. */
    val destructive: Boolean = false,
)

/**
 * Which action a settled swipe means, in terms of what the finger did on screen.
 *
 * This lives outside the composable because the mapping is the part that broke:
 * under an RTL locale Material3's `StartToEnd`/`EndToStart` no longer describe
 * the direction the finger travelled (a visually leftward drag settles on
 * `StartToEnd`), so the reveal label and the action it fired disagreed — the row
 * showed «حذف» and ticked the item off. Both now call this one function, so they
 * cannot drift apart again.
 *
 * `null` means "no direction" — a gesture that settled without travelling.
 */
internal fun swipeActionFor(
    value: SwipeToDismissBoxValue,
    swipeLeft: RowAction?,
    swipeRight: RowAction?,
): RowAction? = when (value) {
    // Empirically RTL: a leftward drag settles on StartToEnd. `?: swipeRight`
    // keeps a one-action row working from either direction.
    SwipeToDismissBoxValue.StartToEnd -> swipeLeft ?: swipeRight
    SwipeToDismissBoxValue.EndToStart -> swipeRight ?: swipeLeft
    else -> null
}

/**
 * A list row with swipe gestures AND the same actions in an overflow menu
 * (#127).
 *
 * The gesture is never the only path: swipe-to-dismiss is invisible, easy to
 * trigger by accident, and unreachable for a user who cannot perform it. Every
 * action here is also a labelled menu item, so screen readers and switch access
 * get identical capability.
 *
 * The row never actually dismisses — it snaps back and the action runs, because
 * the undo window owns the decision. A row that vanished on swipe would look
 * deleted with no trace of how to get it back.
 */
@Composable
fun SwipeActionRow(
    /** Action for a visually leftward drag. */
    swipeLeft: RowAction,
    modifier: Modifier = Modifier,
    /** Action for a visually rightward drag; omit for a one-action row. */
    swipeRight: RowAction? = null,
    content: @Composable () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    // Set once per gesture, then cleared when the box is back at rest. A plain
    // `remember` is right here: the action belongs to the gesture, not to
    // surviving a config change.
    var pending by remember { mutableStateOf<RowAction?>(null) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            pending = swipeActionFor(value, swipeLeft, swipeRight)
            // false = do not dismiss; see the KDoc.
            false
        },
    )

    // One shot per gesture: `pending` is cleared here, not by watching the
    // settle. Watching `targetValue` races — it flips to Settled in the same
    // frame as the confirm callback and clears the action before it can run.
    LaunchedEffect(pending) {
        val action = pending ?: return@LaunchedEffect
        action.onClick()
        pending = null
    }

    Box(modifier = modifier.fillMaxWidth()) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                // Same function as the action, so the label can never promise a
                // different action than the one that runs.
                val target = swipeActionFor(
                    dismissState.targetValue,
                    swipeLeft,
                    swipeRight,
                ) ?: swipeLeft
                Surface(
                    color = if (target.destructive) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Icon(
                            imageVector = target.icon,
                            contentDescription = null,
                            tint = if (target.destructive) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = target.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = YekanBakh,
                            textAlign = TextAlign.Start,
                            color = if (target.destructive) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { content() }
                // The labelled alternative to the gesture. Always present.
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "گزینه‌های بیشتر",
                        modifier = Modifier.padding(2.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    listOfNotNull(swipeLeft, swipeRight).forEach { action ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    action.label,
                                    fontFamily = YekanBakh,
                                    color = if (action.destructive) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    action.icon,
                                    contentDescription = null,
                                    tint = if (action.destructive) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                action.onClick()
                            },
                        )
                    }
                }
            }
        }
    }
}
