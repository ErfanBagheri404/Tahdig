package com.erfanbagheri.tahdig.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.PersianText

/**
 * Equipment chips (#100) — a hairline row of tools a dish needs, each toggling
 * «آماده». Session-scoped by design: which pot you dug out is not worth a DB row.
 *
 * House style: flat chips with a 1dp border, no elevation, no rounded card.
 */
@Composable
fun EquipmentRow(
    labels: List<String>,
    readyLabels: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        labels.forEach { label ->
            val ready = label in readyLabels
            Text(
                text = label + if (ready) " · آماده" else "",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = YekanBakh,
                color = if (ready) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        color = if (ready) MaterialTheme.colorScheme.primary
                        else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = if (ready) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onToggle(label) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

/** «۳ ابزار» — the cook-mode top-bar count. */
fun equipmentCountLabel(readyCount: Int, total: Int): String {
    val t = PersianText.toPersianDigits(total.toString())
    return if (readyCount == total) "$t ابزار · آماده"
    else "$t ابزار"
}
