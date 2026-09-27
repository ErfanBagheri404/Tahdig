package com.erfanbagheri.tahdig.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.BarcodeScanEntity
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.util.OffClient
import com.erfanbagheri.tahdig.util.ScanHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Barcode scan flow (#116).
 *
 * Cache-first, and the order matters: a barcode we have already resolved is
 * answered from the local row with no network call at all, which is what makes
 * an airplane-mode scan of a known product work. Only a miss goes to OFF.
 *
 * The three outcomes are all VALUES, never exceptions, because the AC wants an
 * honest offline hint rather than an error modal.
 */
class BarcodeViewModel(app: Application) : AndroidViewModel(app) {

    private val db = TahdigDatabase.getInstance(app)

    /** Newest-first, capped in SQL so the UI cannot forget to trim. */
    val history: StateFlow<List<BarcodeScanEntity>> =
        db.barcodeScanDao().observeRecent()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _scan = MutableStateFlow<ScanState>(ScanState.Idle)
    val scan: StateFlow<ScanState> = _scan

    sealed interface ScanState {
        data object Idle : ScanState
        data object Looking : ScanState

        /** Resolved, from cache or network — [fromCache] drives the hint. */
        data class Found(val product: OffClient.Product, val fromCache: Boolean) : ScanState

        /** OFF answered cleanly; this barcode is not in their database. */
        data object NotFound : ScanState

        /** No network and no cached row — the honest offline case. */
        data object Offline : ScanState
    }

    fun reset() { _scan.value = ScanState.Idle }

    /**
     * Resolve one barcode. [cachedHint] lets the caller pass the already-loaded
     * history so a known barcode short-circuits before any I/O; when it is null
     * the DB row is read directly instead.
     */
    fun onBarcode(barcode: String, cachedHint: Set<String>? = null) {
        val code = barcode.trim()
        if (code.isEmpty()) return
        if (_scan.value is ScanState.Looking) return
        _scan.value = ScanState.Looking
        viewModelScope.launch {
            val dao = db.barcodeScanDao()
            // 1. Cache first — no network. This is the airplane-mode path.
            val cachedRow = withContext(Dispatchers.IO) { dao.byBarcode(code) }
            val knownByHint = cachedHint?.let { ScanHistory.isCached(it, code) } ?: false
            if (cachedRow != null || knownByHint) {
                val row = cachedRow ?: run {
                    // Hint said cached but the row is gone (history trimmed
                    // between the two reads). Fall through to the network
                    // rather than reporting a hit we cannot render.
                    null
                }
                if (row != null) {
                    withContext(Dispatchers.IO) { dao.upsert(row.copy(timestamp = now())) }
                    _scan.value = ScanState.Found(row.toProduct(), fromCache = true)
                    return@launch
                }
            }
            // 2. Network, one-shot. Offline is a value, not a crash.
            val result = withContext(Dispatchers.IO) { OffClient.lookup(code) }
            when (result) {
                is OffClient.Result2.Found -> {
                    withContext(Dispatchers.IO) {
                        dao.upsert(result.product.toEntity(now()))
                        dao.trimTo()
                    }
                    _scan.value = ScanState.Found(result.product, fromCache = false)
                }
                OffClient.Result2.NotFound -> _scan.value = ScanState.NotFound
                OffClient.Result2.Offline -> _scan.value = ScanState.Offline
            }
        }
    }

    /** Forget the whole history and cache. */
    fun clearHistory() {
        viewModelScope.launch { withContext(Dispatchers.IO) { db.barcodeScanDao().clear() } }
    }

    private fun now() = System.currentTimeMillis()
}

private fun BarcodeScanEntity.toProduct() = OffClient.Product(
    barcode = barcode,
    name = name,
    brands = brands,
    imageUrl = imageUrl,
    kcal100g = kcal100g,
    protein100g = protein100g,
    carb100g = carb100g,
    fat100g = fat100g,
    salt100g = salt100g,
    sugars100g = sugars100g,
    allergens = allergens.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    nutriscore = nutriscore,
    nova = null,
)

private fun OffClient.Product.toEntity(stamp: Long) = BarcodeScanEntity(
    barcode = barcode,
    name = name,
    brands = brands,
    imageUrl = imageUrl,
    kcal100g = kcal100g,
    protein100g = protein100g,
    carb100g = carb100g,
    fat100g = fat100g,
    salt100g = salt100g,
    sugars100g = sugars100g,
    allergens = allergens.joinToString(","),
    nutriscore = nutriscore,
    timestamp = stamp,
)
