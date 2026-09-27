package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.NutrientCaps

/**
 * Pass/fail line per active cap on a dish (#113).
 *
 * Flat text states, symbol + colour rather than colour alone so it survives
 * colour blindness, and the symbol is always first. No active caps renders
 * nothing — the section only appears when the user has a limit.
 */
@Composable
fun NutrientCapReport(
    caps: Map<NutrientCaps.Nutrient, Double>,
    amounts: Map<NutrientCaps.Nutrient, Double>,
    modifier: Modifier = Modifier,
) {
    // Absent data is reported as such, never as a comfortable pass.
    val checks = NutrientCaps.check(caps, amounts)
    if (checks.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "محدودیت‌های من",
            style = MaterialTheme.typography.titleSmall,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        checks.forEach { check ->
            val color = when (check.status) {
                NutrientCaps.Status.PASS -> MaterialTheme.colorScheme.primary
                NutrientCaps.Status.WARN -> MaterialTheme.colorScheme.tertiary
                NutrientCaps.Status.FAIL -> MaterialTheme.colorScheme.error
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(
                    text = "${check.status.symbol} ${check.describe()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = YekanBakh,
                    color = color,
                )
            }
        }
        // Capped but no real data behind it: say so instead of showing nothing,
        // otherwise a capped user cannot tell the difference between "fine" and
        // "unknown".
        val missing = caps.keys - amounts.keys
        if (missing.isNotEmpty()) {
            Text(
                text = "بدون داده: ${missing.joinToString("، ") { it.label }} —",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
