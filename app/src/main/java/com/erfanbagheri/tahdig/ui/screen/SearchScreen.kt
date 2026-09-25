package com.erfanbagheri.tahdig.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.clickable
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erfanbagheri.tahdig.util.DietFilter
import com.erfanbagheri.tahdig.util.MicroNutrients
import com.erfanbagheri.tahdig.util.Flavor
import com.erfanbagheri.tahdig.util.VoiceInput
import com.erfanbagheri.tahdig.ui.components.FirstRunTip
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.FirstRun
import com.erfanbagheri.tahdig.ui.viewmodel.SearchHistory
import com.erfanbagheri.tahdig.ui.viewmodel.RecentlyViewedViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.SearchViewModel
import androidx.compose.ui.semantics.semantics
import com.erfanbagheri.tahdig.ui.components.oneA11yStop

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onFoodClick: (Long) -> Unit = {},
    onOpenPantry: () -> Unit = {},
    onOpenScanner: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val query by viewModel.query.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val diet by viewModel.diet.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val excluded by viewModel.excluded.collectAsState()
    val nutriAb by viewModel.nutriAb.collectAsState()
    val withinCaps by viewModel.withinCaps.collectAsState()
    val badge by viewModel.badge.collectAsState()
    val flavors by viewModel.flavors.collectAsState()
    val flavorCount by viewModel.flavorCount.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val results by viewModel.results.collectAsState()
    // #126: dishes a relaxed query reaches when the strict one returns none.
    val relaxedMatches by viewModel.relaxedMatches.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(48.dp))

            // Title + scanner entry (#116). The button only exists while the
            // scanner is enabled in Settings, so disabling it removes the
            // camera from the app surface entirely, not just the settings row.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = "جستجو",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                val scannerEnabled by com.erfanbagheri.tahdig.data.prefs.SettingsStore
                    .scannerEnabled.collectAsState()
                if (scannerEnabled) {
                    androidx.compose.material3.IconButton(onClick = onOpenScanner) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "اسکنر بارکد",
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Search bar
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { viewModel.onSubmit() },
                ),
                placeholder = {
                    Text(
                        "جستجوی غذا…",
                        fontFamily = YekanBakh,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "جستجو",
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "پاک کردن",
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )

            Spacer(Modifier.height(12.dp))

            // Pantry entry — "what can I cook with what I have?"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPantry)
                    // #129: the 🧺 glyph, the heading and the subcaption were
                    // three stops for one tile; one sentence is enough.
                    .oneA11yStop("چی دارم؟ بگو خونه چی داری، غذا پیشنهاد بده"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "🧺", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "چی دارم؟",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = "بگو خونه چی داری، غذا پیشنهاد بده",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Have-on-hand ingredient search: "what can I cook with X?"
            IngredientField(
                value = ingredients,
                onValueChange = viewModel::onIngredientsChange,
                label = "مواد در دسترس (با کاما جدا کن)",
                onSpoken = { viewModel.onIngredientsChange(VoiceInput.append(ingredients, it)) },
            )
            Spacer(Modifier.height(8.dp))
            IngredientField(
                value = excluded,
                onValueChange = viewModel::onExcludedChange,
                label = "مواد نامطلوب (حذف شود)",
                onSpoken = { viewModel.onExcludedChange(VoiceInput.append(excluded, it)) },
            )

            Spacer(Modifier.height(12.dp))

            // Category filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    CategoryChip(
                        label = "همه",
                        selected = selectedCategoryId == null,
                        onClick = { viewModel.onCategorySelect(null) },
                    )
                }
                items(categories, key = { it.id }) { cat ->
                    CategoryChip(
                        label = "${cat.emoji} ${cat.name}",
                        selected = selectedCategoryId == cat.id,
                        onClick = { viewModel.onCategorySelect(cat.id) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Dietary filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    CategoryChip(
                        label = "رژیمی",
                        selected = diet == null,
                        onClick = { viewModel.onDietSelect(null) },
                    )
                }
                items(DietFilter.values().toList()) { d ->
                    CategoryChip(
                        label = d.label,
                        selected = diet == d,
                        onClick = { viewModel.onDietSelect(d) },
                    )
                }
                // Nutri-Score A-B (#111). Kept in the diet row because it is
                // the same kind of question — and it only ever keeps dishes
                // whose grade came from real data.
                item {
                    CategoryChip(
                        label = "نمره A-B",
                        selected = nutriAb,
                        onClick = { viewModel.onNutriAbToggle(!nutriAb) },
                    )
                }
                // «در محدوده من» (#113). Only keeps dishes that pass the
                // user's nutrient caps; estimates drop out because unknown
                // amounts are not within a limit.
                item {
                    CategoryChip(
                        label = "در محدوده من",
                        selected = withinCaps,
                        onClick = { viewModel.onWithinCapsToggle(!withinCaps) },
                    )
                }
                // Threshold badges (#117). One at a time: the filters compose,
                // but a single badge row keeps the chips readable and the
                // empty-result case obvious.
                items(MicroNutrients.Badge.entries.toList()) { b ->
                    CategoryChip(
                        label = b.label,
                        selected = badge == b,
                        onClick = { viewModel.onBadgeSelect(if (badge == b) null else b) },
                    )
                }
            }

            // Taste/mood chips (#89) — «امروز چه مزه‌ای؟».
            // Hidden entirely when the library carries zero tags (acceptance),
            // never rendered as an empty row.
            if (flavorCount > 0) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "امروز چه مزه‌ای؟",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(Flavor.entries.toList()) { fl ->
                        CategoryChip(
                            label = fl.label,
                            selected = fl in flavors,
                            onClick = { viewModel.onFlavorToggle(fl) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // #126: one-line hint on first encounter with the chips row.
            FirstRunTip(
                id = FirstRun.Tip.SEARCH_CHIPS,
                text = "اگه دنبال چیز خاصی هستی، تایپ کن؛ وگرنه از پیشنهادها یکی رو بزن.",
            )

            // Search history (shown when query is empty)
            // Hoisted so the same instance is reused across recompositions.
            val history = remember { SearchHistory(context) }
            val historyQueries by history.queries.collectAsState()
            // #126: shown even with no history — the component falls back to
            // bundled starter chips, so a fresh install has somewhere to start.
            if (query.isBlank()) {
                SearchHistoryChips(
                    history = historyQueries,
                    onSelect = { viewModel.onQueryChange(it) },
                    onClear = { history.clear() },
                )
                Spacer(Modifier.height(8.dp))
            }

            // Recently viewed (shown when query is empty)
            val recentVm: RecentlyViewedViewModel = viewModel()
            val recentFoods by recentVm.recent.collectAsState()
            if (query.isBlank() && recentFoods.isNotEmpty()) {
                Text(
                    text = "اخیراً دیده شده",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                recentFoods.forEach { food ->
                    SearchResultItem(food = food, onClick = { onFoodClick(food.id) })
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            // Results
            val filtering = query.isNotBlank() || selectedCategoryId != null || diet != null ||
                ingredients.isNotBlank() || excluded.isNotBlank()
            // Non-query filters only: the note must not nag someone whose
            // plain query simply has no match.
            val hasActiveFilters = selectedCategoryId != null || diet != null ||
                ingredients.isNotBlank() || excluded.isNotBlank()
            if (results.isEmpty() && filtering) {
                // #126 no-match recovery: a dead sentence is a dead end. Offer
                // the filter reset, plus up to 3 dishes a relaxed query did
                // reach, so the user always has somewhere to go.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "نتیجه‌ای یافت نشد",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (hasActiveFilters) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "فیلترها رو بردار",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = viewModel::clearFilters) {
                            Text("برداشتن فیلترها", fontFamily = YekanBakh)
                        }
                    }
                    if (relaxedMatches.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "شاید این‌ها:",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(8.dp))
                        relaxedMatches.forEach { food ->
                            SearchResultItem(food = food, onClick = { onFoodClick(food.id) })
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.id }) { food ->
                        SearchResultItem(
                            food = food,
                            onClick = { onFoodClick(food.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clickable(
                onClick = onClick,
                role = androidx.compose.ui.semantics.Role.Button,
                onClickLabel = "انتخاب $label",
            )
            // #129: a selected chip was only distinguished by colour.
            .semantics {
                stateDescription = if (selected) "انتخاب‌شده" else "انتخاب‌نشده"
            },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = YekanBakh,
            color = if (selected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SearchResultItem(
    food: com.erfanbagheri.tahdig.data.local.entity.FoodEntity,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                role = androidx.compose.ui.semantics.Role.Button,
                onClickLabel = "نمایش ${food.name}",
            )
            // #129: result rows were thumb + name + meta = three stops.
            .oneA11yStop(food.name),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        // Row: photo thumb on the left, text column on the right
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.erfanbagheri.tahdig.ui.components.DishThumb(
                imageUrl = food.imageUrl,
                categoryId = food.categoryId,
                size = 56.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (food.prepTimeMin > 0) {
                        Text(
                            text = "${food.prepTimeMin} دقیقه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (food.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = food.description,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                if (food.difficulty.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "سختی: ${difficultyLabel(food.difficulty)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun IngredientField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onSpoken: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Recogniser presence is fixed for the process, so this is computed once.
    val micAvailable = remember { !VoiceInput.unavailable(context) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            VoiceInput.parse(VoiceInput.resultsFrom(result.data))?.let(onSpoken)
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(label, fontFamily = YekanBakh)
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        // Hidden when the device has no recogniser rather than shown and failing.
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
}

private fun difficultyLabel(d: String): String = when (d.uppercase()) {
    "EASY" -> "آسان"
    "MEDIUM" -> "متوسط"
    "HARD" -> "سخت"
    else -> d
}
