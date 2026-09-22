package com.erfanbagheri.tahdig.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.components.DishThumb
import com.erfanbagheri.tahdig.ui.components.EmptyState
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.LeftoverViewModel
import com.erfanbagheri.tahdig.util.VoiceInput

/**
 * «غذای مونده دارم» (#107) — enter what is already cooked, get dishes that eat it first.
 *
 * Single-shot: nothing persists, the session dies with the screen. Visually distinct
 * from the pantry screen — the results header is «از اینا استفاده کن», not a match count.
 */
@Composable
fun LeftoverScreen(
    viewModel: LeftoverViewModel,
    onFoodClick: (Long) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val draft by viewModel.draft.collectAsState()
    val items by viewModel.items.collectAsState()
    val results by viewModel.results.collectAsState()
    val yesterday by viewModel.yesterday.collectAsState()

    // Recogniser presence is fixed for the process, so this is computed once.
    val micAvailable = remember { !VoiceInput.unavailable(context) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            VoiceInput.parse(VoiceInput.resultsFrom(result.data))?.let(viewModel::addSpoken)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "غذای مونده دارم",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = YekanBakh,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "چی مونده؟ بگو تا غذاهایی که همون رو مصرف می‌کنن رو بیارم",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (items.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearAll() }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "پاک کردن همه",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = onBack) {
                    Text("بازگشت", fontFamily = YekanBakh)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Quick multi-entry: text + voice, comma adds several at once ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = viewModel::onDraftChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("مثلاً: برنج، مرغ سرخ‌شده", fontFamily = YekanBakh) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = if (!micAvailable) null else {
                        {
                            IconButton(onClick = { VoiceInput.intent(context)?.let(launcher::launch) }) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "ورودی صوتی",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.add() },
                    enabled = draft.isNotBlank(),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "افزودن",
                        tint = if (draft.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // #107 (c): the only urgency input a single-shot session has.
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(
                    checked = yesterday,
                    onCheckedChange = viewModel::setYesterday,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "دیروز پختم (زودتر مصرف بشن)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Entered components as removable chips ──
            if (items.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items) { leftover ->
                        LeftoverChip(text = leftover.text) { viewModel.remove(leftover.text) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                // AC: empty input must not crash and must point at the way forward.
                items.isEmpty() -> EmptyState(
                    icon = Icons.Default.Restaurant,
                    title = "چیزی اضافه نکردی",
                    subtitle = "مثلاً «برنج» و «مرغ سرخ‌شده» رو بنویس یا بگو، تا غذاهایی که ازشون استفاده می‌کنن رو پیدا کنم",
                )

                results.isEmpty() -> EmptyState(
                    icon = Icons.Default.Restaurant,
                    title = "غذایی پیدا نشد",
                    subtitle = "با این مواد غذایی توی دفترچه پیدا نکردم — یکی دیگه اضافه کن",
                )

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "از اینا استفاده کن",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = YekanBakh,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(results, key = { it.food.id }) { scored ->
                            LeftoverRow(scored, onClick = { onFoodClick(scored.food.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftoverChip(text: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "حذف",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun LeftoverRow(scored: com.erfanbagheri.tahdig.util.LeftoverRanker.Scored, onClick: () -> Unit) {
    val food = scored.food
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DishThumb(imageUrl = food.imageUrl, categoryId = food.categoryId)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = YekanBakh,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            // The used components — this is why the dish is in the list at all.
            if (scored.used.isNotEmpty()) {
                Text(
                    text = "استفاده می‌کنه از: ${scored.used.joinToString("، ")}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            // The AC's diff line.
            Text(
                text = scored.headline(),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
