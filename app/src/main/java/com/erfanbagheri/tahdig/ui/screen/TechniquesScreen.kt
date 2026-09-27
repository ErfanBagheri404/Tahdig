package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.Technique
import com.erfanbagheri.tahdig.util.TechniqueRegistry
import java.text.Collator
import java.util.Locale

/**
 * «تکنیک‌ها» — the standalone browse entry (#101): one alphabetical hairline list.
 * Knowledge lives in the asset, so this is a pure render of the registry.
 */
@Composable
fun TechniquesScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val sorted = remember {
        val collator = Collator.getInstance(Locale("fa"))
        TechniqueRegistry.all().sortedWith { a, b -> collator.compare(a.name, b.name) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header("تکنیک‌ها", onBack)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sorted, key = { it.id }) { tech ->
                    TechniqueRow(tech, onClick = { onOpen(tech.id) })
                    Hairline()
                }
            }
        }
    }
}

@Composable
private fun TechniqueRow(tech: Technique, onClick: () -> Unit) {
    Text(
        text = tech.name,
        style = MaterialTheme.typography.titleMedium,
        fontFamily = YekanBakh,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

/** The house hairline: 1dp, faint, no Material divider styling. */
@Composable
fun Hairline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    )
}
