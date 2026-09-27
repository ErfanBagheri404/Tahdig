package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.BarcodeViewModel

/**
 * Barcode scanner screen (#116).
 *
 * Layout: a fixed-height camera window on top (the preview needs a bounded
 * height inside a scrolling parent), then whatever the scan produced, then the
 * recent-scan history. The camera keeps running while a result is shown so the
 * next product is one barcode away — re-arming a camera per scan would be a
 * visible stall.
 */
@Composable
fun BarcodeScreen(
    viewModel: BarcodeViewModel,
    onAddToShopping: (String) -> Unit,
    onAddToPantry: (String) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.scan.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val cachedBarcodes = history.map { it.barcode }.toSet()

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        item {
            // Back button first, matching every other overlay in this app.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text("بازگشت", fontFamily = YekanBakh)
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            ) {
                BarcodeCameraPreview(
                    onBarcode = { code -> viewModel.onBarcode(code, cachedBarcodes) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── The scan outcome ─────────────────────────────────────────
        // Four states, and the offline one is deliberately NOT an error: the
        // AC wants an honest hint, and a modal for "no network" treats a
        // normal situation like a failure.
        when (val s = state) {
            is BarcodeViewModel.ScanState.Looking -> item {
                Text(
                    "در حال جست‌وجو…",
                    fontFamily = YekanBakh,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
            }

            is BarcodeViewModel.ScanState.Found -> item {
                OffProductCard(
                    product = s.product,
                    fromCache = s.fromCache,
                    onAddToShopping = { onAddToShopping(s.product.name) },
                    onAddToPantry = { onAddToPantry(s.product.name) },
                )
                Spacer(Modifier.height(12.dp))
            }

            is BarcodeViewModel.ScanState.NotFound -> item {
                Text(
                    "این بارکد در پایگاه داده نیست.",
                    fontFamily = YekanBakh,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
            }

            is BarcodeViewModel.ScanState.Offline -> item {
                Text(
                    "آنلاین نیست — این بارکد قبلاً اسکن نشده.",
                    fontFamily = YekanBakh,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
            }

            is BarcodeViewModel.ScanState.Idle -> Unit
        }

        // ── History ──────────────────────────────────────────────────
        if (history.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "اسکن‌های اخیر",
                        fontFamily = YekanBakh,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Text("پاک کردن", fontFamily = YekanBakh)
                    }
                }
            }
            items(history, key = { it.barcode }) { row ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(
                        row.name,
                        fontFamily = YekanBakh,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        row.barcode,
                        fontFamily = YekanBakh,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
