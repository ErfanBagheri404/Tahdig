package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.WaterMath
import java.time.LocalDate

/**
 * Home water card (#115): a glass stepper, today's total against the target,
 * and a flat 7-day bar row. No chart library — the ring and bars are a few
 * lines of Canvas each.
 */
@Composable
fun WaterCard(
    consumedMl: Int?,
    targetMl: Int?,
    week: List<Pair<LocalDate, Int>>,
    glassSizeMl: Int,
    onAdd: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val target = targetMl
    val progress = WaterMath.progress(consumedMl ?: 0, target)
    // Absent is not zero: a day with no row yet shows the goal line but no
    // "0 of N" claim about a measurement that was never taken.
    val mlText = consumedMl?.let { PersianText.toPersianDigits(it) } ?: "—"

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("آب", fontFamily = YekanBakh, fontWeight = FontWeight.Bold)
                Text(
                    text = if (target != null) "$mlText از ${PersianText.toPersianDigits(target)} میلی‌لیتر"
                    else "$mlText میلی‌لیتر",
                    fontFamily = YekanBakh,
                    color = colors.onSurfaceVariant,
                )
            }
            WaterRing(
                progress = progress,
                color = colors.primary,
                track = colors.surfaceVariant,
                labelColor = colors.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WaterMath.STEP_MULTIPLIERS.forEach { mult ->
                TextButton(onClick = { onAdd(mult) }) {
                    Text(
                        text = "+${PersianText.toPersianDigits(WaterMath.stepMl(glassSizeMl, mult))}",
                        fontFamily = YekanBakh,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        WaterWeekRow(week = week, color = colors.primary, emptyColor = colors.surfaceVariant)
    }
}

@Composable
private fun WaterRing(
    progress: Float?,
    color: androidx.compose.ui.graphics.Color,
    track: androidx.compose.ui.graphics.Color,
    labelColor: androidx.compose.ui.graphics.Color,
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
        Canvas(Modifier.size(52.dp)) {
            val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val d = size.minDimension - stroke.width
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(d, d),
                style = stroke,
            )
            if (progress != null && progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(d, d),
                    style = stroke,
                )
            }
        }
        if (progress == null) {
            Text("—", fontFamily = YekanBakh, color = labelColor)
        }
    }
}

/**
 * Flat 7 bars, oldest first, each HEIGHT-scaled to the week's max so the row
 * is a comparison and not seven identical sticks. Days with no intake get a
 * faint fixed stub — a zero-height bar and a missing bar mean the same thing
 * here, but the stub keeps the row visually even.
 */
@Composable
private fun WaterWeekRow(
    week: List<Pair<LocalDate, Int>>,
    color: androidx.compose.ui.graphics.Color,
    emptyColor: androidx.compose.ui.graphics.Color,
) {
    val max = week.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        week.forEach { (date, ml) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fraction = (ml.toFloat() / max).coerceIn(0f, 1f)
                val barHeight = if (ml > 0) 8.dp + (20.dp * fraction) else 4.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .background(
                                color = if (ml > 0) color else emptyColor,
                                shape = CircleShape,
                            ),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = PersianText.toPersianDigits(date.dayOfMonth),
                    fontFamily = YekanBakh,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
