package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.background
import androidx.compose.ui.semantics.contentDescription
import com.erfanbagheri.tahdig.ui.components.minTouchTarget
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ExperimentalMaterial3Api as ExpM3
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.local.entity.PantryItemEntity
import com.erfanbagheri.tahdig.util.ExpiryMath
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.ui.components.DishThumb
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.PantryViewModel
import com.erfanbagheri.tahdig.ui.components.oneA11yStop

/**
 * «چی دارم؟» — pantry staples and what they can cook right now.
 */
@Composable
fun PantryScreen(
    viewModel: PantryViewModel,
    onFoodClick: (Long) -> Unit = {},
    onBack: () -> Unit = {},
    onOpenLeftover: () -> Unit = {},
    /** Swipe on an expiring row replaces the item with a shopping row (#106). */
    onAddToShopping: (PantryItemEntity) -> Unit = {},
) {
    val draft by viewModel.draft.collectAsState()
    val items by viewModel.items.collectAsState()
    val ranked by viewModel.ranked.collectAsState()
    val expiring by viewModel.expiring.collectAsState()
    // AC: the TOP result alone gets the «قبل از خراب شدن» badge, and only when
    // an expiring staple actually lifted it.
    val topDish = ranked.firstOrNull()
    var editingExpiry by remember { mutableStateOf<PantryItemEntity?>(null) }

    val fullMatches = ranked.filter { it.coverage >= 1f }
    val partialMatches = ranked.filter { it.coverage < 1f }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "چی دارم؟",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = YekanBakh,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "موادی که خونه داری رو بنویس، ببینم چی می‌تونی بپزی",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (items.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearAll() }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "پاک کردن همه",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = onOpenLeftover) {
                    Text("غذای مونده دارم", fontFamily = YekanBakh)
                }
                TextButton(onClick = onBack) {
                    Text("بازگشت", fontFamily = YekanBakh)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = viewModel::onDraftChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("مثلاً: پیاز، برنج، لوبیا", fontFamily = YekanBakh) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.addDraft() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.addDraft() },
                    enabled = draft.isNotBlank(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "افزودن")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Pantry chips
            if (items.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(items, key = { it.id }) { it2 ->
                        PantryChip(label = it2.item, onRemove = { viewModel.remove(it2.id) })
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (items.isEmpty()) {
                Text(
                    text = "هنوز چیزی اضافه نکردی",
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp, end = 20.dp, bottom = 24.dp,
                    ),
                ) {
                    // «رو به اتمام» (#106): soonest first, red band under 3 days.
                    if (expiring.isNotEmpty()) {
                        item {
                            SectionHeader("رو به اتمام (${expiring.size})")
                        }
                        items(expiring, key = { "exp_${it.id}" }) { pantryItem ->
                            ExpiringRow(
                                item = pantryItem,
                                onTap = { editingExpiry = pantryItem },
                                onShop = {
                                    // Conceptual replace: buy a new one, drop the old row.
                                    onAddToShopping(pantryItem)
                                    viewModel.remove(pantryItem.id)
                                },
                            )
                        }
                    }
                    if (fullMatches.isNotEmpty()) {
                        item {
                            SectionHeader("می‌تونی همین حالا بپزی (${fullMatches.size})")
                        }
                        items(fullMatches, key = { it.food.id }) { dish ->
                            PantryDishRow(
                                dish = dish,
                                fullyCovered = true,
                                badge = dish === topDish && dish.expiryBoost > 1.0,
                                onClick = { onFoodClick(dish.food.id) },
                            )
                        }
                    }
                    if (partialMatches.isNotEmpty()) {
                        item {
                            SectionHeader("با کمی خرید (${partialMatches.size})")
                        }
                        items(partialMatches, key = { it.food.id }) { dish ->
                            PantryDishRow(
                                dish = dish,
                                fullyCovered = false,
                                badge = dish === topDish && dish.expiryBoost > 1.0,
                                onClick = { onFoodClick(dish.food.id) },
                            )
                        }
                    }
                }
            }

            // «tap -> edit date» (#106); «بدون تاریخ» clears back to undated.
            editingExpiry?.let { editing ->
                ExpiryDatePicker(
                    item = editing,
                    onDismiss = { editingExpiry = null },
                    onConfirm = { millis ->
                        viewModel.setExpiry(editing.id, millis)
                        editingExpiry = null
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontFamily = YekanBakh,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun PantryChip(label: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp).minTouchTarget()) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "حذف $label",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PantryDishRow(
    dish: com.erfanbagheri.tahdig.ui.viewmodel.CookableDish,
    fullyCovered: Boolean,
    badge: Boolean = false,
    onClick: () -> Unit,
) {
    val food = dish.food
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // #129: dish + coverage in one stop. Coverage is the reason the row
            // is on screen, so it is spoken rather than left to the badge glyph.
            .oneA11yStop(
                food.name + if (fullyCovered) "، همهٔ مواد را داری" else ""
            )
            .padding(vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DishThumb(
                imageUrl = food.imageUrl,
                categoryId = food.categoryId,
                size = 48.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val pct = (dish.coverage * 100).toInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (fullyCovered) "همه مواد موجوده" else "$pct٪ مواد موجوده",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // AC: the top-ranked dish alone wears this (#106).
                    if (badge) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = "قبل از خراب شدن",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
            }
            if (food.prepTimeMin > 0) {
                Text(
                    text = "${food.prepTimeMin} دقیقه",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // Hairline separator, per the app's flat list style
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

/**
 * One «رو به اتمام» row (#106): days-to-expiry label, red hairline band under
 * 3 days, swipe either way = add to shopping (conceptual replace) and the row
 * leaves the pantry. Tap edits the date.
 */
@OptIn(ExpM3::class)
@Composable
private fun ExpiringRow(
    item: PantryItemEntity,
    onTap: () -> Unit,
    onShop: () -> Unit,
) {
    val now = System.currentTimeMillis()
    val days = ExpiryMath.daysTo(item.expiresAt, now)
    val urgent = ExpiryMath.isUrgent(days)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            val swiped = value == SwipeToDismissBoxValue.EndToStart ||
                value == SwipeToDismissBoxValue.StartToEnd
            if (swiped) onShop()
            swiped
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "برو توی لیست خرید",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
    ) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTap)
                    // #129: item name + expiry were two stops, and the expiry
                    // is the whole reason the row is in the expiry list.
                    .oneA11yStop(
                        item.item + "، " + when {
                            days == null -> "بدون تاریخ"
                            days < 0 -> "گذشته"
                            days == 0 -> "امروز"
                            else -> "${PersianText.toPersianDigits(days.toString())} روز مانده"
                        }
                    )
                    .padding(vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.item,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = when {
                            days == null -> "بدون تاریخ"
                            days < 0 -> "گذشته"
                            days == 0 -> "امروز"
                            else -> "${PersianText.toPersianDigits(days.toString())} روز مانده"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = YekanBakh,
                        fontWeight = FontWeight.Bold,
                        color = if (urgent) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                // The urgency band: red under 3 days, hairline otherwise (#106 AC).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (urgent) 3.dp else 1.dp)
                        .background(
                            if (urgent) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
        }
    }
}

/** Date editor for one pantry row (#106); «بدون تاریخ» clears back to undated. */
@OptIn(ExpM3::class)
@Composable
private fun ExpiryDatePicker(
    item: PantryItemEntity,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
) {
    val initial = item.expiresAt ?: (item.addedAt + 7L * 86_400_000L)
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val picked = state.selectedDateMillis ?: return@TextButton
                // End-of-day local: the chosen date stays "that day" the whole 24h.
                val endOfDay = java.util.Calendar.getInstance().apply {
                    timeInMillis = picked
                    set(java.util.Calendar.HOUR_OF_DAY, 23)
                    set(java.util.Calendar.MINUTE, 59)
                    set(java.util.Calendar.SECOND, 59)
                }.timeInMillis
                onConfirm(endOfDay)
            }) { Text("ثبت", fontFamily = YekanBakh) }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(null) }) { Text("بدون تاریخ", fontFamily = YekanBakh) }
        },
    ) {
        Column {
            Text(
                text = "تاریخ انقضای ${item.item}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
            DatePicker(state = state)
        }
    }
}
