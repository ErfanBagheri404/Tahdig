package com.erfanbagheri.tahdig.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Passphrase prompt for the encrypted library bundle (#133).
 *
 * The key exists only for the duration of one pack/unpack call — it is never
 * stored, written, or logged. A `CharArray` (cleared on dismiss) rather than a
 * `String`, so a forgotten passphrase leaves nothing in memory to recover.
 *
 * @param creating true for "choose one" (export), false for "enter it" (import).
 */
@Composable
fun BundlePassphraseDialog(
    creating: Boolean,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var first by rememberSaveable { mutableStateOf("") }
    var second by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun submit() {
        when {
            first.length < 4 -> error = "گذرواژه باید دست‌کم ۴ نویسه باشد"
            creating && first != second -> error = "دو گذرواژه یکی نیستند"
            else -> {
                onConfirm(first.toCharArray())
                first = ""
                second = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (creating) "گذرواژهٔ بسته" else "بازیابی بسته")
        },
        text = {
            Column {
                Text(
                    if (creating) {
                        "این گذرواژه فقط برای باز کردن این بسته لازم است. " +
                            "جایی ذخیره نمی‌شود — اگر فراموشش کنید، بسته باز نخواهد شد."
                    } else {
                        "گذرواژه‌ای که موقع ساخت بسته انتخاب کردید را وارد کنید."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = first,
                    onValueChange = { first = it; error = null },
                    label = { Text(if (creating) "گذرواژه" else "گذرواژهٔ بسته") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (creating) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = second,
                        onValueChange = { second = it; error = null },
                        label = { Text("تکرار گذرواژه") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = ::submit) { Text("ادامه") } },
        dismissButton = {
            TextButton(onClick = {
                first = ""; second = ""
                onDismiss()
            }) { Text("انصراف") }
        },
    )
}
