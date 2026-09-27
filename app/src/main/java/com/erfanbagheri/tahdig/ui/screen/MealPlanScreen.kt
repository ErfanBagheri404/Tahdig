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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.MealPlanViewModel

private val DAYS = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
private val MEALS = listOf("صبحانه", "ناهار", "شام")

@Composable
fun MealPlanScreen(
    viewModel: MealPlanViewModel,
    onFoodClick: (Long) -> Unit = {},
    onAddPlanToShopping: () -> Unit = {},
) {
    val currentDay by viewModel.currentDay.collectAsState()
    val allPlan by viewModel.observeSlots(currentDay).collectAsState()
    val foods by viewModel.foods.collectAsState()
    val pickerSlotState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val pickerSlot = pickerSlotState.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = "برنامه غذایی هفتگی",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = YekanBakh,
        )

        Spacer(Modifier.height(16.dp))

        // Week-wide action: everything planned, on the shopping list in one tap.
        TextButton(
            onClick = onAddPlanToShopping,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("افزودن کل هفته به لیست خرید", fontFamily = YekanBakh)
        }

        // Auto-fill: only empty slots by default; long-press to regenerate the whole week.
        val generating by viewModel.generating.collectAsState()
        val canUndo by viewModel.canUndo.collectAsState()
        val fillModifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { viewModel.generateWeek(regenerateAll = false) },
                onLongPress = { viewModel.generateWeek(regenerateAll = true) },
            )
        }
        TextButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().then(fillModifier),
        ) {
            Text(
                text = if (generating) "داره می‌چینه…" else "🎲 پر کردن خودکار هفته",
                fontFamily = YekanBakh,
            )
        }
        if (canUndo) {
            Text(
                text = "بازگردانی برنامه قبلی",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = viewModel::undoGenerate)
                    .padding(vertical = 6.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Day tabs
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DAYS.forEachIndexed { i, day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = YekanBakh,
                    color = if (i == currentDay) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { viewModel.setDay(i) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MEALS, key = { it }) { meal ->
                val plan = allPlan.find { it.dayIndex == currentDay && it.mealSlot == meal }
                val foodName = plan?.let { p -> foods.find { it.id == p.foodId }?.name }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (foodName != null) onFoodClick(plan!!.foodId)
                                else pickerSlotState.value = meal
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = meal,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = foodName ?: "انتخاب کنید",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = YekanBakh,
                            color = if (foodName != null) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }

    // Food picker dialog
    pickerSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { pickerSlotState.value = null },
            title = { Text("انتخاب غذا برای $slot", fontFamily = YekanBakh) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(foods, key = { it.id }) { food ->
                        Text(
                            text = food.name,
                            fontFamily = YekanBakh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.assignFood(currentDay, slot, food.id)
                                    pickerSlotState.value = null
                                }
                                .padding(12.dp),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pickerSlotState.value = null }) {
                    Text("لغو", fontFamily = YekanBakh)
                }
            },
        )
    }
}
