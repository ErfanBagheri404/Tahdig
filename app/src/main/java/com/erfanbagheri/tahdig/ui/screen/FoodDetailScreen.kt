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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.RatingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    foodId: Long,
    onBack: () -> Unit,
    ratingViewModel: RatingViewModel? = null,
) {
    val context = LocalContext.current.applicationContext
    var food by remember { mutableStateOf<FoodEntity?>(null) }

    LaunchedEffect(foodId) {
        val db = TahdigDatabase.getInstance(context)
        food = db.foodDao().getById(foodId)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(food?.name ?: "", fontFamily = YekanBakh) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )

            food?.let { f ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Hero emoji
                    val emoji = com.erfanbagheri.tahdig.util.FoodVisuals.emoji(f.categoryId)
                    val accent = com.erfanbagheri.tahdig.util.FoodVisuals.accent(f.categoryId)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 64.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Star rating
                    if (ratingViewModel != null) {
                        val dbStars by ratingViewModel.stars(f.id).collectAsState()
                        // Optimistic local value so rapid taps don't read a stale DB value
                        var localStars by remember(f.id) { mutableStateOf<Int?>(null) }
                        StarRating(
                            stars = localStars ?: dbStars,
                            onRate = {
                                localStars = it
                                ratingViewModel.setStars(f.id, it)
                            },
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    Text(
                        text = f.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = YekanBakh,
                        fontSize = 36.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    if (f.nameEn.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = f.nameEn,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    if (f.description.isNotBlank()) {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = f.description,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = YekanBakh,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    if (f.prepTimeMin > 0 || f.difficulty.isNotBlank()) {
                        Spacer(Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (f.prepTimeMin > 0) {
                                DetailChip("زمان آماده‌سازی: ${f.prepTimeMin} دقیقه")
                            }
                            if (f.difficulty.isNotBlank()) {
                                DetailChip("سختی: ${f.difficulty}")
                            }
                        }
                    }

                    if (f.ingredients.isNotBlank()) {
                        Spacer(Modifier.height(28.dp))
                        Text(
                            text = "مواد لازم",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = f.ingredients,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (f.tags.isNotBlank()) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = "برچسب‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = f.tags,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun StarRating(stars: Int, onRate: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { i ->
            IconButton(onClick = { onRate(if (stars == i) 0 else i) }) {
                Icon(
                    imageVector = if (i <= stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$i ستاره",
                    tint = if (i <= stars) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontFamily = YekanBakh,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
