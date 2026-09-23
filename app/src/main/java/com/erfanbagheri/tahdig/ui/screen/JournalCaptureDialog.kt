package com.erfanbagheri.tahdig.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh

/**
 * Journal capture (#124) — the one-time prompt after «پختم».
 *
 * Everything here is optional: a photo, one line, or nothing. Dismissing keeps
 * the stamped journal row, so the memory exists even when the user taps past.
 */
@Composable
fun JournalCaptureDialog(
    dishName: String,
    onPhoto: (Uri) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember { mutableStateOf("") }
    var photoPicked by remember { mutableStateOf(false) }

    // GetContent keeps this dependency-free — no camera permission, no
    // FileProvider juggling; the user's own picker (or camera app) supplies
    // the image and we only ever read it.
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            onPhoto(uri)
            photoPicked = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "خاطره‌ای از «$dishName»؟",
                fontFamily = YekanBakh,
            )
        },
        text = {
            Column {
                Text(
                    text = "می‌تونی رد کنی — پختت ثبت شده.",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { picker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (photoPicked) "عکس انتخاب شد ✓" else "افزودن عکس",
                        fontFamily = YekanBakh,
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یک خط یادداشت", fontFamily = YekanBakh) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(note) }) {
                Text("ثبت", fontFamily = YekanBakh)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("رد کردن", fontFamily = YekanBakh)
                }
            }
        },
    )
}
