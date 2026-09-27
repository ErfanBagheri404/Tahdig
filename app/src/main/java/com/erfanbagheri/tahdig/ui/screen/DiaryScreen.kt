package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.viewmodel.DiaryViewModel
import com.erfanbagheri.tahdig.util.DailyBudget
import com.erfanbagheri.tahdig.util.MealDiary
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.ReportCard
import com.erfanbagheri.tahdig.util.WeeklyReport

/**
 * Daily diary + weekly report tab (#114). Flat rows and hairline
 * separators — no rounded cards stacked on cards.
 */
@Composable
fun DiaryScreen(
    vm: DiaryViewModel,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val todayBySlot by vm.todayBySlot.collectAsState()
    val weekDays by vm.weekDays.collectAsState()
    val summary by vm.summary.collectAsState()
    val topDishes by vm.topDishes.collectAsState()
    val streak by vm.loggingStreak.collectAsState()
    val weightDeltaKg by vm.weightDeltaKg.collectAsState()
    val undo by vm.undo.collectAsState()
    val profile by vm.profile.collectAsState()
    val carryOverOn by vm.carryOver.collectAsState()

    // 0 means «بدون هدف»: the strip must not render a 0% bar that reads
    // like the user failed a target they never set.
    val dailyTarget = if (profile.hasGoal) DailyBudget.budget(profile) else 0

    // Unspent calories roll forward only when the toggle is on, and never
    // below zero — a day over budget must not shrink tomorrow's allowance.
    val carried = if (carryOverOn) {
        WeeklyReport.carryOver(
            dailyTarget,
            weekDays.lastOrNull()?.calories ?: 0,
            enabled = true,
        )
    } else {
        0
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // One-shot undo after a delete. Keyed on the row so a second delete
    // restarts the countdown instead of queuing two offers at once.
    LaunchedEffect(undo?.id) {
        val row = undo ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = row.foodName + " حذف شد",
            actionLabel = "برگردان",
            withDismissAction = true,
        )
        if (result == SnackbarResult.ActionPerformed) vm.undoRemove() else vm.clearUndo()
    }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                }
                Text("دفترچه وعده‌ها", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            DiaryProgressStrip(
                loggedCalories = weekDays.lastOrNull()?.calories ?: 0,
                target = dailyTarget,
                carried = carried,
            )

            // Slots with at least one row, in display order — an empty
            // breakfast reads as "nothing yet", not a blank section.
            val slots = MealDiary.visibleSlots(todayBySlot)
            if (slots.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "امروز هنوز چیزی ثبت نشده. از «خانه» یا «جستجو» یک غذا را به‌عنوان پخته ثبت کن.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                slots.forEach { slot ->
                    DiarySlotSection(
                        slot = slot,
                        rows = todayBySlot[slot].orEmpty(),
                        onMove = vm::moveToSlot,
                        onRescale = vm::setServings,
                        onRemove = vm::remove,
                        onSetTime = vm::setTime,
                    )
                }
            }

            WeeklySummaryCard(
                days = weekDays,
                summary = summary,
                streak = streak,
                topDishes = topDishes,
                weightDeltaKg = weightDeltaKg,
                // ShareCard's FileProvider path needs a Context; the canvas
                // render is off the UI thread by construction in ReportCard.
                onShare = summary?.let { s ->
                    {
                        ReportCard.share(context, s, topDishes)
                    }
                },
            )

            Spacer(Modifier.height(80.dp))
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}
