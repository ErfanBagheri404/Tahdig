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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.ui.viewmodel.CookHeatmapViewModel
import java.time.DayOfWeek
import java.time.YearMonth

/** Days of week in Farsi, starting from Saturday (Iran). */
private val WEEKDAYS = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

/** Count thresholds for color intensity — four tiers. */
private val TIERS = listOf(0, 1, 2, 4) // 0=none, 1=light, 2=medium, 4=dark

@Composable
fun CookHeatmapScreen(
    viewModel: CookHeatmapViewModel,
    onBack: () -> Unit = {},
) {
    val ym by viewModel.month.collectAsState()
    val counts by viewModel.counts.collectAsState()
    val total by viewModel.totalThisMonth.collectAsState()
    val streak by viewModel.longestStreak.collectAsState()

    // Build the grid: pad leading days (Sat-based), then the month's days
    val firstDayOfWeek = ym.atDay(1).dayOfWeek
    // Saturday=6 in java.time (Mon=1..Sun=7), shift to Sat=0..Fri=6
    val offset = (firstDayOfWeek.value + 1) % 7 // Sat→0, Sun→1, Mon→2 ...
    val daysInMonth = ym.lengthOfMonth()
    val cells = List(offset) { null } + (1..daysInMonth).toList()

    val monthLabel = "${PersianText.toPersianDigits(ym.monthValue)} ${monthName(ym.monthValue)}"
    val yearLabel = PersianText.toPersianDigits(ym.year.toString())

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                }
                Text(
                    text = "سابقه پخت",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = YekanBakh,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Month navigator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = { viewModel.prevMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "ماه قبل")
                }
                Text(
                    text = "$monthLabel\n$yearLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = YekanBakh,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(140.dp),
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "ماه بعد")
                }
            }

            Spacer(Modifier.height(20.dp))

            // Weekday headers
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                WEEKDAYS.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Day grid — 7 columns, chunk into rows
            val chunked = cells.chunked(7)
            chunked.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { day ->
                        if (day == null) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            val count = counts[day] ?: 0
                            val tier = TIERS.indexOfLast { count >= it }.coerceAtLeast(0)
                            val color = tierColor(tier, MaterialTheme.colorScheme)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color)
                                    .height(36.dp),
                            ) {
                                Text(
                                    text = PersianText.toPersianDigits(day.toString()),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = YekanBakh,
                                    color = if (tier >= 2) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Stats
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatItem(
                        label = "این ماه",
                        value = PersianText.toPersianDigits(total.toString()),
                    )
                    StatItem(
                        label = "طولانی‌ترین رشته",
                        value = "${PersianText.toPersianDigits(streak.toString())} روز",
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = YekanBakh,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 4-tier color: none → light → medium → dark. */
@Composable
private fun tierColor(tier: Int, colors: androidx.compose.material3.ColorScheme) = when (tier) {
    0 -> colors.surfaceVariant
    1 -> colors.secondaryContainer.copy(alpha = 0.5f)
    2 -> colors.secondaryContainer
    else -> colors.primary
}

/** Farsi month names. */
private fun monthName(m: Int): String = when (m) {
    1 -> "ژانویه"; 2 -> "فوریه"; 3 -> "مارس"; 4 -> "آوریل"
    5 -> "مه"; 6 -> "ژوئن"; 7 -> "ژوئیه"; 8 -> "اوت"
    9 -> "سپتامبر"; 10 -> "اکتبر"; 11 -> "نوامبر"; 12 -> "دسامبر"
    else -> ""
}
