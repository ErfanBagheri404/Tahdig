package com.erfanbagheri.tahdig.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh

/**
 * Ghost-row empty state (#126).
 *
 * An illustration-only empty list teaches nothing — it shows no rows, so the
 * user cannot tell what will appear there. This renders the *real* row shape at
 * low opacity instead, so the list previews its own layout, then one inline CTA
 * under it. Ghost rows are decorative only (no semantics), the CTA is the
 * single accessible action.
 */
@Composable
fun GhostRowsEmptyState(
    title: String,
    cta: String,
    onCta: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    rows: Int = 3,
) {
    val line = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(rows) { i ->
            // Middle row slightly shorter — reads as a list, not a stack of bars.
            val inset = if (i == rows / 2) 40.dp else 0.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .alpha(0.22f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(line, RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth(if (i == rows / 2) 0.55f else 0.75f)
                            .height(12.dp)
                            .background(line, RoundedCornerShape(6.dp)),
                    )
                    Spacer(Modifier.height(7.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.35f)
                            .height(10.dp)
                            .background(line, RoundedCornerShape(5.dp)),
                    )
                }
                if (inset > 0.dp) Spacer(Modifier.width(inset))
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onCta) {
            Text(cta, fontFamily = YekanBakh, style = MaterialTheme.typography.labelLarge)
        }
    }
}
