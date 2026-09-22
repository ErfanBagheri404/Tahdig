package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.local.entity.ShoppingItemEntity
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.ShoppingViewModel

@Composable
fun ShoppingListScreen(
    viewModel: ShoppingViewModel,
) {
    val items by viewModel.items.collectAsState()
    val checkedCount = items.count { it.isChecked }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(48.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "لیست خرید",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (checkedCount > 0) {
                TextButton(onClick = { viewModel.clearChecked() }) {
                    Text("حذف انجام‌شده‌ها", fontFamily = YekanBakh)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (items.isEmpty()) {
            Text(
                text = "لیست خرید خالیه\nاز صفحه غذا، مواد لازم رو اضافه کن",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
            )
        } else {
            // Grouped by shopping aisle so the list follows the route through a store.
            // Grouping the entities directly (not the parsed text) keeps each row's id.
            val grouped = items.groupBy { com.erfanbagheri.tahdig.util.IngredientParser.categoryOf(it.item) }
                .toList()
                .sortedBy { (bucket, _) -> if (bucket == "سایر") 1 else 0 }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                grouped.forEach { (bucket, rows) ->
                    item(key = "hdr-$bucket") {
                        Text(
                            text = bucket,
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
                        )
                    }
                    items(rows, key = { it.id }) { item ->
                        ShoppingRow(
                            item = item,
                            onToggle = { checked -> viewModel.setChecked(item.id, checked) },
                            onDelete = { viewModel.remove(item.id) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingRow(
    item: ShoppingItemEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    // Optimistic local check so rapid taps never read a stale Room value.
    var localChecked by remember(item.id) { mutableStateOf<Boolean?>(null) }
    val checked = localChecked ?: item.isChecked

    // Swipe is an accelerator for the delete the row already exposes: both directions
    // fire the same onDelete, and the visible trash button keeps the identical action
    // reachable for anyone who does not (or cannot) swipe. Returning false snaps the row
    // back immediately — removal comes from Room, and the undo snackbar reverses it.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDelete()
                false
            } else false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { target ->
                    localChecked = target
                    onToggle(target)
                },
            )
            Text(
                text = item.item,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = YekanBakh,
                color = if (checked)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
                textDecoration = if (checked) TextDecoration.LineThrough else null,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        localChecked = !checked
                        onToggle(!checked)
                    },
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
    }
}
