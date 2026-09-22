package com.erfanbagheri.tahdig.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.MisePlace

/**
 * Mise-en-place rows (#99): flat square check indicator with a hairline border —
 * deliberately NOT the Material checkbox (house style: flat, hairline, custom).
 *
 * Interactive when [onToggle] is set; cook mode passes null so the rows are a
 * read-only overview and a mid-cook tap can never uncheck prep state.
 */
@Composable
fun MiseChecklist(
    rows: List<MisePlace.Row>,
    checkedHashes: Set<String>,
    onToggle: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEach { row ->
            val checked = row.hash in checkedHashes
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onToggle != null) Modifier.clickable { onToggle(row.hash) } else Modifier)
                    .padding(vertical = 6.dp),
            ) {
                SquareCheck(checked)
                Text(
                    text = row.display,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (checked) TextDecoration.LineThrough else null,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                        .alpha(if (checked) 0.55f else 1f),
                )
            }
        }
    }
}

/** Custom square indicator: flat fill when checked, 1dp hairline border when not. */
@Composable
private fun SquareCheck(checked: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(20.dp)
            .border(
                width = 1.dp,
                color = if (checked) primary else MaterialTheme.colorScheme.outlineVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            )
            .background(
                color = if (checked) primary else Color.Transparent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            ),
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** «۳ از ۱۱ آماده شد» — the counter shown above the list. */
fun miseCounter(checkedCount: Int, total: Int): String {
    val c = com.erfanbagheri.tahdig.util.PersianText.toPersianDigits(checkedCount.toString())
    val t = com.erfanbagheri.tahdig.util.PersianText.toPersianDigits(total.toString())
    return "$c از $t آماده شد"
}
