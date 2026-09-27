package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.erfanbagheri.tahdig.ui.components.oneA11yStop
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erfanbagheri.tahdig.data.local.entity.CategoryEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.SelectionSet
import com.erfanbagheri.tahdig.ui.viewmodel.CategoryViewModel

@Composable
fun CategoryBrowseScreen(
    viewModel: CategoryViewModel,
    onCategoryClick: (Long) -> Unit,
    onBack: () -> Unit,
    /** Standalone «تکنیک‌ها» browse entry (#101). */
    onTechniques: () -> Unit = {},
    /** Standalone «مناسبت‌ها» browse entry (#88). */
    onOccasions: () -> Unit = {},
    /** Standalone «کاوش آشپزی» browse entry (#90). */
    onCuisineMap: () -> Unit = {},
) {
    val categories by viewModel.categories.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header("دسته‌بندی‌ها", onBack)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(categories, key = { it.id }) { cat ->
                    CategoryTile(cat, onClick = { onCategoryClick(cat.id) })
                }
                // Technique library entry (#101) — knowledge, not a dish category.
                item(key = "techniques") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onTechniques)
                            .oneA11yStop("تکنیک‌ها"),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 20.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(text = "📖", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "تکنیک‌ها",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                // Occasion browse entry (#88) — same tile shape as تکنیک‌ها.
                item(key = "occasions") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOccasions)
                            .oneA11yStop("مناسبت‌ها"),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 20.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(text = "🎉", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "مناسبت‌ها",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                // Cuisine map entry (#90) — deep-linked from the top of Categories.
                item(key = "cuisine-map") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onCuisineMap)
                            .oneA11yStop("کاوش آشپزی"),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 20.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(text = "🗺", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "کاوش آشپزی",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(cat: CategoryEntity, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // #129: the 32sp emoji is decorative — say the category once.
            .oneA11yStop(cat.name),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Decorative; the row carries the spoken name.
            Text(
                text = cat.emoji.ifEmpty { "🍽" },
                fontSize = 32.sp,
                modifier = Modifier.clearAndSetSemantics { },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = cat.name,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun CategoryDishesScreen(
    viewModel: CategoryViewModel,
    categoryId: Long,
    categoryName: String,
    onFoodClick: (Long) -> Unit,
    onBack: () -> Unit,
    favoritesViewModel: com.erfanbagheri.tahdig.ui.viewmodel.FavoritesViewModel? = null,
    shoppingViewModel: com.erfanbagheri.tahdig.ui.viewmodel.ShoppingViewModel? = null,
    // #81: hoisted so the host owns back-exit + bottom-nav hiding.
    selection: SelectionSet = SelectionSet(),
    onSelectionChange: (SelectionSet) -> Unit = {},
) {
    val dishes by viewModel.dishesByCategory(categoryId).collectAsState(initial = emptyList())
    // #81: batch selection. Long-press enters, tap toggles, back/cancel exits.

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selection.isActive) {
                SelectionTopBar(
                    count = selection.count,
                    onCancel = { onSelectionChange(selection.clear()) },
                    onAddToShopping = {
                        val ids = selection.ids.toList()
                        onSelectionChange(selection.clear())
                        shoppingViewModel?.addDishes(ids)
                    },
                    onAddToFavorites = {
                        val ids = selection.ids.toList()
                        onSelectionChange(selection.clear())
                        ids.forEach { favoritesViewModel?.addFavorite(it) }
                    },
                    onDelete = {
                        val ids = selection.ids.toList()
                        onSelectionChange(selection.clear())
                        viewModel.deleteDishes(ids)
                    },
                )
            } else {
                Header(categoryName, onBack)
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(dishes, key = { it.id }) { dish ->
                    SelectableDishCard(
                        dish = dish,
                        selected = dish.id in selection.ids,
                        selectionActive = selection.isActive,
                        onClick = {
                            if (selection.isActive) onSelectionChange(selection.toggle(dish.id))
                            else onFoodClick(dish.id)
                        },
                        onLongClick = { onSelectionChange(selection.add(dish.id)) },
                    )
                }
            }
        }
    }
}

/** #81 — contextual bar: count in Persian digits + the four batch actions. */
@Composable
private fun SelectionTopBar(
    count: Int,
    onCancel: () -> Unit,
    onAddToShopping: () -> Unit,
    onAddToFavorites: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCancel) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "لغو")
        }
        Text(
            text = "${PersianText.toPersianDigits(count)} مورد",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = YekanBakh,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onAddToShopping) {
            Icon(Icons.Default.ShoppingCart, contentDescription = "افزودن به لیست خرید")
        }
        IconButton(onClick = onAddToFavorites) {
            Icon(Icons.Default.Favorite, contentDescription = "افزودن به علاقهمندیها")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "حذف")
        }
    }
}

/** #81 — dish card that toggles in selection mode, opens the dish otherwise. */
// combinedClickable is still @ExperimentalFoundationApi; long-press is the only
// way into selection mode, so the opt-in is structural, not a preference.
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SelectableDishCard(
    dish: FoodEntity,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = "انتخاب غذا",
            )
            .oneA11yStop(
                dish.name +
                    if (dish.description.isNotBlank())
                        "، ${dish.description.take(60)}" +
                        if (dish.description.length > 60) "..." else ""
                    else ""
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = dish.name,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = YekanBakh,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (dish.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dish.description.take(60) + (if (dish.description.length > 60) "..." else ""),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun Header(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = YekanBakh,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun DishCard(dish: FoodEntity, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // #129: name + the 60-char description line are two stops
            // otherwise; the description is what decides "this is the one".
            .oneA11yStop(
                dish.name +
                    if (dish.description.isNotBlank())
                        "، ${dish.description.take(60)}" +
                        if (dish.description.length > 60) "…" else ""
                    else ""
            ),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = dish.name,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = YekanBakh,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (dish.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dish.description.take(60) + (if (dish.description.length > 60) "…" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
