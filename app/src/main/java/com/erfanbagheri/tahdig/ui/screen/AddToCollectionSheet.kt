package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.LocalHairline
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.CollectionViewModel

/**
 * «افزودن به مجموعه» (#79): long-press a dish → this sheet. Every collection
 * is a row with a checkmark when the dish is already in it; tapping toggles,
 * so one dish can join several collections in a single pass.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCollectionSheet(
    viewModel: CollectionViewModel,
    foodId: Long,
    onDismiss: () -> Unit,
) {
    val counts by viewModel.counts.collectAsState()
    val members by viewModel.memberIds(foodId).collectAsState(initial = emptyList())
    var creating by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "افزودن به مجموعه",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(thickness = LocalHairline.current)
            if (counts.isEmpty()) {
                Text(
                    text = "هنوز مجموعه‌ای نیست؛ یکی بساز.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            LazyColumn {
                items(counts, key = { it.id }) { row ->
                    val inIt = row.id in members
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggle(row.id, foodId) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        if (inIt) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            HorizontalDivider(thickness = LocalHairline.current)
            TextButton(onClick = { creating = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("  مجموعهٔ جدید", fontFamily = YekanBakh)
            }
        }
    }

    if (creating) {
        NameDialog(
            title = "مجموعهٔ جدید",
            confirmLabel = "ساخت و افزودن",
            onConfirm = {
                viewModel.createAndAdd(it, foodId)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
}
