package com.erfanbagheri.tahdig.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.util.ShareLayout

/**
 * Which card do you want to send? (#132)
 *
 * Three flat options instead of a preview grid: the rendered card is already
 * visible in the app, so what the user needs here is the *choice* of emphasis,
 * not a thumbnail. previews when the share is tapped, not here.
 */
@Composable
fun ShareLayoutDialog(
    onPick: (ShareLayout) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("کارت را چطور بفرستیم؟") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ShareLayout.entries.forEach { layout ->
                    OutlinedButton(
                        onClick = { onPick(layout) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(layout.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        },
    )
}
