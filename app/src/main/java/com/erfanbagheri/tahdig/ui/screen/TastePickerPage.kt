package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.CategoryEntity
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import kotlinx.coroutines.launch

/**
 * First-run taste picker (#92) — «سلیقت رو انتخاب کن».
 *
 * Sits between the brochure pages and the first-dish handoff: it is the first
 * stop where the user actually tells us something, so it belongs after the
 * "what this app is" pages and before the app starts guessing.
 *
 * Categories are read from the seeded `categories` table rather than hard-coded,
 * so a dish added to a new category in a later seed shows up here with no code
 * change and the chips can never drift from what the catalog actually contains.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TastePickerPage(
    onDone: () -> Unit,
) {
    // Same singleton the rest of onboarding reads (#126 handoff), so no db
    // parameter: a new caller would otherwise pass a second instance and the
    // picker would read a different catalog copy than the dishes it scores.
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { TahdigDatabase.getInstance(context) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var picked by remember { mutableStateOf(SettingsStore.preferredCategories.value) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        categories = db.categoryDao().getAll()
    }

    fun commit(next: Set<Long>) {
        picked = next
        scope.launch { SettingsStore.setPreferredCategories(next) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "سلیقت چیه؟",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = YekanBakh,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "هرچی دوست داری انتخاب کن. بعداً با امتیازهایی که می‌دی خودش دقیق‌تر می‌شه.",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            categories.forEach { cat ->
                val on = cat.id in picked
                FilterChip(
                    selected = on,
                    onClick = { commit(if (on) picked - cat.id else picked + cat.id) },
                    label = { Text(text = cat.name, fontFamily = YekanBakh) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (picked.isEmpty()) "بگذر، بعداً انتخاب می‌کنم" else "بریم جلو",
                fontFamily = YekanBakh,
            )
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = { commit(emptySet()); onDone() }) {
            Text(text = "رد کردن", fontFamily = YekanBakh)
        }
    }
}
