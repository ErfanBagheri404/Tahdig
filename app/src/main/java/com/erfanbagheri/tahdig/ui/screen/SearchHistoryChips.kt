package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.FirstRun

/**
 * Recent searches, falling back to bundled starter chips on a fresh install
 * (#126). A brand-new install has no history, and an empty search screen
 * teaches nothing — so before the first search the same row shows popular
 * dishes instead of disappearing. Clearing only affects the real history; the
 * starters are constants and come back until there is history to replace them.
 */
@Composable
fun SearchHistoryChips(
    history: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    starters: List<String> = FirstRun.starterChips,
) {
    val showingStarters = history.isEmpty()
    val items = if (showingStarters) starters else history
    if (items.isEmpty()) return
    Column(modifier = modifier.padding(top = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (showingStarters) "پیشنهاد شروع" else "جستجوهای اخیر",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            // No clear button for the bundled starters — there is nothing
            // user-owned to clear, and the button would be a lie.
            if (!showingStarters) {
                IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                    Text(
                        text = "پاک کردن",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        items.forEach { q ->
            Text(
                text = q,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(q) }
                    .padding(vertical = 6.dp),
            )
        }
    }
}
