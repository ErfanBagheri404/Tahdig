package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.local.entity.TagEntity
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.TagViewModel
import com.erfanbagheri.tahdig.util.PersianText
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * #80 — the Tags manager. Rename and delete; deleting drops joins only, so no
 * dish ever disappears from the library.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsManagerScreen(
    viewModel: TagViewModel,
    onBack: () -> Unit,
) {
    val tags by viewModel.allTags.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<TagEntity?>(null) }
    var draft by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<TagEntity?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("برچسب‌های من", fontFamily = YekanBakh) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
            )

            if (tags.isEmpty()) {
                Text(
                    text = "هنوز برچسبی نساختی. از صفحهٔ جستجو یا یک غذا برچسب بساز.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
                return@Column
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tags, key = { it.id }) { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = tag.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { editing = tag; draft = tag.name }) {
                            Icon(Icons.Default.Edit, contentDescription = "تغییر نام ${tag.name}")
                        }
                        IconButton(onClick = { deleting = tag }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "حذف برچسب ${tag.name}",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    editing?.let { tag ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("تغییر نام برچسب", fontFamily = YekanBakh) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    label = { Text("نام", fontFamily = YekanBakh) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameTag(tag.id, draft)
                    editing = null
                }) { Text("ذخیره", fontFamily = YekanBakh) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text("انصراف", fontFamily = YekanBakh) }
            },
        )
    }

    deleting?.let { tag ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف برچسب", fontFamily = YekanBakh) },
            text = {
                Text(
                    text = "«${tag.name}» از همهٔ غذاها برداشته می‌شود. خود غذاها دست‌نخورده می‌مانند.",
                    fontFamily = YekanBakh,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTag(tag.id)
                    deleting = null
                }) { Text("حذف", fontFamily = YekanBakh) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("انصراف", fontFamily = YekanBakh) }
            },
        )
    }
}
