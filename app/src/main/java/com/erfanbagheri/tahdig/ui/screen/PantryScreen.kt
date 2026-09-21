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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.components.DishThumb
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.PantryViewModel

/**
 * «چی دارم؟» — pantry staples and what they can cook right now.
 */
@Composable
fun PantryScreen(
    viewModel: PantryViewModel,
    onFoodClick: (Long) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val draft by viewModel.draft.collectAsState()
    val items by viewModel.items.collectAsState()
    val ranked by viewModel.ranked.collectAsState()

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
                    if (fullMatches.isNotEmpty()) {
                        item {
                            SectionHeader("می‌تونی همین حالا بپزی (${fullMatches.size})")
                        }
                        items(fullMatches, key = { it.food.id }) { dish ->
                            PantryDishRow(
                                dish = dish,
                                fullyCovered = true,
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
                                onClick = { onFoodClick(dish.food.id) },
                            )
                        }
                    }
                }
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
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
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
    onClick: () -> Unit,
) {
    val food = dish.food
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                Text(
                    text = if (fullyCovered) "همه مواد موجوده" else "$pct٪ مواد موجوده",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
