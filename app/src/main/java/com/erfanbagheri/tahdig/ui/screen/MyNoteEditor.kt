package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erfanbagheri.tahdig.data.local.entity.RatingEntity
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.RatingViewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay

/**
 * «یادداشت من» — the user's private note on a dish (#134).
 *
 * Deliberately one line, not a dialog: a note is a thought you have while
 * looking at the dish, and a dialog would put a modal between the user and
 * the thing they are thinking about. The field collapses back to a quiet label
 * once saved, so a dish with a note does not look like a form.
 *
 * Delete keeps the text in a [remember] slot for the undo window, so undo is
 * restoring what was typed rather than re-fetching from the DB.
 */
@Composable
fun MyNoteEditor(
    viewModel: RatingViewModel,
    foodId: Long,
    modifier: Modifier = Modifier,
) {
    val saved by viewModel.note(foodId).collectAsState()

    // Editing state: null = not editing (show the label or the saved text).
    var editing by remember(foodId) { mutableStateOf(false) }
    var draft by remember(foodId) { mutableStateOf("") }

    // Last deletion, kept only long enough for an undo to be worth offering.
    var lastDeleted by remember(foodId) { mutableStateOf<String?>(null) }

    LaunchedEffect(lastDeleted) {
        if (lastDeleted != null) {
            delay(UNDO_WINDOW_MS)
            lastDeleted = null
        }
    }

    if (editing) {
        Column(modifier = modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("یادداشت من", fontFamily = YekanBakh) },
                placeholder = {
                    Text("مثلاً: با لوبیای قرمز درست می‌شود", fontFamily = YekanBakh, fontSize = 13.sp)
                },
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontFamily = YekanBakh),
                minLines = 1,
                maxLines = 4,
                supportingText = {
                    Text(
                        "${draft.length} / ${RatingEntity.MAX_NOTE_CHARS}",
                        fontFamily = YekanBakh,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = {
                    viewModel.setNote(foodId, draft)
                    editing = false
                }) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("ذخیره", fontFamily = YekanBakh, fontSize = 13.sp)
                }
                TextButton(onClick = {
                    draft = ""
                    editing = false
                }) {
                    Text("انصراف", fontFamily = YekanBakh, fontSize = 13.sp)
                }
            }
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = {
                draft = saved
                editing = true
            }) {
                Text(
                    if (saved.isBlank()) "+ یادداشت من" else "یادداشت من:",
                    fontFamily = YekanBakh,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (saved.isNotBlank()) {
                Text(
                    text = saved,
                    fontFamily = YekanBakh,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f, fill = true),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                IconButton(
                    onClick = {
                        lastDeleted = saved
                        viewModel.clearNote(foodId)
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "پاک کردن یادداشت",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        // Undo for a delete. Sits under the row so it never shifts the stars.
        lastDeleted?.let { text ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "یادداشت پاک شد",
                    fontFamily = YekanBakh,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { viewModel.setNote(foodId, text) }) {
                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("بازگردانی", fontFamily = YekanBakh, fontSize = 12.sp)
                }
            }
        }
    }
}

/** How long a delete stays undoable. Long enough to notice, short enough to forget. */
private const val UNDO_WINDOW_MS = 5_000L
