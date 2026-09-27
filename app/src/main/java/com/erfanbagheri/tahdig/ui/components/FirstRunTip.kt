package com.erfanbagheri.tahdig.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import kotlinx.coroutines.flow.StateFlow

/**
 * One-line contextual hint (#126).
 *
 * Shows once per tip id, never returns after dismissal. The dismissal is
 * permanent, so this must not sit above a primary action: it is an aside, and
 * [dismissTip] is a no-op once the id is in the store.
 */
@Composable
fun FirstRunTip(
    id: String,
    text: String,
    seen: StateFlow<Set<String>> = SettingsStore.tipsSeen,
    modifier: Modifier = Modifier,
) {
    val dismissed by seen.collectAsStateWithLifecycle()
    if (id in dismissed) return
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = { SettingsStore.dismissTip(id) },
                modifier = Modifier.size(28.dp),
            ) {
                // One label, spoken once: the hint text is the message, the
                // button only says "understood".
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "متوجه شدم",
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * Post-first-cook done state (#126).
 *
 * A brief pointer at the next action, not a modal wall. Renders nothing once
 * the first save/cook has happened, so it can never become a nag.
 */
@Composable
fun FirstSuccessNote(
    // The celebration clock (>0 from the first save/cook) and the ack flag.
    fired: StateFlow<Long> = SettingsStore.firstSuccessAt,
    done: StateFlow<Boolean> = SettingsStore.sampleDone,
    modifier: Modifier = Modifier,
) {
    val firedAt by fired.collectAsStateWithLifecycle()
    val celebrated by done.collectAsStateWithLifecycle()
    // Nothing to celebrate before the first save/cook (#126). A fresh install
    // must not see the note; a celebrated one must never see it again.
    if (firedAt <= 0L || celebrated) return
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = "اولین غذا رو پختی — آفرین",
                style = MaterialTheme.typography.titleSmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "برای ثبت عکس و یادداشت، خاطرات پخت رو باز کن؛ برای برنامه هفته، تب برنامه.",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                androidx.compose.material3.TextButton(onClick = { SettingsStore.setSampleDone() }) {
                    Text("متوجه شدم", fontFamily = YekanBakh)
                }
            }
        }
    }
}
