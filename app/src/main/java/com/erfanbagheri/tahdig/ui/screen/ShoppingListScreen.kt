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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.local.entity.ShoppingItemEntity
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.ui.components.GhostRowsEmptyState
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.AislePlanner
import com.erfanbagheri.tahdig.ui.viewmodel.ShoppingViewModel

@Composable
fun ShoppingListScreen(
    viewModel: ShoppingViewModel,
    onGoHome: () -> Unit = {},
) {
    val items by viewModel.items.collectAsState()
    // Pantry items already at home: those rows get a «داری» badge and sink to the bottom
    // so the list reads as a shopping route, not a restatement of the cupboard.
    val pantryNames by viewModel.pantryItems.collectAsState()
    val tripActive by SettingsStore.tripActive.collectAsState()
    val aisleOrder by SettingsStore.aisleOrder.collectAsState()
    val aisleRenames by SettingsStore.aisleRenames.collectAsState()
    val aisleHidden by SettingsStore.aisleHidden.collectAsState()
    val checkedCount = items.count { it.isChecked }
    var showAisleManager by remember { mutableStateOf(false) }
    var showTripEnd by remember { mutableStateOf(false) }

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
            if (checkedCount > 0 && !tripActive) {
                TextButton(onClick = { viewModel.clearChecked() }) {
                    Text("حذف انجام‌شده‌ها", fontFamily = YekanBakh)
                }
            }
            if (tripActive) {
                TextButton(onClick = { showTripEnd = true }) {
                    Text("پایان", fontFamily = YekanBakh, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = { viewModel.startTrip() }) {
                    Text("شروع سفر خرید", fontFamily = YekanBakh)
                }
            }
            TextButton(onClick = { showAisleManager = true }) {
                Text("مسیر", fontFamily = YekanBakh)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (items.isEmpty()) {
            // #126: ghost rows + one CTA instead of a bare sentence.
            GhostRowsEmptyState(
                title = "لیست خرید خالیه",
                subtitle = "از صفحه هر غذا، مواد لازم رو اضافه کن",
                cta = "دیدن پیشنهاد امروز",
                onCta = onGoHome,
                modifier = Modifier.padding(top = 32.dp),
            )
        } else {
            // Grouped by shopping aisle so the list follows the route through a store.
            // Grouping the entities directly (not the parsed text) keeps each row's id.
            // Within an aisle, rows the pantry already covers sink to the bottom.
            val sections = com.erfanbagheri.tahdig.util.AislePlanner.plan(
                groups = items
                    .sortedBy { if (viewModel.inPantry(it.item, pantryNames)) 1 else 0 }
                    .groupBy { com.erfanbagheri.tahdig.util.IngredientRegistry.aisleOf(it.item) },
                order = aisleOrder,
                renames = aisleRenames,
                hidden = aisleHidden,
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                sections.forEach { sec ->
                    val bucket = sec.header
                    val rows = sec.rows
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
                        // #127: swipe right = tick off, swipe left = delete —
                        // and both are labelled entries in the row's menu, so
                        // the gesture is never the only way to reach them.
                        com.erfanbagheri.tahdig.ui.components.SwipeActionRow(
                            swipeLeft = com.erfanbagheri.tahdig.ui.components.RowAction(
                                label = if (item.isChecked) "برگردون به نخریده" else "خریدم",
                                icon = Icons.Default.Check,
                                onClick = { viewModel.setChecked(item.id, !item.isChecked) },
                            ),
                            swipeRight = com.erfanbagheri.tahdig.ui.components.RowAction(
                                label = "حذف",
                                icon = Icons.Default.Delete,
                                onClick = { viewModel.remove(item.id) },
                                destructive = true,
                            ),
                        ) {
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

    if (showAisleManager) {
        AisleManagerSheet(onDismiss = { showAisleManager = false })
    }
    if (showTripEnd) {
        TripEndDialog(
            onDismiss = { showTripEnd = false },
            onConfirm = {
                viewModel.endTrip()
                showTripEnd = false
            },
        )
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
            // #127: the delete button moved into the row's overflow menu, so
            // the gesture and the menu share one labelled path instead of
            // showing a bare trash icon with no way back.
            Spacer(Modifier.width(4.dp))
        }
    }
}


/**
 * Aisle manager (#108): rename (stored as an override on the canonical key),
 * reorder (up/down within the store-route list), hide (folded into "سایر").
 * Every change persists immediately — there is no save state to lose.
 */
@Composable
private fun AisleManagerSheet(onDismiss: () -> Unit) {
    var order by remember { mutableStateOf(SettingsStore.aisleOrder.value.ifEmpty { AislePlanner.KNOWN }) }
    var renames by remember { mutableStateOf(SettingsStore.aisleRenames.value) }
    var hidden by remember { mutableStateOf(SettingsStore.aisleHidden.value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مسیر خرید", fontFamily = YekanBakh) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                order.forEachIndexed { index, key ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = renames[key]?.takeIf { it.isNotBlank() } ?: key,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = YekanBakh,
                            color = if (key in hidden) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            enabled = index > 0,
                            onClick = {
                                val next = order.toMutableList()
                                next[index] = next[index - 1].also { next[index - 1] = next[index] }
                                order = next
                            },
                        ) { Text("▲", fontFamily = YekanBakh) }
                        IconButton(
                            enabled = index < order.lastIndex,
                            onClick = {
                                val next = order.toMutableList()
                                next[index] = next[index + 1].also { next[index + 1] = next[index] }
                                order = next
                            },
                        ) { Text("▼", fontFamily = YekanBakh) }
                        TextButton(onClick = {
                            hidden = if (key in hidden) hidden - key else hidden + key
                        }) {
                            Text(
                                if (key in hidden) "پنهان" else "نمایش",
                                fontFamily = YekanBakh,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    // One-line rename: the placeholder shows the current name.
                    OutlinedTextField(
                        value = renames[key] ?: "",
                        onValueChange = { text ->
                            renames = if (text.isBlank() || text == key) {
                                renames - key
                            } else {
                                renames + (key to text)
                            }
                        },
                        singleLine = true,
                        label = { Text("نام جدید", fontFamily = YekanBakh) },
                        placeholder = { Text(key, fontFamily = YekanBakh) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                SettingsStore.setAisleConfig(order, renames, hidden)
                onDismiss()
            }) { Text("ذخیره", fontFamily = YekanBakh) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("لغو", fontFamily = YekanBakh) }
        },
    )
}

/**
 * End-trip confirmation (#108): archives date + counts + a row snapshot for
 * history, then clears the list. Cancel keeps the trip running.
 */
@Composable
private fun TripEndDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("پایان سفر خرید؟", fontFamily = YekanBakh) },
        text = {
            Text(
                "لیست ثبت و پاک می‌شه. توی تاریخچه می‌مونه.",
                fontFamily = YekanBakh,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("ثبت و پایان", fontFamily = YekanBakh, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ادامه", fontFamily = YekanBakh) }
        },
    )
}
