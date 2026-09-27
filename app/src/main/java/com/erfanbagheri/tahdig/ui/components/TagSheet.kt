package com.erfanbagheri.tahdig.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.TagViewModel

/**
 * #80 — the tag sheet: free-form Farsi entry with autocomplete over existing
 * tags, plus the dish's current tags as removable chips.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagSheet(
    viewModel: TagViewModel,
    foodId: Long,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val all by viewModel.allTags.collectAsState(initial = emptyList())
    val current by remember(foodId) { viewModel.observeFoodTags(foodId) }
        .collectAsState(initial = emptyList())
    var typed by remember { mutableStateOf("") }
    val attached = current.map { it.id }.toSet()
    val suggestions = viewModel.suggestions(all, typed).filter { it.id !in attached }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "برچسب‌های من",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "با کلمه‌های خودت دسته‌بندی کن: سریع، مهمونی، بچه‌ها دوست دارن",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            if (current.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    current.forEach { tag ->
                        TagChip(
                            label = tag.name,
                            onRemove = { viewModel.removeTag(foodId, tag.id) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("برچسب تازه…", fontFamily = YekanBakh) },
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (typed.isNotBlank()) {
                        TextButton(onClick = {
                            viewModel.addTag(foodId, typed)
                            typed = ""
                        }) { Text("افزودن", fontFamily = YekanBakh) }
                    }
                },
            )

            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    suggestions.take(12).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable(
                                role = Role.Button,
                                onClickLabel = "افزودن ${tag.name}",
                            ) {
                                viewModel.addTag(foodId, tag.name)
                                typed = ""
                            },
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("تمام", fontFamily = YekanBakh)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TagChip(label: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "برداشتن برچسب $label",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .height(16.dp)
                    .width(16.dp)
                    .clickable(onClick = onRemove, role = Role.Button),
            )
        }
    }
}
