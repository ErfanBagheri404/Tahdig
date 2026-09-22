package com.erfanbagheri.tahdig.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erfanbagheri.tahdig.util.DietFilter
import com.erfanbagheri.tahdig.util.DifficultyFilter
import com.erfanbagheri.tahdig.util.SortOrder
import com.erfanbagheri.tahdig.util.TimeBucket
import com.erfanbagheri.tahdig.util.VoiceInput
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.SearchHistory
import com.erfanbagheri.tahdig.ui.viewmodel.RecentlyViewedViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onFoodClick: (Long) -> Unit = {},
    onOpenPantry: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val query by viewModel.query.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val diet by viewModel.diet.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val excluded by viewModel.excluded.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val results by viewModel.results.collectAsState()
    val cuisine by viewModel.cuisine.collectAsState()

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

            // Title
            Text(
                text = "جستجو",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
            )

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
                    .clickable(onClick = onOpenPantry),
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
            }

            Spacer(Modifier.height(8.dp))

            // Advanced filters: time / difficulty / sort (collapse to one hairline row)
            val timeBucket by viewModel.timeBucket.collectAsState()
            val difficultyFilter by viewModel.difficultyFilter.collectAsState()
            val sortOrder by viewModel.sortOrder.collectAsState()
            val activeFilterCount by viewModel.activeFilterCount.collectAsState()
            val cuisines by viewModel.cuisines.collectAsState()

            // Time chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TimeBucket.values().toList()) { bucket ->
                    CategoryChip(
                        label = bucket.label,
                        selected = timeBucket == bucket,
                        onClick = { viewModel.onTimeSelect(bucket) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Difficulty chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DifficultyFilter.values().toList()) { d ->
                    CategoryChip(
                        label = d.label,
                        selected = difficultyFilter == d,
                        onClick = { viewModel.onDifficultySelect(d) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Cuisine chips — «همه» plus one chip per cuisine present in the DB
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    CategoryChip(
                        label = "همه آشپزی‌ها",
                        selected = cuisine == null,
                        onClick = { viewModel.onCuisineSelect(null) },
                    )
                }
                items(cuisines, key = { it }) { code ->
                    CategoryChip(
                        label = cuisineLabel(code),
                        selected = cuisine == code,
                        onClick = { viewModel.onCuisineSelect(code) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Sort + clear-all row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ترتیب:",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                SortOrder.values().forEach { s ->
                    CategoryChip(
                        label = s.label,
                        selected = sortOrder == s,
                        onClick = { viewModel.onSortSelect(s) },
                    )
                    Spacer(Modifier.width(4.dp))
                }
                if (activeFilterCount > 0) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "حذف فیلترها",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = viewModel::clearFilters),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Search history (shown when query is empty)
            // Hoisted so the same instance is reused across recompositions.
            val history = remember { SearchHistory(context) }
            val historyQueries by history.queries.collectAsState()
            if (query.isBlank() && historyQueries.isNotEmpty()) {
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
            if (results.isEmpty() && filtering) {
                Text(
                    text = "نتیجه‌ای یافت نشد",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    textAlign = TextAlign.Center,
                )
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
        modifier = Modifier.clickable(
            onClick = onClick,
            role = androidx.compose.ui.semantics.Role.Button,
            onClickLabel = "انتخاب $label",
        ),
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
            ),
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

/** Farsi label for a [FoodEntity.cuisine] code. */
private fun cuisineLabel(code: String): String = when (code.uppercase()) {
    "IRANI" -> "🇮🇷 ایرانی"
    "GILAKI" -> "گیلانی"
    "ISFAHANI" -> "اصفهانی"
    "SHIRAZI" -> "شیرازی"
    "INTERNATIONAL" -> "🌍 بین‌المللی"
    "ITALIAN" -> "ایتالیایی"
    "TURKISH" -> "ترکی"
    "ARABIC" -> "عربی"
    "CHINESE" -> "چینی"
    "JAPANESE" -> "ژاپنی"
    "KOREAN" -> "کره‌ای"
    "INDIAN" -> "هندی"
    "THAI" -> "تایلندی"
    "VIETNAMESE" -> "ویتنامی"
    "GREEK" -> "یونانی"
    "FRENCH" -> "فرانسوی"
    "SPANISH" -> "اسپانیایی"
    "MEXICAN" -> "مکزیکی"
    "AMERICAN" -> "آمریکایی"
    "BRITISH" -> "انگلیسی"
    "POLISH" -> "لهستانی"
    "HUNGARIAN" -> "مجاری"
    "RUSSIAN" -> "روسی"
    else -> code.lowercase().replaceFirstChar { it.uppercase() }
}
