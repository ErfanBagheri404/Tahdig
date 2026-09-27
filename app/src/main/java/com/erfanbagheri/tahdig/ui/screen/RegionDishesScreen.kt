package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.clickable
import com.erfanbagheri.tahdig.ui.components.oneA11yStop
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.CuisineRegions

/**
 * Dishes of one region (#90), sorted so untried dishes come first — the map's
 * gamified nudge beyond the household's same-10 dishes.
 */
@Composable
fun RegionDishesScreen(
    cuisine: String,
    onFoodClick: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val db = TahdigDatabase.getInstance(context)
    val dishes by db.foodDao().observeByCuisine(cuisine).collectAsState(initial = emptyList())
    val cooked by db.historyDao().observeCookedIds().collectAsState(initial = emptyList())
    val cookedSet = cooked.toSet()

    val sorted = dishes.sortedBy { it.id in cookedSet }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(CuisineRegions.labelFor(cuisine), onBack)
            LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
                items(sorted, key = { it.id }) { dish ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onFoodClick(dish.id) }
                            // #129: name and the cooked check in one stop.
                            .oneA11yStop(
                                dish.name +
                                    if (dish.id in cookedSet) "، قبلاً درست کردی" else ""
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text(
                                text = dish.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (dish.id in cookedSet) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "✔ قبلاً درست کردی",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = YekanBakh,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
