package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.TechniqueLinker
import com.erfanbagheri.tahdig.util.TechniqueRegistry

/**
 * Technique detail (#101): body + offline reverse links into dishes.
 * Keyword match over name/description/ingredients — the same boundary-safe
 * matcher the step linkifier uses, so lists can't disagree with links.
 */
@Composable
fun TechniqueDetailScreen(
    techniqueId: String,
    onBack: () -> Unit,
    onDishClick: (Long) -> Unit,
) {
    val tech = TechniqueRegistry.byId(techniqueId)
    val context = LocalContext.current.applicationContext
    val foods by TahdigDatabase.getInstance(context).foodDao()
        .observeAll().collectAsState(initial = emptyList())

    val linkedDishes = remember(foods, tech) {
        if (tech == null) emptyList()
        else foods.filter { f ->
            TechniqueLinker.hasKeyword(f.name, tech) ||
                TechniqueLinker.hasKeyword(f.description, tech) ||
                TechniqueLinker.hasKeyword(f.ingredients, tech)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Header(tech?.name ?: "تکنیک", onBack)

            if (tech != null) {
                Text(
                    text = tech.body,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = YekanBakh,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )
            }

            if (linkedDishes.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "غذاهایی که از این تکنیک استفاده می‌کنن",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = YekanBakh,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(4.dp))
                linkedDishes.forEach { dish ->
                    Text(
                        text = dish.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDishClick(dish.id) }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                    )
                    Hairline()
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
