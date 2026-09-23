package com.erfanbagheri.tahdig.ui.screen

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.ui.viewmodel.RatingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoodDetailScreen(
    foodId: Long,
    onBack: () -> Unit,
    onStartStepMode: (Long) -> Unit = {},
    /** Resume an interrupted session (#93) — restores exact step + countdown. */
    onResumeStepMode: (Long) -> Unit = {},
    ratingViewModel: RatingViewModel? = null,
    onAddToShoppingList: (Long, String) -> Unit = { _, _ -> },
    /** Adds only the pantry gap, so the user isn't told to rebuy what they own. */
    onAddMissing: (String) -> Unit = {},
    onShare: (FoodEntity) -> Unit = {},
    /** Mise-en-place checked-state store (#99); null renders rows without checks. */
    milestoneViewModel: com.erfanbagheri.tahdig.ui.viewmodel.MilestoneViewModel? = null,
) {
    val context = LocalContext.current.applicationContext
    var food by remember { mutableStateOf<FoodEntity?>(null) }

    // Pantry state for the "what am I missing" line. Loaded once per dish; the pantry
    // only changes while the Pantry screen is open, so a one-shot read is enough.
    var pantry by remember { mutableStateOf<List<String>>(emptyList()) }

    val recentViewDao = TahdigDatabase.getInstance(context).recentViewDao()
    // Interrupted cook session (#93): offer «ادامه بده» only when one exists.
    val cookSession by TahdigDatabase.getInstance(context).cookSessionDao()
        .observe(foodId).collectAsState(initial = null)
    LaunchedEffect(foodId) {
        val db = TahdigDatabase.getInstance(context)
        food = db.foodDao().getById(foodId)
        pantry = db.pantryDao().allItems()
        food?.let { recentViewDao.recordView(it.id) }
    }

    // ── Cooking timer ──────────────────────────────────────────────
    val haptic = LocalHapticFeedback.current
    var remainingSec by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }

    fun onTimerTick() {
        if (remainingSec <= 1) {
            running = false
            showTimerDialog = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            remainingSec -= 1
        }
    }

    TimerDialog(
        show = showTimerDialog,
        onDismiss = { showTimerDialog = false },
    )

    // ── TimerDialog is hoisted; ticks are driven from the UI coroutine ──
    LaunchedEffect(running) {
        if (running) {
            while (isActive) {
                delay(1000)
                onTimerTick()
                if (!running) break
            }
        }
    }

    val appContext = context.applicationContext
    var tts: TextToSpeech? = null
    var ttsReady by remember { mutableStateOf(false) }
    var ttsHasFa by remember { mutableStateOf(true) }
    tts = remember(appContext) {
        TextToSpeech(appContext) { status ->
            val engine = tts ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) {
                ttsHasFa = false
                ttsReady = true
                return@TextToSpeech
            }
            // Language is only set when fa-IR is confirmed available
            val result = engine.setLanguage(Locale("fa", "IR"))
            ttsHasFa = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ttsHasFa) engine.language = Locale.getDefault()
            ttsReady = true
        }
    }
    DisposableEffect(Unit) {
        onDispose { tts?.stop(); tts?.shutdown() }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(food?.name ?: "", fontFamily = YekanBakh) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (food != null) {
                        IconButton(onClick = { onShare(food!!) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "اشتراک‌گذاری",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )

            food?.let { f ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Hero: real photo when available, category emoji otherwise
                    com.erfanbagheri.tahdig.ui.components.DishPhoto(
                        imageUrl = f.imageUrl,
                        categoryId = f.categoryId,
                        height = 220.dp,
                        cornerRadius = 20.dp,
                    )

                    Spacer(Modifier.height(20.dp))

                    // Star rating
                    if (ratingViewModel != null) {
                        val dbStars by ratingViewModel.stars(f.id).collectAsState()
                        // Optimistic local value so rapid taps don't read a stale DB value
                        var localStars by remember(f.id) { mutableStateOf<Int?>(null) }
                        StarRating(
                            stars = localStars ?: dbStars,
                            onRate = {
                                localStars = it
                                ratingViewModel.setStars(f.id, it)
                            },
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    Text(
                        text = f.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = YekanBakh,
                        fontSize = 36.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    if (f.nameEn.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = f.nameEn,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    if (f.description.isNotBlank()) {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = f.description,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = YekanBakh,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    if (f.prepTimeMin > 0 || f.difficulty.isNotBlank() || f.flavors.isNotBlank()) {
                        Spacer(Modifier.height(24.dp))
                        // FlowRow so the taste badges (#89) wrap instead of overflowing.
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (f.prepTimeMin > 0) {
                                DetailChip("زمان آماده‌سازی: ${f.prepTimeMin} دقیقه")
                            }
                            if (f.difficulty.isNotBlank()) {
                                DetailChip("سختی: ${difficultyLabel(f.difficulty)}")
                            }
                            val spiceLevel = com.erfanbagheri.tahdig.util.SpiceProfile.level(f.tags, f.ingredients, f.name)
                            if (spiceLevel > 0) {
                                DetailChip(com.erfanbagheri.tahdig.util.SpiceProfile.label(spiceLevel))
                            }
                            // Taste badges next to the spice pill (#89) — baked axis labels.
                            f.flavors.split(',').forEach { raw ->
                                com.erfanbagheri.tahdig.util.Flavor.entries
                                    .firstOrNull { it.name == raw.trim() }
                                    ?.let { DetailChip(it.label) }
                            }
                        }
                    }

                    // Cooking timer
                    if (f.prepTimeMin > 0) {
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                // Toggle: start if idle, else pause/resume — never reset while running.
                                if (running) {
                                    running = false
                                } else {
                                    if (remainingSec == 0L) remainingSec = f.prepTimeMin * 60L
                                    running = true
                                }
                            },
                        ) {
                            Text(
                                when {
                                    running -> "زمان باقی: ${mmss(remainingSec)}"
                                    remainingSec > 0L -> "ادامه تایمر"
                                    else -> "شروع تایمر آشپزی"
                                },
                                fontFamily = YekanBakh,
                            )
                        }
                    }

                    // Scale state (#103): declared before nutrition + mise so both
                    // read the same servings × batch factor.
                    var servings by remember { mutableStateOf(1) }
                    var batch by remember { mutableStateOf(1.0) }
                    val scaleFactor = com.erfanbagheri.tahdig.util.ServingScaler
                        .composed(servings, batch)

                    // Nutrition: real OFF data when the ingredients are covered,
                    // else the per-category heuristic. Badge says which.
                    val real = com.erfanbagheri.tahdig.util.NutritionEstimate
                        .estimateFromIngredients(f.ingredients)
                    val nut = real?.info
                        ?: com.erfanbagheri.tahdig.util.NutritionEstimate.estimate(f.name, f.tags)
                    // Per-serving recalc (#103): chips reflect the amounts shown.
                    fun scaledCalories(): Int = Math.round(nut.calories * scaleFactor).toInt()
                    fun scaledGrams(s: String): String = if (scaleFactor == 1.0) s
                    else s.removeSuffix("g").toDoubleOrNull()
                        ?.let { "${Math.round(it * scaleFactor)}g" } ?: s
                    Spacer(Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DetailChip("~${scaledCalories()} کالری")
                        DetailChip("پروتئین ${scaledGrams(nut.protein)}")
                        DetailChip("چربی ${scaledGrams(nut.fat)}")
                        DetailChip("کربوهیدرات ${scaledGrams(nut.carb)}")
                        // Honesty badge: the user should know which tier this came from.
                        DetailChip(if (real != null) "واقعی" else "تخمینی")
                    }

                    // ── Mise-en-place (#99) ───────────────────────────────────────
                    // Parsed once per dish; display scales with servings × batch, hashes never do.
                    val checkedHashes = if (milestoneViewModel != null)
                        milestoneViewModel.checkedHashes.collectAsState().value
                    else emptySet<String>()
                    LaunchedEffect(f.id) { milestoneViewModel?.forFood(f.id) }
                    val miseParsed = remember(f.ingredients) {
                        com.erfanbagheri.tahdig.util.MisePlace.rowsOf(f.ingredients)
                    }
                    val miseRows = remember(miseParsed, scaleFactor) {
                        com.erfanbagheri.tahdig.util.MisePlace.rowsFor(miseParsed, scaleFactor)
                    }
                    val miseChecked = miseRows.map { it.hash }.toSet() intersect checkedHashes
                    val miseReady = miseRows.isNotEmpty() && miseChecked.size == miseRows.size

                    if (f.ingredients.isNotBlank()) {
                        Spacer(Modifier.height(28.dp))
                        Text(
                            text = "مواد لازم",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))

                        // Serving scale stepper
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconButton(onClick = { if (servings > 1) servings-- }) {
                                Icon(Icons.Default.Remove, contentDescription = "کم کردن")
                            }
                            Text(
                                text = "$servings نفر",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = YekanBakh,
                                fontWeight = FontWeight.Bold,
                            )
                            IconButton(onClick = { if (servings < 20) servings++ }) {
                                Icon(Icons.Default.Add, contentDescription = "زیاد کردن")
                            }
                        }

                        // Batch multiplier (#103): composes with servings above.
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            com.erfanbagheri.tahdig.util.ServingScaler.BATCHES.forEach { mult ->
                                val selected = batch == mult
                                Text(
                                    text = com.erfanbagheri.tahdig.util.PersianText
                                        .toPersianDigits("${mult}x"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = YekanBakh,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .background(
                                            color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                        .clickable { batch = mult }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        // ── Checklist: counter + reset, then tappable rows (#99) ──
                        if (miseRows.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = com.erfanbagheri.tahdig.ui.components.miseCounter(
                                        miseChecked.size, miseRows.size,
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = YekanBakh,
                                    fontWeight = FontWeight.Bold,
                                    color = if (miseReady)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                // Reset is per dish and only offered once something is checked.
                                if (miseChecked.isNotEmpty()) {
                                    TextButton(onClick = { milestoneViewModel?.clear(f.id) }) {
                                        Text("پاک کردن", fontFamily = YekanBakh)
                                    }
                                }
                            }
                            com.erfanbagheri.tahdig.ui.components.MiseChecklist(
                                rows = miseRows,
                                checkedHashes = miseChecked,
                                // No VM (call-site omitted it) => rows render but don't toggle.
                                onToggle = if (milestoneViewModel != null)
                                    { hash -> milestoneViewModel.toggle(f.id, hash) }
                                else null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // ── What's missing vs the pantry ──────────────────────────
                        // Only meaningful once the user has a pantry; with none, every
                        // ingredient would read as missing, which is noise not insight.
                        if (pantry.isNotEmpty()) {
                            val diff = com.erfanbagheri.tahdig.util.MissingDiff
                                .diff(f.ingredients, pantry)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = diff.summary(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = YekanBakh,
                                    color = if (diff.allCovered)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                // Adds just the gap, not the whole ingredient list —
                                // buying what you already have is the thing to avoid.
                                if (diff.missing.isNotEmpty()) {
                                    TextButton(onClick = {
                                        onAddMissing(diff.missing.joinToString("، ") { it.display })
                                    }) {
                                        Text("افزودن کم‌داشته‌ها", fontFamily = YekanBakh)
                                    }
                                }
                            }
                        }

                        // ── Substitutes ───────────────────────────────────────────
                        // Per-ingredient swaps, so a missing saffron is a suggestion
                        // rather than a dead end.
                        val swaps = remember(f.ingredients) {
                            com.erfanbagheri.tahdig.util.MissingDiff.itemsOf(f.ingredients)
                                .mapNotNull { name ->
                                    val list = com.erfanbagheri.tahdig.util.SubstitutionRegistry
                                        .swapsForName(name)
                                    if (list.isEmpty()) null else name to list
                                }
                        }
                        if (swaps.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            var showSubs by remember { mutableStateOf(false) }
                            TextButton(onClick = { showSubs = !showSubs }) {
                                Text(
                                    text = if (showSubs) "بستن جایگزین‌ها" else "جایگزین‌ها را ببین",
                                    fontFamily = YekanBakh,
                                )
                            }
                            if (showSubs) {
                                swaps.forEach { (from, list) ->
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = from,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontFamily = YekanBakh,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                                        )
                                        list.forEach { swap ->
                                            Text(
                                                text = "${swap.toName} — ${ratioLabel(swap.ratio)}${if (swap.caveat.isBlank()) "" else " · ${swap.caveat}"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = YekanBakh,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Unit converter (cups/grams/tbsp quick reference)
                        Spacer(Modifier.height(16.dp))
                        var showConverter by remember { mutableStateOf(false) }
                        TextButton(onClick = { showConverter = !showConverter }) {
                            Text(
                                text = if (showConverter) "بستن تبدیل واحد" else "تبدیل واحد",
                                fontFamily = YekanBakh,
                            )
                        }
                        if (showConverter) {
                            val uc = com.erfanbagheri.tahdig.util.UnitConverter
                            // Metric ⇄ ایرانی display (#103); the toggle only flips
                            // the same rows' direction, so numbers always agree.
                            var persianUnits by remember { mutableStateOf(false) }
                            val favKey by SettingsStore.convertFavorite
                                .collectAsState(initial = null)
                            val rows = if (persianUnits) uc.iranianRows() else uc.metricRows()
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                listOf("متریک" to false, "ایرانی" to true).forEach { (label, iranian) ->
                                    val selected = persianUnits == iranian
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = YekanBakh,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .background(
                                                color = if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(8.dp),
                                            )
                                            .clickable { persianUnits = iranian }
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            rows.forEach { row ->
                                val value = uc.valueOf(row) ?: return@forEach
                                val label = "${com.erfanbagheri.tahdig.util.PersianText.toPersianDigits(row.amount)} " +
                                    "${row.from} = ${com.erfanbagheri.tahdig.util.PersianText.toPersianDigits(value)} ${row.to}"
                                val isFav = row.key == favKey
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = YekanBakh,
                                        color = if (isFav) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                    )
                                    // One-tap favorite (#103): remembered across restart.
                                    Text(
                                        text = if (isFav) "★" else "☆",
                                        fontFamily = YekanBakh,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clickable { SettingsStore.setConvertFavorite(row.key) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        Button(
                            // Same mergeInto path as before (#103): the whole blob is
                            // pre-scaled once, so dedupe sees the scaled quantities.
                            onClick = {
                                onAddToShoppingList(
                                    f.id,
                                    if (scaleFactor == 1.0) f.ingredients
                                    else com.erfanbagheri.tahdig.util.ServingScaler
                                        .scaleAll(f.ingredients, scaleFactor),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("افزودن به لیست خرید", fontFamily = YekanBakh)
                        }
                    }

                    if (f.tags.isNotBlank()) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = "برچسب‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = f.tags,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    // ── Equipment (#100) ──────────────────────────────────────────
                    // Baked seed list first, keyword inference as fallback; «آماده»
                    // toggles are session-scoped (remember), deliberately not persisted.
                    val tools = remember(f.equipment, f.ingredients, f.description) {
                        com.erfanbagheri.tahdig.util.EquipmentInferrer.forDish(
                            f.equipment, f.description, f.ingredients,
                        )
                    }
                    var readyTools by remember(f.id) { mutableStateOf(emptySet<String>()) }
                    if (tools.isNotEmpty()) {
                        com.erfanbagheri.tahdig.ui.components.EquipmentRow(
                            labels = tools,
                            readyLabels = readyTools,
                            onToggle = { label ->
                                readyTools = if (label in readyTools) readyTools - label
                                else readyTools + label
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    // All-checked emphasis: a border color change on the cook entry,
                    // not a nag — readiness should feel earned, not demanded.
                    val cookBorder = when {
                        miseRows.isEmpty() -> null
                        miseReady -> androidx.compose.foundation.BorderStroke(
                            2.dp, MaterialTheme.colorScheme.primary,
                        )
                        else -> androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    Button(
                        onClick = { onStartStepMode(f.id) },
                        border = cookBorder,
                    ) {
                        Text("حالت پخت مرحله‌به‌مرحله", fontFamily = YekanBakh)
                    }

                    // Resume offer (#93): an interrupted session jumps straight back
                    // to its step + countdown. Renders above the fresh-start button.
                    if (cookSession != null) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { onResumeStepMode(f.id) }) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "ادامه بده",
                                fontFamily = YekanBakh,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { tts?.speak(f.ingredients, TextToSpeech.QUEUE_FLUSH, null, "tahdig") },
                        enabled = ttsReady,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "گوش دادن به مواد",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("گوش دادن به مواد", fontFamily = YekanBakh)
                    }

                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun StarRating(stars: Int, onRate: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { i ->
            IconButton(onClick = { onRate(if (stars == i) 0 else i) }) {
                Icon(
                    imageVector = if (i <= stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$i ستاره",
                    tint = if (i <= stars) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TimerDialog(show: Boolean, onDismiss: () -> Unit) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تایمر تمام شد", fontFamily = YekanBakh) },
        text = { Text("زمان آماده‌سازی به پایان رسید!", fontFamily = YekanBakh) },
        confirmButton = {
            Button(onClick = onDismiss) { Text("باشه", fontFamily = YekanBakh) }
        },
    )
}

private fun mmss(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    val mm = if (m < 10) "0$m" else "$m"
    val ss = if (s < 10) "0$s" else "$s"
    return "$mm:$ss"
}

@Composable
private fun DetailChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontFamily = YekanBakh,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

private fun difficultyLabel(d: String): String = when (d.uppercase()) {
    "EASY" -> "آسان"
    "MEDIUM" -> "متوسط"
    "HARD" -> "سخت"
    else -> d
}

/**
 * Human phrasing for a swap's ratio. A ratio of 1.0 is not a "×1.0" to the reader —
 * "همان مقدار" says what it means; 0.0 marks a swap the table flags as incomplete.
 */
private fun ratioLabel(ratio: Double): String = when {
    ratio <= 0.0 -> "جایگزین کامل نیست"
    kotlin.math.abs(ratio - 1.0) < 0.001 -> "همان مقدار"
    else -> "با نسبت ${com.erfanbagheri.tahdig.util.PersianText.toPersianDigits(trimNum(ratio))}"
}

private fun trimNum(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
