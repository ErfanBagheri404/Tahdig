package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.background
import com.erfanbagheri.tahdig.ui.components.minTouchTarget
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.local.entity.NutritionLogEntity
import com.erfanbagheri.tahdig.util.MealTimeHelper
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.WeeklyReport
import com.erfanbagheri.tahdig.ui.components.oneA11yStop

/**
 * The diary slot list for today (#114). Flat rows with a hairline
 * separator, not nested cards — one row per log, slot name in the accent
 * colour, macros on the right.
 */
@Composable
fun DiarySlotSection(
    slot: String,
    rows: List<NutritionLogEntity>,
    onMove: (Long, String) -> Unit,
    onRescale: (Long, Double) -> Unit,
    onRemove: (Long) -> Unit,
    onSetTime: (Long, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(MealTimeHelper.farsiLabel(slot), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                PersianText.toPersianDigits(rows.size.toDouble()) + " مورد",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        rows.forEachIndexed { i, row ->
            if (i > 0) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                )
            }
            DiaryRow(row, onMove, onRescale, onRemove, onSetTime)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DiaryRow(
    row: NutritionLogEntity,
    onMove: (Long, String) -> Unit,
    onRescale: (Long, Double) -> Unit,
    onRemove: (Long) -> Unit,
    onSetTime: (Long, Int) -> Unit,
) {
    // The logged minute of day, derived once so the +/-15m buttons and the
    // time label read the same value instead of recomputing from the clock.
    val loggedLocal = remember(row.id, row.loggedAt) {
        java.time.Instant.ofEpochMilli(row.loggedAt)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.foodName, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
            Text(
                String.format("%02d:%02d", loggedLocal.hour, loggedLocal.minute) +
                    " · " + PersianText.toPersianDigits(row.calories.toDouble()) + " کیلوکالری" +
                    if (row.servings != 1.0) " · " + PersianText.toPersianDigits(row.servings) + " واحد" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        // ±15 min shift: a time picker is a dialog for a change that is
        // almost always "I logged this a bit late". The slot follows.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                // #129: 28dp visual, 48dp hit box. minTouchTarget grows only
                // the tap bounds, so the stepper keeps its tight layout.
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .minTouchTarget()
                    .clickable { onSetTime(row.id, -15) },
                contentAlignment = Alignment.Center,
            ) {
                Text("−۱۵", style = MaterialTheme.typography.labelSmall)
            }
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .minTouchTarget()
                    .clickable { onSetTime(row.id, 15) },
                contentAlignment = Alignment.Center,
            ) {
                Text("+۱۵", style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.width(8.dp))
        // Servings: a single stepper pair, no dropdown — two taps max.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .minTouchTarget()
                    .clickable { onRescale(row.id, row.servings + 0.5) },
                contentAlignment = Alignment.Center,
            ) {
                Text("+", style = MaterialTheme.typography.titleSmall)
            }
            Text(
                PersianText.toPersianDigits(row.servings),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(34.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .minTouchTarget()
                    .clickable { onRescale(row.id, (row.servings - 0.5).coerceAtLeast(0.5)) },
                contentAlignment = Alignment.Center,
            ) {
                Text("−", style = MaterialTheme.typography.titleSmall)
            }
        }
        Spacer(Modifier.width(8.dp))
        // Cycle slot: tapping the chip moves breakfast -> lunch -> snack -> dinner.
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable {
                    val next = MealTimeHelper.nextSlot(row.mealSlot)
                    onMove(row.id, next)
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                MealTimeHelper.farsiLabel(row.mealSlot),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.width(4.dp))
        Box(
            // #129: this is a delete control. 32dp visual, 48dp hit box.
            Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .minTouchTarget()
            .clickable { onRemove(row.id) },
            contentAlignment = Alignment.Center,
        ) {
            Text("×", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

/**
 * Seven-day bar row + the averages card (#114). Bars scale to the busiest
 * day, not to a fixed kcal ceiling, so a light week doesn't render as a
 * row of stubs.
 */
@Composable
fun WeeklySummaryCard(
    days: List<WeeklyReport.DayTotals>,
    summary: WeeklyReport.Summary?,
    streak: Int,
    topDishes: List<Pair<String, Int>> = emptyList(),
    weightDeltaKg: Double? = null,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return
    val maxCal = days.maxOf { it.calories }.coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("گزارش هفتگی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (summary != null && onShare != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { onShare() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        "اشتراک",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { d ->
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (d.logged) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((12 + (d.calories.toFloat() / maxCal) * 48).dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        PersianText.toPersianDigits(d.date.dayOfMonth.toDouble()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (summary == null) {
            Text(
                "این هفته هنوز چیزی ثبت نشده",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat("میانگین کالری", summary.avgCalories)
                SummaryStat("پروتئین", summary.avgProtein)
                SummaryStat("چربی", summary.avgFat)
                SummaryStat("کربوهیدرات", summary.avgCarbs)
            }
            Spacer(Modifier.height(6.dp))
            // Weight delta rides the totals line ("… · وزن −۰٫۳ کیلو") — a
            // nullable Double, never a 0.0 that reads like "held steady".
            // The sign comes from the value: ↓ for a loss, ↑ for a gain.
            val deltaLine = if (weightDeltaKg == null) {
                ""
            } else {
                val shown = PersianText.toPersianDigits(kotlin.math.abs(weightDeltaKg))
                " · وزن ${if (weightDeltaKg <= 0) "↓" else "↑"}$shown کیلو"
            }
            Text(
                "مجموع " + PersianText.toPersianDigits(summary.totalCalories.toDouble()) +
                    " کیلوکالری در " + PersianText.toPersianDigits(summary.loggedDays.toDouble()) + " روز" +
                    (if (streak > 0) " · پیاپی " + PersianText.toPersianDigits(streak.toDouble()) + " روز" else "") +
                    deltaLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Top dishes ride the summary card (#114) — the report is one
            // block, not a stat row plus a separate list further down.
            if (topDishes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "پرتکرارترین‌های هفته",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                topDishes.forEachIndexed { i, (name, count) ->
                    Text(
                        PersianText.toPersianDigits((i + 1).toDouble()) + ". " + name +
                            " — " + PersianText.toPersianDigits(count.toDouble()) + " بار",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(PersianText.toPersianDigits(value.toDouble()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The target progress strip; no goal renders «بدون هدف», not a 0% bar. */
@Composable
fun DiaryProgressStrip(loggedCalories: Int, target: Int, carried: Int, modifier: Modifier = Modifier) {
    val ratio = if (target <= 0) 0f else (loggedCalories.toFloat() / target).coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.height(4.dp))
        val line = if (target <= 0) {
            PersianText.toPersianDigits(loggedCalories.toDouble()) + " کیلوکالری امروز"
        } else {
            PersianText.toPersianDigits(loggedCalories.toDouble()) + " از " +
                PersianText.toPersianDigits(target.toDouble()) + " کیلوکالری"
        }
        Text(
            line + if (carried > 0) {
                " · +" + PersianText.toPersianDigits(carried.toDouble()) + " فردا"
            } else {
                ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
