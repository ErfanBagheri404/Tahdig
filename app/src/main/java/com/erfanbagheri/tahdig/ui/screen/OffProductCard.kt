package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.OffClient
import com.erfanbagheri.tahdig.util.PersianText

/**
 * The scanned product card (#116): name, per-100g nutrients, allergens and the
 * Nutri-Score, plus the two actions the issue asks for.
 *
 * Every nutrient row obeys the app's one honesty rule: a null amount prints
 * «—», never 0. OFF omits a field nobody assayed, and printing 0 there would
 * claim the product contains none, which is a different and false statement.
 */
@Composable
fun OffProductCard(
    product: OffClient.Product,
    fromCache: Boolean,
    onAddToShopping: () -> Unit,
    onAddToPantry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (product.imageUrl != null) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.width(64.dp).height(64.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        product.name,
                        fontFamily = YekanBakh,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (product.brands.isNotBlank()) {
                        Text(
                            product.brands,
                            fontFamily = YekanBakh,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (fromCache) {
                        Text(
                            "از حافظهٔ آفلاین",
                            fontFamily = YekanBakh,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                product.nutriscore?.let { grade ->
                    Text(
                        "Nutri-Score $grade",
                        fontFamily = YekanBakh,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "در ۱۰۰ گرم:",
                fontFamily = YekanBakh,
                style = MaterialTheme.typography.labelMedium,
            )
            OffNutrientLine("انرژی", product.kcal100g, "کیلوکالری")
            OffNutrientLine("پروتئین", product.protein100g, "گرم")
            OffNutrientLine("کربوهیدرات", product.carb100g, "گرم")
            OffNutrientLine("چربی", product.fat100g, "گرم")
            OffNutrientLine("قند", product.sugars100g, "گرم")
            OffNutrientLine("نمک", product.salt100g, "گرم")

            if (product.allergens.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "حاوی " + product.allergens.joinToString("، "),
                    fontFamily = YekanBakh,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row {
                androidx.compose.material3.TextButton(onClick = onAddToShopping) {
                    Text("به سبد خرید", fontFamily = YekanBakh)
                }
                Spacer(Modifier.width(8.dp))
                androidx.compose.material3.TextButton(onClick = onAddToPantry) {
                    Text("به انبار", fontFamily = YekanBakh)
                }
            }
        }
    }
}

/** One nutrient line. Null prints «—»: OFF never assayed it, which is not zero. */
@Composable
private fun OffNutrientLine(label: String, amount: Double?, unit: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(
            label,
            fontFamily = YekanBakh,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (amount == null) "—"
            else PersianText.toPersianDigits(amount.toString()) + " " + unit,
            fontFamily = YekanBakh,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
        )
    }
}
