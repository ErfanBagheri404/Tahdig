package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onBrowseCategories: () -> Unit = {},
) {
    val suggestion by viewModel.suggestion.collectAsState()
    val mealLabel by viewModel.mealLabel.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

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

            TextButton(onClick = onBrowseCategories) {
                Text("مرور دسته‌بندی‌ها", fontFamily = YekanBakh)
            }

            Spacer(Modifier.height(24.dp))

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
                    onClick = { viewModel.toggleFavorite() },
                    enabled = suggestion != null,
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }

                // Re-roll
                Button(
                    onClick = { viewModel.roll() },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "غذا دیگه",
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("غذا دیگه")
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
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero emoji on accent background
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = accent.copy(alpha = 0.15f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Text(
                text = emoji,
                fontSize = 56.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
            )
        }

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
                    MetaChip("سختی: ${food.difficulty}")
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
