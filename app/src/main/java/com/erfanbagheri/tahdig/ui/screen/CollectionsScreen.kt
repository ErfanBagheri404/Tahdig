package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.LocalHairline
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.CollectionViewModel
import com.erfanbagheri.tahdig.util.CollectionMath
import kotlinx.coroutines.launch

/**
 * «مجموعه‌ها» (#79): create / rename / delete named groups, one flat row each
 * with its dish count, hairline-separated. Deleting here only drops the
 * collection and its joins — the dishes live on in «علاقه‌مندی‌ها».
 */
@Composable
fun CollectionsScreen(
    viewModel: CollectionViewModel,
    onBack: () -> Unit,
) {
    val counts by viewModel.counts.collectAsState()
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Long?>(null) }
    var deleteTarget by remember { mutableStateOf<Long?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                }
                Text(
                    text = "مجموعه‌ها",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = YekanBakh,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { creating = true }) {
                    Icon(Icons.Default.Add, contentDescription = "ساخت مجموعه")
                }
            }
            HorizontalDivider(thickness = LocalHairline.current)

            LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
                items(counts, key = { it.id }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = CollectionMath.countLabel(row.count),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { renameTarget = row.id }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "تغییر نام «${row.name}»",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { deleteTarget = row.id }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "حذف «${row.name}»",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        NameDialog(
            title = "مجموعهٔ جدید",
            confirmLabel = "ساخت",
            onConfirm = {
                scope.launch { viewModel.create(it) }
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
    renameTarget?.let { id ->
        NameDialog(
            title = "تغییر نام",
            initial = counts.firstOrNull { it.id == id }?.name.orEmpty(),
            confirmLabel = "ذخیره",
            onConfirm = {
                scope.launch { viewModel.rename(id, it) }
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }
    deleteTarget?.let { id ->
        val name = counts.firstOrNull { it.id == id }?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف مجموعه", fontFamily = YekanBakh) },
            text = {
                Text(
                    "«$name» حذف می‌شه؛ غذاهایش در علاقه‌مندی‌ها می‌مونن.",
                    fontFamily = YekanBakh,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(id)
                    deleteTarget = null
                }) { Text("حذف", fontFamily = YekanBakh) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("انصراف", fontFamily = YekanBakh)
                }
            },
        )
    }
}

/** One text field, shared by create and rename. Blank / taken names are refused. */
@Composable
internal fun NameDialog(
    title: String,
    initial: String = "",
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(TextFieldValue(initial)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontFamily = YekanBakh) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("نام مجموعه", fontFamily = YekanBakh) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.text) }) {
                Text(confirmLabel, fontFamily = YekanBakh)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", fontFamily = YekanBakh) }
        },
    )
}
