package com.erfanbagheri.tahdig.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.erfanbagheri.tahdig.util.FoodVisuals

/**
 * Dish photo with the emoji as fallback.
 *
 * Offline-first: when the URL is null or the fetch fails (no network / not cached yet)
 * we render the category emoji on the category accent, exactly like the pre-photo app,
 * so the UI never shows a broken-image placeholder.
 *
 * ponytail: Coil's disk cache handles offline replay; we do not pre-download images.
 * Upgrade path: a Settings toggle to prefetch all URLs on Wi-Fi.
 */
@Composable
fun DishPhoto(
    imageUrl: String?,
    categoryId: Long,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    cornerRadius: Dp = 20.dp,
) {
    val emoji = FoodVisuals.emoji(categoryId)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        // Emoji layer doubles as the loading + error state.
        Text(text = emoji, fontSize = 64.sp)

        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Compact variant for list rows. */
@Composable
fun DishThumb(
    imageUrl: String?,
    categoryId: Long,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val emoji = FoodVisuals.emoji(categoryId)
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .height(size)
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = (size.value / 2.2f).sp)

        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
