package com.erfanbagheri.tahdig.ui.screen

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.components.DishPhoto
import com.erfanbagheri.tahdig.util.Haptics
import com.erfanbagheri.tahdig.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onBrowseCategories: () -> Unit = {},
    onFoodClick: (Long) -> Unit = {},
    onOpenLeftover: () -> Unit = {},
    /** «رو به اتمام» card taps through to the pantry (#106). */
    onOpenPantry: () -> Unit = {},
) {
    val suggestion by viewModel.suggestion.collectAsState()
    val mealLabel by viewModel.mealLabel.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val view = LocalView.current
    val dishOfDay by viewModel.dishOfDay.collectAsState()
    val expirySummary by viewModel.expirySummary.collectAsState()
    val leftoverSuggestions by viewModel.leftoverSuggestions.collectAsState()
    val occasion by viewModel.occasion.collectAsState()
    val occasionDishes by viewModel.occasionDishes.collectAsState()
    // Refresh day-dependent state when app returns to foreground (midnight-safe).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDay()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // Meal label pill
            Text(
                text = mealLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )

            Spacer(Modifier.height(40.dp))

            // Dish of the day (deterministic by date)
            dishOfDay?.let { dod ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🌟 غذای امروز",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = dod.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = YekanBakh,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
            TextButton(onClick = onBrowseCategories) {
                Text("مرور دسته‌بندی‌ها", fontFamily = YekanBakh)
            }
            // #107: the zero-waste daily trigger lives next to the browse entry.
            TextButton(onClick = onOpenLeftover) {
                Text("غذای مونده دارم", fontFamily = YekanBakh)
            }
            Spacer(Modifier.height(24.dp))

            // ── Expiry summary (#106) ──────────────────────────────────────
            // Computed from Room alone; the card emits NOTHING when the string is
            // empty, so a fresh pantry shows no empty row.
            if (expirySummary.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenPantry),
                ) {
                    Text(
                        text = expirySummary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Occasion shelf (#88) ───────────────────────────────────────
            // Renders only while an occasion window is active — outside every
            // window this emits nothing at all, so no empty row can appear (AC).
            occasion?.let { occ ->
                if (occasionDishes.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = occ.shelf,
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "مناسبت‌ها",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(occasionDishes, key = { it.id }) { food ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                ),
                                modifier = Modifier.clickable { onFoodClick(food.id) },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = food.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontFamily = YekanBakh,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            leftoverSuggestions.takeIf { it.isNotEmpty() }?.let { leftovers ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "از این چیزی مونده؟ فردا با باقی‌مونده‌ش چی درست کنم؟",
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = viewModel::dismissLeftover) {
                                Icon(Icons.Default.Close, contentDescription = "بستن",
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(leftovers, key = { it.id }) { food ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.clickable { onFoodClick(food.id) },
                                ) {
                                    Text(
                                        text = food.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontFamily = YekanBakh,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Suggestion card
            if (suggestion != null) {
                SuggestionCard(
                    food = suggestion!!,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = "در حال انتخاب…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.weight(1f))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Favorite toggle
                IconButton(
                    onClick = {
                        Haptics.tap(view)
                        viewModel.toggleFavorite()
                    },
                    enabled = suggestion != null,
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }

                // Cooked — triggers the leftover prompt
                IconButton(
                    onClick = {
                        Haptics.confirm(view)
                        viewModel.markCooked()
                    },
                    enabled = suggestion != null,
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "پختم",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }

                // Re-roll
                Button(
                    onClick = {
                        Haptics.tap(view)
                        viewModel.roll()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "غذای دیگه",
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("غذای دیگه")
                }

                // Block current
                Button(
                    onClick = { viewModel.toggleBlocked() },
                    enabled = suggestion != null,
                    modifier = Modifier
                        .size(52.dp),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "دیگه اینو نده",
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SuggestionCard(
    food: com.erfanbagheri.tahdig.data.local.entity.FoodEntity,
    modifier: Modifier = Modifier,
) {
    val accent = com.erfanbagheri.tahdig.util.FoodVisuals.accent(food.categoryId)
    val emoji = com.erfanbagheri.tahdig.util.FoodVisuals.emoji(food.categoryId)

    Column(
        modifier = modifier
            .semantics { contentDescription = "پیشنهاد غذا: ${food.name}" }
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero: real photo when available, category emoji otherwise
        DishPhoto(
            imageUrl = food.imageUrl,
            categoryId = food.categoryId,
            height = 200.dp,
            cornerRadius = 0.dp,
            modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        )

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        // Food name — big, centered
        Text(
            text = food.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontFamily = YekanBakh,
            fontSize = 32.sp,
        )

        // Description
        if (food.description.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = food.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontFamily = YekanBakh,
            )
        }

        // Meta row: prep time + difficulty
        if (food.prepTimeMin > 0 || food.difficulty.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (food.prepTimeMin > 0) {
                    MetaChip("زمان آماده‌سازی: ${food.prepTimeMin} دقیقه")
                }
                if (food.difficulty.isNotBlank()) {
                    MetaChip("سختی: ${difficultyLabel(food.difficulty)}")
                }
            }
        }

        // Ingredients
        if (food.ingredients.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "مواد لازم",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = food.ingredients,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                fontFamily = YekanBakh,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        }
    }
}

@Composable
fun MetaChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private fun difficultyLabel(d: String): String = when (d.uppercase()) {
    "EASY" -> "آسان"
    "MEDIUM" -> "متوسط"
    "HARD" -> "سخت"
    else -> d
}
