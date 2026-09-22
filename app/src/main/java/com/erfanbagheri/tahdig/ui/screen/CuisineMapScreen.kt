package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.Coverage
import com.erfanbagheri.tahdig.util.CuisineRegions
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.Region
import androidx.compose.ui.platform.LocalContext

/**
 * Cuisine exploration map (#90): region rows with a cooked/total coverage meter.
 * Rows render only for regions that have dishes — empty regions never appear,
 * so a zero-dish region can't draw an empty row.
 */
@Composable
fun CuisineMapScreen(
    onBack: () -> Unit,
    onRegionClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val db = TahdigDatabase.getInstance(context)
    val foods by db.foodDao().observeAll().collectAsState(initial = emptyList())
    val cooked by db.historyDao().observeCookedIds().collectAsState(initial = emptyList())

    val cookedSet = cooked.toSet()
    val byCuisine = foods.groupBy { it.cuisine }
    val rows = CuisineRegions.all.mapNotNull { region ->
        val ids = byCuisine[region.key].orEmpty().map { it.id }.toSet()
        if (ids.isEmpty()) null
        else region to CuisineRegions.coverage(ids, cookedSet)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header("کاوش آشپزی", onBack)
            LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
                CuisineRegions.SECTION_IRAN.let { section ->
                    val sectionRows = rows.filter { it.first.section == section }
                    if (sectionRows.isNotEmpty()) {
                        item(key = "hdr-$section") { SectionHeader(section) }
                        items(sectionRows.size) { i ->
                            val (region, cov) = sectionRows[i]
                            RegionRow(region, cov) { onRegionClick(region.key) }
                        }
                    }
                }
                CuisineRegions.SECTION_WORLD.let { section ->
                    val sectionRows = rows.filter { it.first.section == section }
                    if (sectionRows.isNotEmpty()) {
                        item(key = "hdr-$section") { SectionHeader(section) }
                        items(sectionRows.size) { i ->
                            val (region, cov) = sectionRows[i]
                            RegionRow(region, cov) { onRegionClick(region.key) }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontFamily = YekanBakh,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun RegionRow(region: Region, cov: Coverage, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        ) {
            Text(text = region.emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = region.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                // «۷ از ۴۲ غذا را امتحان کردی» — Persian digits, AC wording.
                Text(
                    text = PersianText.toPersianDigits(cov.cooked) + " از " +
                        PersianText.toPersianDigits(cov.total) + " غذا را امتحان کردی",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = YekanBakh,
                    color = if (cov.isComplete) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                // Flat 4dp meter — matches the app's hairline aesthetic, no rounded pill.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(cov.fraction)
                            .height(4.dp)
                            .background(
                                if (cov.isComplete) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            ),
                    )
                }
            }
        }
        // House hairline, full row width.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        )
    }
}
