package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.local.entity.CaffeineLogEntity
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.Caffeine
import com.erfanbagheri.tahdig.util.PersianText

/**
 * Home caffeine card (#119): preset chips, today's accumulation against
 * the cap, an over-limit line, the 7-day row, and today's hits with a
 * delete. Flat rows and hairlines — no nested cards.
 */
@Composable
fun CaffeineCard(
    todayMg: Int,
    capMg: Int,
    pregnancyMode: Boolean,
    rows: List<CaffeineLogEntity>,
    week: List<Int>,
    onLogPreset: (String, Int) -> Unit,
    onLogCustom: (Int) -> Unit,
    onRemove: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val over = Caffeine.overCap(todayMg, capMg)
    // Bar fill caps at 1.0: a 600 mg day should not draw past the track,
    // the over-limit LINE is what reports the excess.
    val ratio = if (capMg <= 0) 0f else (todayMg.toFloat() / capMg).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Text("کافئین", fontFamily = YekanBakh, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (over) colors.error else colors.primary,
        )
        Spacer(Modifier.height(6.dp))

        Text(
            text = Caffeine.progressLine(todayMg, capMg, pregnancyMode),
            fontFamily = YekanBakh,
            color = colors.onSurfaceVariant,
        )
        // The warning is a separate line, not a colour change alone —
        // colour alone is not a signal a colour-blind user can read.
        if (over) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "بیش از سقف روزانه — " +
                    PersianText.toPersianDigits((todayMg - capMg).toDouble()) +
                    " میلی‌گرم بیشتر",
                fontFamily = YekanBakh,
                color = colors.error,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Caffeine.PRESETS.forEach { (label, mg) ->
                TextButton(onClick = { onLogPreset(label, mg) }) {
                    Text(
                        text = label + " " + PersianText.toPersianDigits(mg.toDouble()),
                        fontFamily = YekanBakh,
                    )
                }
            }
            // A custom hit needs a number, not a dialog: +۵۰ is the common
            // case (half a coffee, a cola) and one tap beats a form.
            TextButton(onClick = { onLogCustom(50) }) {
                Text(
                    text = "+" + PersianText.toPersianDigits(50.0),
                    fontFamily = YekanBakh,
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        CaffeineWeekRow(week = week, over = capMg, color = colors.primary,
            warnColor = colors.error, emptyColor = colors.surfaceVariant)

        if (rows.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            rows.take(4).forEachIndexed { i, row ->
                if (i > 0) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.outlineVariant.copy(alpha = 0.4f)),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = row.label + " · " +
                            PersianText.toPersianDigits(row.mg.toDouble()) + " میلی‌گرم",
                        fontFamily = YekanBakh,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onRemove(row.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "حذف",
                            fontFamily = YekanBakh,
                            color = colors.error,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Seven-day bars. A day past the cap is drawn in the error colour so the
 * week's pattern is visible at a glance — the same colour the today bar
 * uses, so the two never disagree.
 */
@Composable
private fun CaffeineWeekRow(
    week: List<Int>,
    over: Int,
    color: androidx.compose.ui.graphics.Color,
    warnColor: androidx.compose.ui.graphics.Color,
    emptyColor: androidx.compose.ui.graphics.Color,
) {
    if (week.isEmpty()) return
    val maxMg = week.maxOrNull() ?: 0
    // Scale to the cap when everything fits under it, so a light week does
    // not render as a row of full bars.
    val scale = maxOf(maxMg, over).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        week.forEach { mg ->
            Box(
                Modifier
                    .weight(1f)
                    .height((4 + (mg.toFloat() / scale) * 20).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            mg == 0 -> emptyColor
                            mg > over -> warnColor
                            else -> color
                        },
                    ),
            )
        }
    }
}
