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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.FavoritesViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.HistoryViewModel

@Composable
fun FavoritesScreen(
    favoritesViewModel: FavoritesViewModel,
    historyViewModel: HistoryViewModel,
    onFoodClick: (Long) -> Unit = {},
) {
    val favoritedFoods by favoritesViewModel.favoritedFoods.collectAsState()
    val blockedFoods by favoritesViewModel.blockedFoods.collectAsState()
    val historyItems by historyViewModel.historyItems.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf("علاقه‌مندی‌ها", "مسدود شده‌ها", "تاریخچه")

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(48.dp))

        Text(
            text = tabs[selectedTab],
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontFamily = YekanBakh) },
                )
            }
        }

        when (selectedTab) {
            0 -> {
                if (favoritedFoods.isEmpty()) EmptyMessage("غذای مورد علاقه‌ای ثبت نشده")
                else FavoriteList(
                    foods = favoritedFoods,
                    isBlocked = false,
                    onRemove = { favoritesViewModel.removeFavorite(it) },
                    onClick = onFoodClick,
                )
            }
            1 -> {
                if (blockedFoods.isEmpty()) EmptyMessage("غذای مسدود شده‌ای نیست")
                else FavoriteList(
                    foods = blockedFoods,
                    isBlocked = true,
                    onRemove = { favoritesViewModel.unblock(it) },
                    onClick = onFoodClick,
                )
            }
            2 -> {
                if (historyItems.isEmpty()) EmptyMessage("تاریخچه‌ای ثبت نشده")
                else HistoryList(
                    items = historyItems,
                    onClick = onFoodClick,
                )
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HistoryList(
    items: List<com.erfanbagheri.tahdig.data.local.entity.HistoryWithFood>,
    onClick: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(items, key = { it.history.id }) { item ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(item.food.id) },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.food.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (item.food.description.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.food.description,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatRelativeTime(item.history.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatRelativeTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000L -> "اکنون"
        diff < 3_600_000L -> "${diff / 60_000} دقیقه پیش"
        diff < 86_400_000L -> "${diff / 3_600_000} ساعت پیش"
        else -> "${diff / 86_400_000} روز پیش"
    }
}

@Composable
private fun FavoriteList(
    foods: List<FoodEntity>,
    isBlocked: Boolean,
    onRemove: (Long) -> Unit,
    onClick: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(foods, key = { it.id }) { food ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(food.id) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = food.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (food.description.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = food.description,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { onRemove(food.id) }) {
                        Icon(
                            imageVector = if (isBlocked) Icons.Default.Block else Icons.Default.FavoriteBorder,
                            contentDescription = if (isBlocked) "رفع مسدود" else "حذف از علاقه‌مندی‌ها",
                            tint = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}
