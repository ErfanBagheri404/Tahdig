package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.BorderStroke
import com.erfanbagheri.tahdig.ui.components.oneA11yStop
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.OccasionRegistry
import com.erfanbagheri.tahdig.util.Occasion
import java.time.LocalDate

/**
 * «مناسبت‌ها» browse (#88): every bundled occasion with its curated dish strip.
 * The window rule is evaluated here live, so today's occasion is marked «فعال».
 */
@Composable
fun OccasionsScreen(
    onBack: () -> Unit,
    onFoodClick: (Long) -> Unit,
) {
    val activeKey = OccasionRegistry.activeOn(LocalDate.now())?.key

    // Curated ids are loose references — resolve them to real names once (#88),
    // so a stale id renders as its numeric fallback instead of vanishing.
    val context = LocalContext.current
    val names by produceState(initialValue = emptyMap<Long, String>()) {
        val dao = TahdigDatabase.getInstance(context).foodDao()
        val ids = OccasionRegistry.all.flatMap { it.dishes }.distinct()
        if (ids.isNotEmpty()) {
            value = dao.byIds(ids).associate { it.id to it.name }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header("مناسبت‌ها", onBack)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                items(OccasionRegistry.all, key = { it.key }) { occ ->
                    OccasionSection(occ, occ.key == activeKey, names, onFoodClick)
                }
                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "فهرست مناسبت‌ها از دادهٔ داخل برنامه ساخته می‌شود و بدون اینترنت کار می‌کند.",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun OccasionSection(
    occ: Occasion,
    isActive: Boolean,
    names: Map<Long, String>,
    onFoodClick: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = occ.title,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (isActive) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "فعال",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        if (occ.dishes.isEmpty()) {
            Text(
                text = "غذایی برای این مناسبت ثبت نشده",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                items(occ.dishes) { foodId ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        // #129: single-Text chip already reads as one stop; the
                        // modifier makes that a promise the audit can check.
                        modifier = Modifier
                            .clickable { onFoodClick(foodId) }
                            .oneA11yStop(names[foodId] ?: "غذای حذف‌شده"),
                    ) {
                        Text(
                            text = names[foodId] ?: "#$foodId",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = YekanBakh,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
