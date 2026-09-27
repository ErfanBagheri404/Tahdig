package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.WeightTrend

/**
 * Weight trend (#115): thin polyline of the logged weights plus the 7-day
 * moving average, the delta line since the first entry, and a log field.
 * Canvas only — no chart dependency, per the issue.
 */
@Composable
fun WeightTrendCard(
    entries: List<WeightTrend.Entry>,
    goalStale: Boolean,
    onUpdateGoalPrompt: () -> Unit,
    onLog: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val series = remember(entries) { WeightTrend.series(entries) }
    val delta = remember(entries) { WeightTrend.deltaKg(entries) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text("روند وزن", fontFamily = YekanBakh, fontWeight = FontWeight.Bold)

        val deltaLine = delta?.let { WeightTrend.deltaLine(it) }
        if (deltaLine != null) {
            Text(
                text = deltaLine,
                fontFamily = YekanBakh,
                color = if (delta < 0) colors.primary else colors.onSurfaceVariant,
            )
        } else if (entries.size < 2) {
            Text(
                text = "برای دیدن رند، حداقل دو وزن ثبت کن",
                fontFamily = YekanBakh,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(8.dp))

        if (series.size >= 2) {
            WeightChart(
                series = series,
                lineColor = colors.primary,
                averageColor = colors.tertiary,
                track = colors.surfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row {
                Text("— ", fontFamily = YekanBakh, color = colors.primary)
                Text("وزن روز", fontFamily = YekanBakh, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Text("   ‑‑ ", fontFamily = YekanBakh, color = colors.tertiary)
                Text("میانگین هفت روز", fontFamily = YekanBakh, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            // Honest empty state: the chart is not drawn at all, rather than
            // showing a single point that reads as "flat for a week".
            val emptyTrack = colors.surfaceVariant
            Canvas(Modifier.fillMaxWidth().height(80.dp)) {
                drawLine(
                    color = emptyTrack,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }

        if (goalStale) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = "وزنت عوض شده، هدف کالری رو به‌روز کن؟",
                    fontFamily = YekanBakh,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onUpdateGoalPrompt) {
                    Text("به‌روز", fontFamily = YekanBakh)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        LogWeightField(onLog = onLog)
    }
}

@Composable
private fun WeightChart(
    series: List<WeightTrend.Point>,
    lineColor: androidx.compose.ui.graphics.Color,
    averageColor: androidx.compose.ui.graphics.Color,
    track: androidx.compose.ui.graphics.Color,
) {
    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        val kgs = series.flatMap { listOf(it.kg, it.average ?: it.kg) }
        val min = kgs.min()
        val max = kgs.max()
        // A flat week divides by zero; keep a 1kg band so it draws a line
        // instead of NaN coordinates.
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0

        fun x(i: Int): Float = if (series.size == 1) size.width / 2f else size.width * i / (series.size - 1).toFloat()
        fun y(kg: Double): Float = (size.height - 8f) * (1f - ((kg - min) / span).toFloat()) + 4f

        drawLine(
            color = track,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1f,
        )

        // 7-day average polyline, dotted-by-construction (drawn where present).
        val avgPath = Path()
        var avgStarted = false
        series.forEachIndexed { i, p ->
            val avg = p.average ?: return@forEachIndexed
            if (!avgStarted) {
                avgPath.moveTo(x(i), y(avg))
                avgStarted = true
            } else {
                avgPath.lineTo(x(i), y(avg))
            }
        }
        if (avgStarted) {
            drawPath(
                path = avgPath,
                color = averageColor,
                style = Stroke(width = 4f, cap = StrokeCap.Round),
            )
        }

        // Weight polyline — the raw signal.
        val path = Path()
        series.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(x(i), y(p.kg)) else path.lineTo(x(i), y(p.kg))
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )
    }
}

/**
 * Numeric field gated on parseability, not non-blankness — a field holding
 * "،،" must not enable a button that then drops the input.
 */
@Composable
private fun LogWeightField(onLog: (Double) -> Unit) {
    var text by remember { mutableStateOf("") }
    val kg = text.trim().replace(',', '.').toDoubleOrNull()
    val valid = kg != null && kg > 0 && kg <= 500

    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("وزن امروز (کیلو)", fontFamily = YekanBakh) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            enabled = valid,
            onClick = {
                onLog(kg!!)
                text = ""
            },
        ) {
            Text("ثبت", fontFamily = YekanBakh)
        }
    }
}
