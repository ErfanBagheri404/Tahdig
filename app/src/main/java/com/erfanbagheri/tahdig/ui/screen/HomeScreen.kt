package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import com.erfanbagheri.tahdig.ui.components.minTouchTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.erfanbagheri.tahdig.ui.components.FirstRunTip
import com.erfanbagheri.tahdig.ui.components.FirstSuccessNote
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.FirstRun
import com.erfanbagheri.tahdig.ui.components.DishPhoto
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.util.DietFilter
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.flow.MutableStateFlow
import com.erfanbagheri.tahdig.util.Haptics
import com.erfanbagheri.tahdig.util.MealTimeHelper
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.RouletteMath
import com.erfanbagheri.tahdig.util.ShakeDetector
import com.erfanbagheri.tahdig.util.ShakeWatcher
import kotlin.math.cos
import kotlin.math.sin
import com.erfanbagheri.tahdig.ui.viewmodel.HomeViewModel
import com.erfanbagheri.tahdig.ui.viewmodel.WellnessViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    wellness: com.erfanbagheri.tahdig.ui.viewmodel.WellnessViewModel? = null,
    caffeine: com.erfanbagheri.tahdig.ui.viewmodel.CaffeineViewModel? = null,
    badges: com.erfanbagheri.tahdig.ui.viewmodel.BadgeViewModel? = null,
    onBrowseCategories: () -> Unit = {},
    onFoodClick: (Long) -> Unit = {},
    onOpenLeftover: () -> Unit = {},
    /** «رو به اتمام» card taps through to the pantry (#106). */
    onOpenPantry: () -> Unit = {},
) {
    val suggestion by viewModel.suggestion.collectAsState()
    val mealLabel by viewModel.mealLabel.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val view = LocalView.current
    val dishOfDay by viewModel.dishOfDay.collectAsState()
    val expirySummary by viewModel.expirySummary.collectAsState()
    val nutritionDay by viewModel.nutritionDay.collectAsState()
    val streakUi by viewModel.streakUi.collectAsState()
    val leftoverSuggestions by viewModel.leftoverSuggestions.collectAsState()
    val occasion by viewModel.occasion.collectAsState()
    val occasionDishes by viewModel.occasionDishes.collectAsState()
    // Dinner roulette (#123)
    val spinOpen by viewModel.spinOpen.collectAsState()
    val spinPool by viewModel.spinPool.collectAsState()
    val spinPick by viewModel.spinPick.collectAsState()
    val spinToken by viewModel.spinToken.collectAsState()
    val spinBucket by viewModel.spinBucket.collectAsState()
    val spinDiet by viewModel.spinDiet.collectAsState()
    val journalPending by viewModel.journalPending.collectAsState()
    val shakeSpin by viewModel.shakeSpin.collectAsState()
    val shakeSensitivity by viewModel.shakeSensitivity.collectAsState()

    // Badge unlocks (#121): a non-blocking snackbar, never a push notification.
    // The issue is explicit that streak-at-risk owns the nudges — a badge is
    // a reward for something already done, so it must not interrupt later.
    val badgeHostState = remember { SnackbarHostState() }
    val newBadges by (badges?.newlyUnlocked ?: remember {
        MutableStateFlow(emptyList())
    }).collectAsState()
    LaunchedEffect(newBadges) {
        val first = newBadges.firstOrNull() ?: return@LaunchedEffect
        Haptics.confirm(view)
        val more = if (newBadges.size > 1) " (+${newBadges.size - 1})" else ""
        badgeHostState.showSnackbar("نشان جدید: ${first.title}$more")
        badges?.acknowledge()
    }

    // Shake-to-spin (#123): the watcher only lives while this screen does —
    // entering cook mode disposes it, so the two gestures can't collide.
    DisposableEffect(view, shakeSpin, shakeSensitivity) {
        val watcher = ShakeWatcher(view.context) { viewModel.openSpin() }
        watcher.threshold = ShakeDetector.thresholdFor(shakeSensitivity)
        watcher.enabled = shakeSpin
        if (shakeSpin) watcher.start()
        onDispose { watcher.stop() }
    }
    // Refresh day-dependent state when app returns to foreground (midnight-safe).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDay()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Freeze decision prompt (#120): only when a freeze is the difference
    // between holding the streak and losing it, and not declined today.
    if (streakUi.promptVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.declineFreeze() },
            title = {
                Text(
                    "امروز نپختی — انجماد مصرف بشه؟",
                    fontFamily = YekanBakh,
                )
            },
            text = {
                Text(
                    "با این انجماد سری پختت حفظ می‌شه. " +
                        "ماهی یکی داری: " +
                        PersianText.toPersianDigits(
                            streakUi.state.freezesLeft.toString(),
                        ) + " تا.",
                    fontFamily = YekanBakh,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.acceptFreeze() }) {
                    Text("بله، انجماد", fontFamily = YekanBakh)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.declineFreeze() }) {
                    Text("نه", fontFamily = YekanBakh)
                }
            },
        )
    }

    // Journal capture (#124): after «پختم». Both fields optional and the
    // dialog is dismissible — the memory is already stamped either way.
    journalPending?.let { cooked ->
        JournalCaptureDialog(
            dishName = cooked.name,
            onPhoto = { uri -> viewModel.attachJournalPhoto(uri) },
            onSave = { note ->
                viewModel.attachJournalNote(note)
                viewModel.dismissJournalPrompt()
            },
            onDismiss = viewModel::dismissJournalPrompt,
        )
    }

    val homeScroll = rememberScrollState()
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        // #127: pull-to-refresh = local re-shuffle. Offline-first, so it never
        // pretends to fetch — it re-ranks the feed from what is already here.
        com.erfanbagheri.tahdig.ui.components.LocalPullToRefresh(
            scrollState = homeScroll,
            onRefresh = { viewModel.roll(); viewModel.refreshDay() },
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // The action row (چرخوندن incl.) fell off the bottom once the
                // nutrition/streak sections stacked up — scroll instead of clipping.
                .verticalScroll(homeScroll)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // Permission priming (#122): the primer lives here because this
            // screen owns the cook event — it shows exactly once per session,
            // only after the first cook and only when reminders are off. A
            // dismissal hides it for the session; the prefs outcome persists.
            val primeDismissed = remember { mutableStateOf(false) }
            val primed by remember { SettingsStore.notifPrimed }.collectAsState()
            val notifyOn by remember { SettingsStore.dailyNotify }.collectAsState()
            val cookedAny by viewModel.everCooked.collectAsState()
            if (!primed && !notifyOn && cookedAny && !primeDismissed.value) {
                NotificationPrimingRow(onDismiss = { primeDismissed.value = true })
                Spacer(Modifier.height(16.dp))
            }

            // Meal label pill + streak number (#120). Streak renders only
            // when alive — a 0 would read as failure, not information.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = mealLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
                if (streakUi.state.current > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "\uD83D\uDD25 " +
                            PersianText.toPersianDigits(streakUi.state.current.toString()) + " روز",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Dish of the day (deterministic by date)
            dishOfDay?.let { dod ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🌟 غذای امروز",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = dod.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = YekanBakh,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
            TextButton(onClick = onBrowseCategories) {
                Text("مرور دسته‌بندی‌ها", fontFamily = YekanBakh)
            }
            // #107: the zero-waste daily trigger lives next to the browse entry.
            TextButton(onClick = onOpenLeftover) {
                Text("غذای مونده دارم", fontFamily = YekanBakh)
            }
            Spacer(Modifier.height(12.dp))

            // ── First-run hints (#126) ─────────────────────────────────────
            // One line each, shown until dismissed, never returning after.
            // Placed below the suggestion so they never push the dish off screen.
            FirstRunTip(
                id = FirstRun.Tip.HOME_REROLL,
                text = "«غذای دیگه» یه انتخاب تازه میاره؛ قلب برای ذخیره، دست برای مسدود کردن.",
            )
            FirstRunTip(
                id = FirstRun.Tip.PANTRY,
                text = "مواد خونه‌ت رو بگو تا بگیم چی می‌تونی همین حالا بپزی.",
            )
            // Post-first-cook done state: a pointer at the next action.
            FirstSuccessNote()
            Spacer(Modifier.height(24.dp))

            // ── Expiry summary (#106) ──────────────────────────────────────
            // Computed from Room alone; the card emits NOTHING when the string is
            // empty, so a fresh pantry shows no empty row.
            if (expirySummary.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenPantry),
                ) {
                    Text(
                        text = expirySummary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Today's budget (#110) ──────────────────────────────────────
            // No-goal mode renders the totals line only — the card must never
            // draw a ring it has no target for (AC: بدون هدف = totals only).
            nutritionDay?.let { day ->
                // Daily sodium against the cap (#117): resolve pass/warn/fail
                // here with the same pure rule the report uses. Hidden when no
                // cap is set or no meal had real data.
                val sodium by viewModel.dailySodium.collectAsState()
                val sodiumCap by viewModel.sodiumCap.collectAsState()
                val sodiumLine: String? = sodiumCap?.let { cap ->
                    if (sodium < 0.0) "سدیم امروز: بدون داده"
                    else {
                        val status = com.erfanbagheri.tahdig.util.MicroNutrients
                            .dailyStatus(sodium, cap)
                        val whole = sodium.toLong().toString()
                        "${status?.symbol} سدیم امروز: ${
                            PersianText.toPersianDigits(whole)
                        } از ${PersianText.toPersianDigits(cap.toLong().toString())} — ${
                            status?.label ?: ""
                        }"
                    }
                }
                val sodiumColor = sodiumCap?.let { cap ->
                    when (com.erfanbagheri.tahdig.util.MicroNutrients.dailyStatus(sodium, cap)) {
                        com.erfanbagheri.tahdig.util.NutrientCaps.Status.FAIL ->
                            MaterialTheme.colorScheme.error
                        com.erfanbagheri.tahdig.util.NutrientCaps.Status.WARN ->
                            MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                } ?: MaterialTheme.colorScheme.onSurfaceVariant
                TodayCard(day, sodiumLine, sodiumColor)
                Spacer(Modifier.height(16.dp))
            }

            // Water + weight (#115). `wellness` is optional so callers that
            // never constructed a WellnessViewModel still compile.
            if (wellness != null) {
                val water by wellness.todayWater.collectAsState()
                val week by wellness.weekWater.collectAsState()
                val glass by wellness.glassSizeMl.collectAsState()
                WaterCard(
                    consumedMl = water,
                    targetMl = wellness.targetMl(),
                    week = week,
                    glassSizeMl = glass,
                    onAdd = wellness::addWater,
                )
                Spacer(Modifier.height(16.dp))

                val weightRows by wellness.weightEntries.collectAsState()
                WeightTrendCard(
                    entries = weightRows.map {
                        com.erfanbagheri.tahdig.util.WeightTrend.Entry(
                            java.time.LocalDate.ofEpochDay(it.epochDay),
                            it.kg,
                        )
                    },
                    goalStale = wellness.goalStale(),
                    onUpdateGoalPrompt = wellness::setGoalWeightFromLatest,
                    onLog = wellness::logWeight,
                )
                Spacer(Modifier.height(16.dp))
            }

            // Caffeine + pregnancy mode (#119). Optional like `wellness`, so
            // previews and tests that never build the VM still compile.
            if (caffeine != null) {
                val mg by caffeine.todayMg.collectAsState()
                val cap by caffeine.effectiveCap.collectAsState()
                val mode by caffeine.pregnancyMode.collectAsState()
                val rows by caffeine.todayRows.collectAsState()
                val week by caffeine.weekMg.collectAsState()
                CaffeineCard(
                    todayMg = mg,
                    capMg = cap,
                    pregnancyMode = mode,
                    rows = rows,
                    week = week,
                    onLogPreset = caffeine::logPreset,
                    onLogCustom = caffeine::logCustom,
                    onRemove = caffeine::remove,
                )
                Spacer(Modifier.height(16.dp))
            }

            // Weekly floor progress (#120) — flat line, no card.
            Text(
                text = "این هفته " +
                    PersianText.toPersianDigits(streakUi.state.thisWeek.toString()) +
                    " از " +
                    PersianText.toPersianDigits(streakUi.state.weeklyFloor.toString()) +
                    " پخت",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            // ── Occasion shelf (#88) ───────────────────────────────────────
            // Renders only while an occasion window is active — outside every
            // window this emits nothing at all, so no empty row can appear (AC).
            occasion?.let { occ ->
                if (occasionDishes.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = occ.shelf,
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "مناسبت‌ها",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(occasionDishes, key = { it.id }) { food ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                ),
                                modifier = Modifier.clickable { onFoodClick(food.id) },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = food.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontFamily = YekanBakh,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            leftoverSuggestions.takeIf { it.isNotEmpty() }?.let { leftovers ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "از این چیزی مونده؟ فردا با باقی‌مونده‌ش چی درست کنم؟",
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            // #129: 48dp hit box on the 18dp glyph (minTouchTarget).
                            IconButton(
                                onClick = viewModel::dismissLeftover,
                                modifier = Modifier.minTouchTarget(),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "بستن",
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(leftovers, key = { it.id }) { food ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.clickable { onFoodClick(food.id) },
                                ) {
                                    Text(
                                        text = food.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontFamily = YekanBakh,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Suggestion card
            if (suggestion != null) {
                SuggestionCard(
                    food = suggestion!!,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = "در حال انتخاب…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Spin sheet (#123) — wheel of the current filter pool.
            if (spinOpen) {
                SpinSheet(
                    pool = spinPool,
                    pick = spinPick,
                    token = spinToken,
                    bucket = spinBucket,
                    diet = spinDiet,
                    onPickFilters = { b, d -> viewModel.spinWith(b, d) },
                    onVeto = viewModel::vetoSpin,
                    onClose = viewModel::closeSpin,
                    onOpenDish = { id ->
                        viewModel.closeSpin()
                        onFoodClick(id)
                    },
                    onLanded = { Haptics.confirm(view) },
                )
            }

            // No weight() here — a scrolling column has unbounded height.
            Spacer(Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Favorite toggle
                IconButton(
                    onClick = {
                        Haptics.tap(view)
                        viewModel.toggleFavorite()
                    },
                    enabled = suggestion != null,
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }

                // Cooked — triggers the leftover prompt
                IconButton(
                    onClick = {
                        Haptics.confirm(view)
                        viewModel.markCooked()
                        // Re-evaluate badges right after the cook, which is the
                        // only moment a badge can newly unlock.
                        badges?.refresh()
                    },
                    enabled = suggestion != null,
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "پختم",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }

                // Re-roll
                Button(
                    onClick = {
                        Haptics.tap(view)
                        viewModel.roll()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "غذای دیگه",
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("غذای دیگه")
                }

                // Dinner roulette (#123)
                Button(
                    onClick = {
                        Haptics.tap(view)
                        viewModel.openSpin()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("چرخوندن")
                }

                // Block current
                Button(
                    onClick = { viewModel.toggleBlocked() },
                    enabled = suggestion != null,
                    modifier = Modifier
                        .size(52.dp),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "دیگه اینو نده",
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }   // Column
        }   // LocalPullToRefresh
    }       // Surface

        // Badge unlock toast (#121): non-blocking, above the nav bar.
        SnackbarHost(
            hostState = badgeHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp, start = 16.dp, end = 16.dp),
        )
    }
}

/**
 * «امروز» card (#110): consumed vs target. Rings are thin flat arcs — the
 * house look forbids gradient candy, and a full circle simply means "at or
 * over target" (the fraction is clamped upstream).
 *
 * With no goal the card degrades to a single totals line, deliberately: a ring
 * without a target is a lie.
 */
@Composable
fun TodayCard(
    day: com.erfanbagheri.tahdig.util.NutritionDay,
    sodiumLine: String? = null,
    sodiumColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "امروز",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            if (!day.hasGoal) {
                Text(
                    text = "${PersianText.toPersianDigits(day.consumedCal.toString())} کیلوکالری خورده\u200cشده",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "برای هدف‌گذاری، پروفایل رو در تنظیمات پر کن",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RingColumn("کالری", day.rings[0], day.consumedCal, day.targetCal)
                    RingColumn("پروتئین", day.rings[1], day.consumedProtein, day.macroTarget?.first ?: 0)
                    RingColumn("کربوهیدرات", day.rings[2], day.consumedCarbs, day.macroTarget?.second ?: 0)
                    RingColumn("چربی", day.rings[3], day.consumedFat, day.macroTarget?.third ?: 0)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (day.remainingCal >= 0) {
                        "${PersianText.toPersianDigits(day.remainingCal.toString())} کیلوکالری مونده"
                    } else {
                        "${PersianText.toPersianDigits((-day.remainingCal).toString())} کیلوکالری بیشتر از هدف"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = YekanBakh,
                    color = if (day.remainingCal >= 0) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                // Daily sodium against the cap (#117). Only shown when a
                // sodium cap is set and at least one logged meal had real
                // data — a day of estimates reports unknown, not zero.
                val sodium = sodiumLine
                if (sodium != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = sodium,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = YekanBakh,
                        color = sodiumColor,
                    )
                }
            }
        }
    }
}

/** One flat ring + its label + «x از y» line. */
@Composable
private fun RingColumn(label: String, fraction: Double, consumed: Int, target: Int) {
    // Theme colors are composable reads — DrawScope has no theme access, so
    // they are captured out here and used inside the Canvas below.
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val arcColor = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(56.dp)) {
                val stroke = 4.dp.toPx()
                // Track first, then the arc on top — no gradient, no shadow.
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke),
                )
                if (fraction > 0) {
                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = (360.0 * fraction).toFloat(),
                        useCenter = false,
                        style = Stroke(width = stroke),
                    )
                }
            }
            Text(
                text = PersianText.toPersianDigits("${(fraction * 100).toInt()}%"),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${PersianText.toPersianDigits(consumed.toString())} از ${PersianText.toPersianDigits(target.toString())}",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = YekanBakh,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SuggestionCard(
    food: com.erfanbagheri.tahdig.data.local.entity.FoodEntity,
    modifier: Modifier = Modifier,
) {
    val accent = com.erfanbagheri.tahdig.util.FoodVisuals.accent(food.categoryId)
    val emoji = com.erfanbagheri.tahdig.util.FoodVisuals.emoji(food.categoryId)

    Column(
        modifier = modifier
            .semantics { contentDescription = "پیشنهاد غذا: ${food.name}" }
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero: real photo when available, category emoji otherwise
        DishPhoto(
            imageUrl = food.imageUrl,
            categoryId = food.categoryId,
            height = 200.dp,
            cornerRadius = 0.dp,
            modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        )

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        // Food name — big, centered
        Text(
            text = food.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontFamily = YekanBakh,
            fontSize = 32.sp,
        )

        // Description
        if (food.description.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = food.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontFamily = YekanBakh,
            )
        }

        // Meta row: prep time + difficulty
        if (food.prepTimeMin > 0 || food.difficulty.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (food.prepTimeMin > 0) {
                    MetaChip("زمان آماده‌سازی: ${food.prepTimeMin} دقیقه")
                }
                if (food.difficulty.isNotBlank()) {
                    MetaChip("سختی: ${difficultyLabel(food.difficulty)}")
                }
            }
        }

        // Ingredients
        if (food.ingredients.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "مواد لازم",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = food.ingredients,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                fontFamily = YekanBakh,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        }
    }
}

@Composable
fun MetaChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private fun difficultyLabel(d: String): String = when (d.uppercase()) {
    "EASY" -> "آسان"
    "MEDIUM" -> "متوسط"
    "HARD" -> "سخت"
    else -> d
}

/** Meal buckets as (key, Farsi label) pairs for the sheet's زمان chips. */
private val MEAL_BUCKETS = listOf(
    MealTimeHelper.BREAKFAST to MealTimeHelper.BREAKFAST_FA,
    MealTimeHelper.LUNCH to MealTimeHelper.LUNCH_FA,
    MealTimeHelper.DINNER to MealTimeHelper.DINNER_FA,
    MealTimeHelper.SNACK to MealTimeHelper.SNACK_FA,
    MealTimeHelper.LIGHT_DINNER to MealTimeHelper.LIGHT_DINNER_FA,
)

/**
 * Dinner roulette (#123): wheel of the current filter pool.
 *
 * The winner is decided up-front in the ViewModel ([RouletteMath.pick]) —
 * this animation only *lands* it, so a dropped frame can never change the
 * dish. Labels orbit with their slice and stay upright.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SpinSheet(
    pool: List<FoodEntity>,
    pick: FoodEntity?,
    token: Long,
    bucket: String,
    diet: DietFilter?,
    onPickFilters: (String, DietFilter?) -> Unit,
    onVeto: () -> Unit,
    onClose: () -> Unit,
    onOpenDish: (Long) -> Unit,
    onLanded: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "چرخوندن",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = YekanBakh,
            )
            Spacer(Modifier.height(12.dp))

            // Pre-spin filters (#123): زمان + رژیمی — changing either
            // rebuilds the pool and spins again.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                MEAL_BUCKETS.forEach { (key, fa) ->
                    FilterChip(
                        selected = bucket == key,
                        onClick = { onPickFilters(key, diet) },
                        label = { Text(fa, fontFamily = YekanBakh) },
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                FilterChip(
                    selected = diet == null,
                    onClick = { onPickFilters(bucket, null) },
                    label = { Text("همه", fontFamily = YekanBakh) },
                )
                DietFilter.values().forEach { d ->
                    FilterChip(
                        selected = diet == d,
                        onClick = { onPickFilters(bucket, d) },
                        label = { Text(d.label, fontFamily = YekanBakh) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            if (pool.isEmpty() || pick == null) {
                Text(
                    text = "چیزی برای چرخوندن نیست",
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onClose) { Text("بستن", fontFamily = YekanBakh) }
                return@Column
            }

            val rotation = remember { Animatable(0f) }
            var landed by remember(token) { mutableStateOf(false) }
            val winnerIndex = pool.indexOfFirst { it.id == pick.id }
            val target = RouletteMath.landingDegrees(winnerIndex, pool.size)
            LaunchedEffect(token) {
                landed = false
                rotation.snapTo(0f)
                rotation.animateTo(target, tween(1600))
                landed = true
                onLanded()
            }

            // Theme reads happen in composition — DrawScopes aren't composable.
            val cWin = MaterialTheme.colorScheme.primaryContainer
            val cEven = MaterialTheme.colorScheme.surfaceVariant
            val cOdd = MaterialTheme.colorScheme.secondaryContainer
            val cSpoke = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            val cPointer = MaterialTheme.colorScheme.primary
            val cLabel = MaterialTheme.colorScheme.onSurface

            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Slices spin…
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = rotation.value),
                ) {
                    val arc = 360f / pool.size
                    val r = size.minDimension / 2f
                    pool.forEachIndexed { i, food ->
                        val start = -90f + i * arc
                        val fill = when {
                            food.id == pick.id && landed -> cWin
                            i % 2 == 0 -> cEven
                            else -> cOdd
                        }
                        drawArc(
                            color = fill,
                            startAngle = start,
                            sweepAngle = arc,
                            useCenter = true,
                            style = Fill,
                        )
                        // Hairline border on the arc curve…
                        drawArc(
                            color = cSpoke,
                            startAngle = start,
                            sweepAngle = arc,
                            useCenter = false,
                            style = Stroke(width = 1f),
                        )
                        // …and on each radial spoke.
                        val rad = Math.toRadians(start.toDouble())
                        drawLine(
                            color = cSpoke,
                            start = center,
                            end = androidx.compose.ui.geometry.Offset(
                                center.x + (r * sin(rad)).toFloat(),
                                center.y - (r * cos(rad)).toFloat(),
                            ),
                            strokeWidth = 1f,
                        )
                    }
                }

                // …labels orbit with their slice, always upright.
                val ring = 96.dp
                pool.forEachIndexed { i, food ->
                    val ang = Math.toRadians(RouletteMath.sliceCenterDeg(i, pool.size) + rotation.value)
                    Text(
                        text = if (food.name.length > 12) food.name.take(11) + "…" else food.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = YekanBakh,
                        color = cLabel,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = (ring.value * sin(ang)).dp, y = (-ring.value * cos(ang)).dp),
                    )
                }

                // Static pointer — the wheel comes to it, not the reverse.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val p = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w / 2f - 10f, 0f)
                        lineTo(w / 2f + 10f, 0f)
                        lineTo(w / 2f, 24f)
                        close()
                    }
                    drawPath(p, cPointer)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = pick.name,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = YekanBakh,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOpenDish(pick.id) }) {
                    Text("می‌بینمش", fontFamily = YekanBakh)
                }
                OutlinedButton(onClick = onVeto) {
                    Text("نه", fontFamily = YekanBakh)
                }
            }
            TextButton(onClick = onClose) { Text("بستن", fontFamily = YekanBakh) }
        }
    }
}
