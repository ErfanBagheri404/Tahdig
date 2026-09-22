package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
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
    // Pantry items already at home: those rows get a «داری» badge and sink to the bottom
    // so the list reads as a shopping route, not a restatement of the cupboard.
    val pantryNames by viewModel.pantryItems.collectAsState()
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
            // Within an aisle, rows the pantry already covers sink to the bottom.
            val grouped = items
                .sortedBy { if (viewModel.inPantry(it.item, pantryNames)) 1 else 0 }
                .groupBy { com.erfanbagheri.tahdig.util.IngredientRegistry.aisleOf(it.item) }
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
                            inPantry = viewModel.inPantry(item.item, pantryNames),
                            onToggle = { checked -> viewModel.setChecked(item.id, checked) },
                            onDelete = { viewModel.remove(item.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItemEntity,
    inPantry: Boolean = false,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    // Optimistic local check so rapid taps never read a stale Room value.
    var localChecked by remember(item.id) { mutableStateOf<Boolean?>(null) }
    val checked = localChecked ?: item.isChecked

    Surface(
        // Faded, not hidden: the user asked for the item to be on the list, so it stays
        // visible and one tap from un-marking — it just stops demanding attention.
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (inPantry) 0.55f else 1f },
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
            if (inPantry) {
                Text(
                    text = "داری",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
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
