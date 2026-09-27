package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.components.oneA11yStop
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.MealPlanViewModel
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import com.erfanbagheri.tahdig.data.CalendarExport
import com.erfanbagheri.tahdig.util.PersianText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private val DAYS = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
private val MEALS = listOf("صبحانه", "ناهار", "شام")

/**
 * #83 — the plan is editable, not just generated.
 *
 * Drag-and-drop needs a library and is hostile to TalkBack, so a long-press
 * opens the same moves as a menu: move to another day, duplicate a day, clear
 * a cell. Zero new deps, and every action is reachable without a finger drag.
 * «تکرار هفتهٔ قبل» sits in the header because it targets the whole week, not
 * one cell.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MealPlanScreen(
    viewModel: MealPlanViewModel,
    onFoodClick: (Long) -> Unit = {},
    onAddPlanToShopping: () -> Unit = {},
) {
    val currentDay by viewModel.currentDay.collectAsState()
    val weekStart by viewModel.weekStart.collectAsState()
    val allPlan by viewModel.observeSlots(weekStart, currentDay).collectAsState()
    val foods by viewModel.foods.collectAsState()
    val lastAction by viewModel.lastAction.collectAsState()
    val pickerSlotState = remember { mutableStateOf<String?>(null) }
    val pickerSlot = pickerSlotState.value
    // Which cell's menu is open, and which cell is mid-move.
    val menuSlotState = remember { mutableStateOf<String?>(null) }
    val moveFromState = remember { mutableStateOf<String?>(null) }
    // Reorder inside the day: swap this cell with another meal's cell.
    val reorderFromState = remember { mutableStateOf<String?>(null) }

    // #84: export the whole plan to the device calendar. WRITE_CALENDAR is
    // asked HERE, on the first export — never on the first screen.
    val context = LocalContext.current
    val week by viewModel.week.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    fun exportMessage(ctx: android.content.Context, msg: String) {
        android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
    }
    suspend fun runExport(
        ctx: android.content.Context,
        vm: MealPlanViewModel,
        plan: List<com.erfanbagheri.tahdig.data.local.entity.MealPlanEntity>,
    ) {
        val foodMap = vm.foods.value.associateBy { it.id }
        val items = plan.mapNotNull { row ->
            foodMap[row.foodId]?.let { CalendarExport.Item(row.dayIndex, row.mealSlot, it.name) }
        }
        if (items.isEmpty()) {
            exportMessage(ctx, "اول برای چند وعده غذا انتخاب کن.")
            return
        }
        val outcome = withContext(Dispatchers.IO) { CalendarExport.exportWeek(ctx, items) }
        exportMessage(
            ctx,
            when (outcome) {
                is CalendarExport.Outcome.Done ->
                    "${PersianText.toPersianDigits(outcome.written)} وعده در تقویم ثبت شد"
                CalendarExport.Outcome.NoPermission ->
                    "اجازهٔ تقویم داده نشد — از تنظیمات گوشی می‌توانی بدهی."
                is CalendarExport.Outcome.Failed -> outcome.reason
            }
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.any { it }) scope.launch { runExport(context, viewModel, week) }
        else exportMessage(
            context,
            "برای نوشتن در تقویم باید اجازهٔ دسترسی بدهی — از تنظیمات گوشی می‌شود دوباره پرسید.",
        )
    }
    fun requestExport() {
        if (CalendarExport.hasPermission(context)) {
            scope.launch { runExport(context, viewModel, week) }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_CALENDAR,
                    android.Manifest.permission.WRITE_CALENDAR,
                )
            )
        }
    }

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

        Spacer(Modifier.height(8.dp))

        // Which week this grid IS. Without this the «تکرار هفتهٔ قبل» button has
        // no visible referent — "last week" of what?
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { viewModel.stepWeek(-1) }) {
                Text("هفتهٔ قبل", fontFamily = YekanBakh)
            }
            val anchor = LocalDate.ofEpochDay(weekStart)
            Text(
                text = PersianText.toPersianDigits(anchor.dayOfMonth.toString()) +
                    " " + FARSIC_MONTHS[anchor.monthValue - 1],
                style = MaterialTheme.typography.labelLarge,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            TextButton(onClick = { viewModel.stepWeek(1) }) {
                Text("هفتهٔ بعد", fontFamily = YekanBakh)
            }
        }

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

        // #84: same week, one tap, into the device calendar. Permission is
        // asked here on first use — with a Farsi reason, never a silent no-op.
        TextButton(
            onClick = { requestExport() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("افزودن کل هفته به تقویم گوشی", fontFamily = YekanBakh)
        }

        // #83: «تکرار هفتهٔ قبل» fills empty slots from last week — it never
        // overwrites a planned dish.
        TextButton(
            onClick = { viewModel.repeatLastWeek() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("تکرار هفتهٔ قبل", fontFamily = YekanBakh)
        }

        // The count is the honest answer: "copied" with nothing copied is a lie.
        lastAction?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        // #129: the selected day said nothing about being
                        // selected — colour was the only signal.
                        .semantics(mergeDescendants = true) {
                            contentDescription = day +
                                if (i == currentDay) "، روز انتخاب‌شده" else ""
                        }
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
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (foodName != null) onFoodClick(plan!!.foodId)
                                        else pickerSlotState.value = meal
                                    },
                                    onLongClick = { menuSlotState.value = meal },
                                    onLongClickLabel = "کارهای $meal",
                                )
                                // #129: slot + chosen dish were two stops; the
                                // pair is one decision.
                                .oneA11yStop("$meal، ${foodName ?: "انتخاب کنید"}"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = meal,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp),
                            )
                            Text(
                                text = foodName ?: "انتخاب کنید",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = YekanBakh,
                                color = if (foodName != null) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        }
                        // Editing is a menu, not a drag: TalkBack can open it and
                        // every action is a labelled row.
                        DropdownMenu(
                            expanded = menuSlotState.value == meal,
                            onDismissRequest = { menuSlotState.value = null },
                        ) {
                            if (plan != null) {
                                DropdownMenuItem(
                                    text = { Text("انتقال به روز…", fontFamily = YekanBakh) },
                                    onClick = {
                                        moveFromState.value = meal
                                        menuSlotState.value = null
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("جابه‌جایی با خانهٔ دیگر…", fontFamily = YekanBakh) },
                                    onClick = {
                                        reorderFromState.value = meal
                                        menuSlotState.value = null
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("تکرار روز", fontFamily = YekanBakh) },
                                    onClick = {
                                        viewModel.duplicateDay(currentDay)
                                        menuSlotState.value = null
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("پاک کردن خانه", fontFamily = YekanBakh) },
                                    onClick = {
                                        viewModel.clearFood(currentDay, meal)
                                        menuSlotState.value = null
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Move target: which day this dish lands on.
    moveFromState.value?.let { fromMeal ->
        AlertDialog(
            onDismissRequest = { moveFromState.value = null },
            title = { Text("انتقال $fromMeal به کدام روز؟", fontFamily = YekanBakh) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(DAYS.withIndex().toList(), key = { it.index }) { (i, day) ->
                        Text(
                            text = day,
                            fontFamily = YekanBakh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.moveSlot(currentDay, fromMeal, i, fromMeal)
                                    moveFromState.value = null
                                }
                                .padding(12.dp),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { moveFromState.value = null }) {
                    Text("لغو", fontFamily = YekanBakh)
                }
            },
        )
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

private val FARSIC_MONTHS = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر",
    "مرداد", "شهریور", "مهر", "آبان",
    "آذر", "دی", "بهمن", "اسفند",
)
