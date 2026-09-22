package com.erfanbagheri.tahdig.ui.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.erfanbagheri.tahdig.notify.TimerScheduler
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erfanbagheri.tahdig.data.local.entity.CookSessionEntity
import com.erfanbagheri.tahdig.data.local.entity.HistoryEntity
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.CookSessionMath
import com.erfanbagheri.tahdig.util.CookVoiceCommands
import com.erfanbagheri.tahdig.util.CookVoiceSession
import com.erfanbagheri.tahdig.util.DurationParser
import com.erfanbagheri.tahdig.util.Haptics
import com.erfanbagheri.tahdig.util.KeepAwake
import com.erfanbagheri.tahdig.util.MealTimeHelper
import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.ShakeDetector
import com.erfanbagheri.tahdig.util.ShakeWatcher
import com.erfanbagheri.tahdig.util.ShareCard
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Sentence-split shared by composition and the session loader — one definition, no drift. */
internal fun splitSteps(description: String): List<String> =
    description.split(Regex("[.!؟\\n]+")).map { it.trim() }.filter { it.isNotBlank() }
        .ifEmpty { listOf(description) }

/**
 * Guided cooking: one step at a time, with an inline timer when the step states a
 * duration, and the screen held awake so it does not sleep mid-recipe.
 */
@Composable
fun StepModeScreen(
    foodId: Long,
    onBack: () -> Unit,
    /** Restore an interrupted session instead of starting fresh (#93). */
    resume: Boolean = false,
    /** False while a technique overlay owns the system back button (#101). */
    backEnabled: Boolean = true,
    /** Opens a linked technique page (#101); back returns to this step. */
    onTechnique: (String) -> Unit = {},
    /** Done-state «امتیاز بده» (#98): routes to the detail screen's rating row. */
    onRate: () -> Unit = {},
    /** Done-state «پختم» (#98): feeds heatmap via history + leftover prompt. */
    onCooked: (com.erfanbagheri.tahdig.data.local.entity.FoodEntity) -> Unit = {},
) {
    val appContext = LocalContext.current.applicationContext
    val uiContext = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    // ── Hands-free voice control (#94) ─────────────────────────────
    // State + helpers declared BEFORE `food`/`steps`/navigation so the session
    // callback can reference them; the effect that starts/stops listening sits
    // AFTER `goTo` exists (forward reference is illegal in Compose).
    if (!SettingsStore.isInitialized()) SettingsStore.init(appContext)
    val voiceMasterOn by SettingsStore.voiceControl.collectAsState(initial = false)
    val voiceReadAloud by SettingsStore.voiceReadAloud.collectAsState(initial = false)
    // Shake-to-advance (#96): opt-in, gated by the sub-60s guard + re-arm.
    val shakeOn by SettingsStore.shakeAdvance.collectAsState(initial = false)
    val shakeSensitivity by SettingsStore.shakeSensitivity.collectAsState(initial = 0.5f)
    // Visual ack flash (#96): shown when a shake (or a11y action) advances.
    var shakeFlashTick by remember { mutableIntStateOf(0) }
    var shakeFlashVisible by remember { mutableStateOf(false) }
    LaunchedEffect(shakeFlashTick) {
        if (shakeFlashTick == 0) return@LaunchedEffect
        shakeFlashVisible = true
        delay(1200)
        shakeFlashVisible = false
    }

    var micGranted by remember { mutableStateOf(false) }
    var voiceEcho by remember { mutableStateOf<String?>(null) }
    // Re-trigger for the REPEAT command (same text, needs its own launch).
    var repeatTick by remember { mutableIntStateOf(0) }

    // ── Session polish (#98) ───────────────────────────────────────
    // Undo: which step to restore (-1 = nothing pending) + a nonce that
    // (re)starts the 5s snackbar — forward moves only.
    var undoStep by remember { mutableIntStateOf(-1) }
    var undoNonce by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    // Manual keep-awake override, per cook session (default off = battery rule).
    var manualKeepAwake by remember { mutableStateOf(false) }
    // Done state (#98): celebration covers the mode until a route is chosen.
    var showDone by rememberSaveable { mutableStateOf(false) }

    // ── Cook session state (#93) ───────────────────────────────────
    // Declared above the loader that writes them. Written on state changes only
    // (step move, pause, resume) — never per timer tick; restore math is
    // CookSessionMath's job.
    var current by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(false) }
    var fired by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    val sessionDao = remember(appContext) {
        com.erfanbagheri.tahdig.data.local.TahdigDatabase
            .getInstance(appContext).cookSessionDao()
    }
    // Done-state «پختم» (#98) logs a history row here — heatmap + coverage read it.
    val historyDao = remember(appContext) {
        com.erfanbagheri.tahdig.data.local.TahdigDatabase
            .getInstance(appContext).historyDao()
    }
    val scope = rememberCoroutineScope()

    var food by remember {
        mutableStateOf<com.erfanbagheri.tahdig.data.local.entity.FoodEntity?>(null)
    }
    LaunchedEffect(foodId) {
        val db = com.erfanbagheri.tahdig.data.local.TahdigDatabase.getInstance(appContext)
        val f = db.foodDao().getById(foodId)
        // Read the session BEFORE exposing `food` — once food lands, the UI may
        // persist a fresh row, and a late read would restore that instead (#93).
        val sess = if (resume) db.cookSessionDao().get(foodId) else null
        food = f
        val list = splitSteps(f?.description ?: "")
        val target = sess?.let { CookSessionMath.restoreStep(it.stepIndex, list.size) } ?: 0
        val dur = list.getOrNull(target)?.let { DurationParser.first(it) }
        if (sess != null) {
            remaining = if (dur == null) 0L else CookSessionMath.restoreRemaining(
                sess.remainingMs, sess.updatedAt, System.currentTimeMillis(), sess.paused,
            )
            running = dur != null && !sess.paused && remaining > 0
        } else {
            remaining = dur?.seconds ?: 0L
            running = false
        }
        fired = false
        current = target
        // Normalize updatedAt so the next restore measures from now, not history.
        sessionDao.save(
            CookSessionEntity(
                foodId = foodId, stepIndex = target, remainingMs = remaining,
                paused = !running, updatedAt = System.currentTimeMillis(),
            )
        )
    }

    val description = food?.description ?: ""
    val foodName = food?.name ?: ""

    // ── Read-only mise-en-place overview (#99) ─────────────────────
    // Cook mode shows prep state but can't toggle it — a mid-cook tap must never
    // uncheck something the user already verified.
    var showMise by remember { mutableStateOf(false) }
    val miseCheckedList by remember(foodId) {
        com.erfanbagheri.tahdig.data.local.TahdigDatabase
            .getInstance(appContext).milestoneCheckDao().observeHashes(foodId)
    }.collectAsState(initial = emptyList())
    val miseChecked = miseCheckedList.toSet()
    val miseRows = remember(food) {
        com.erfanbagheri.tahdig.util.MisePlace.rowsOf(food?.ingredients ?: "")
            .map { com.erfanbagheri.tahdig.util.MisePlace.rowsFor(listOf(it), 1.0).first() }
    }

    // ── Timer center (#95): overlay above the step view ──
    var showTimers by remember { mutableStateOf(false) }

    // ── Equipment (#100): count in the top bar, full list in an overlay ──
    var showTools by remember { mutableStateOf(false) }
    val tools = remember(food) {
        com.erfanbagheri.tahdig.util.EquipmentInferrer.forDish(
            food?.equipment ?: "", food?.description ?: "", food?.ingredients ?: "",
        )
    }
    // Session-scoped readiness, cleared when the cook session ends (screen leaves).
    var readyTools by remember(foodId) { mutableStateOf(emptySet<String>()) }

    val steps = remember(description) { splitSteps(description) }

    // ── Session navigation (#93) ───────────────────────────────────
    // durationOf/goTo own every step move (buttons, swipe, system-back) so the
    // timer reset and the persist happen in exactly one place.

    fun durationOf(index: Int) = steps.getOrNull(index)?.let { DurationParser.first(it) }

    fun persistSession() {
        scope.launch {
            sessionDao.save(
                CookSessionEntity(
                    foodId = foodId,
                    stepIndex = current,
                    remainingMs = remaining,
                    paused = !running,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    /** One navigation path for buttons, swipe and system-back: timer resets per step. */
    fun goTo(target: Int) {
        if (target == current || target !in steps.indices) return
        Haptics.tap(view)
        // Undo window (#98): every forward move arms a 5s restore of the step
        // we just left — goTo gives that step a full timer again (AC).
        if (target > current) {
            undoStep = current
            undoNonce++
        }
        current = target
        remaining = durationOf(target)?.seconds ?: 0L
        running = false
        fired = false
        persistSession()
    }

    val goBackInMode = {
        if (current > 0) goTo(current - 1) else showExitConfirm = true
    }

    /** Done state (#98): celebration overlay, session cleared so no resume offer. */
    fun enterDone() {
        if (showDone) return
        // Same haptic pair as the timer firing — this IS a "time's up" moment.
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        Haptics.confirm(view)
        showDone = true
        scope.launch { sessionDao.clear(foodId) }
    }

    /** Done-state «پختم» (#98): history row → heatmap/coverage, leftovers on Home. */
    fun cookIt() {
        val f = food ?: return
        Haptics.confirm(view)
        scope.launch {
            historyDao.insert(
                HistoryEntity(
                    foodId = f.id,
                    timestamp = System.currentTimeMillis(),
                    mealTime = MealTimeHelper.currentBucket(),
                )
            )
        }
        onCooked(f)
    }

    // ── Shake-to-advance (#96) ──────────────────────────────────────
    // `shakeOn`/`shakeSensitivity`/`shakeFlashTick` are declared up top (they
    // have no forward refs); this block sits after goTo/enterDone so it may
    // call them. The sensor gate + sub-60s guard live in the watcher callback.
    fun advanceFromHandsFree() {
        if (showDone) return
        Haptics.tap(view)
        shakeFlashTick++
        // Same path as the nav button — persist + undo arm come along (#96).
        if (current >= steps.lastIndex) enterDone() else goTo(current + 1)
    }

    val shakeWatcher = remember(appContext) {
        ShakeWatcher(appContext) {
            if (ShakeDetector.canAdvance(remaining, running)) {
                advanceFromHandsFree()
            }
        }
    }
    DisposableEffect(shakeWatcher, shakeOn, shakeSensitivity) {
        shakeWatcher.enabled = shakeOn && !showDone
        shakeWatcher.threshold = ShakeDetector.thresholdFor(shakeSensitivity)
        if (shakeOn && shakeWatcher.available) shakeWatcher.start()
        onDispose { shakeWatcher.stop() }
    }

    // ── Per-step timer ──────────────────────────────────────────────
    // A timer belongs to the step it was found in, so everything resets on step change.
    val stepDuration = remember(current, steps) {
        steps.getOrNull(current)?.let { DurationParser.first(it) }
    }
    // Per-step cues (#97): read from this step's own text every step change.
    val stepText = remember(current, steps) { steps.getOrElse(current) { "" } }
    val stepHeat = remember(stepText) { com.erfanbagheri.tahdig.util.HeatTagger.levelOf(stepText) }
    val stepCues = remember(stepText) { com.erfanbagheri.tahdig.util.DonenessCues.of(stepText) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // ── Keep-screen-on (#98) ────────────────────────────────────────
    // Replaces the old hold-the-screen-awake-for-the-whole-mode flag: awake only
    // while a LONG step's timer actually runs, or while the session override is on.
    val keepAwake = KeepAwake.shouldKeepAwake(stepDuration?.seconds, running, manualKeepAwake)
    DisposableEffect(view, keepAwake) {
        val window = view.context.findActivity()?.window
        if (keepAwake) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // System/back-button inside the mode: previous step, exit confirm on step 1,
    // plain exit once the done overlay owns the screen (#98).
    // Gated so a technique overlay sitting on top owns back while it is open.
    BackHandler(enabled = backEnabled) {
        if (showDone) onBack() else goBackInMode()
    }

    // ── Undo window (#98) ───────────────────────────────────────────
    // 5s Farsi snackbar after a forward move; the action restores the step we
    // left. Auto-dismiss is manual — Compose's duration enum has no 5s member.
    LaunchedEffect(undoNonce) {
        val restore = undoStep
        if (restore < 0) return@LaunchedEffect
        launch {
            val res = snackbarHostState.showSnackbar(
                message = "رفتی مرحلهٔ بعد",
                actionLabel = "بازگردانی مرحله",
                withDismissAction = false,
                duration = SnackbarDuration.Indefinite,
            )
            if (res == SnackbarResult.ActionPerformed) {
                undoStep = -1
                goTo(restore)
            }
        }
        delay(5000)
        snackbarHostState.currentSnackbarData?.dismiss()
    }

    // ── Voice control wiring (#94) ──────────────────────────────────
    // Streaming recognition needs the runtime mic grant (unlike the one-shot
    // VoiceInput dictation). Asked on toggle-on; a refusal leaves the toggle off.
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        micGranted = granted
        // Grant lands → turn the master on; a refusal leaves it off.
        if (granted) SettingsStore.setVoiceControl(true)
    }
    LaunchedEffect(Unit) {
        micGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            appContext, android.Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // One session for the screen's lifetime; released whenever the toggle is off
    // or the mode leaves, so no mic is held (AC).
    val voiceSession = remember(appContext) {
        CookVoiceSession(appContext) { transcript ->
            // Done overlay owns the screen (#98): no commands behind it.
            if (showDone) return@CookVoiceSession
            val cmd = CookVoiceCommands.match(transcript)
            voiceEcho = cmd?.let { CookVoiceCommands.echoOf(it) } ?: CookVoiceCommands.UNKNOWN_ECHO
            Haptics.tap(view)
            when (cmd) {
                CookVoiceCommands.Command.NEXT -> goTo(current + 1)
                // At step 1 «قبلی» only echoes — a hands-free confirm dialog
                // would need the wet hands we are trying to spare.
                CookVoiceCommands.Command.PREVIOUS -> if (current > 0) goTo(current - 1)
                CookVoiceCommands.Command.REPEAT -> repeatTick++
                CookVoiceCommands.Command.TIMER -> {
                    if (stepDuration != null) { running = !running; persistSession() }
                }
                CookVoiceCommands.Command.STOP -> { running = false; persistSession() }
                // Finished cooking → celebration (#98), which clears the session.
                CookVoiceCommands.Command.DONE -> enterDone()
                null -> Unit
            }
        }
    }

    DisposableEffect(voiceSession) {
        onDispose { voiceSession.release() }
    }

    LaunchedEffect(voiceMasterOn, micGranted) {
        if (voiceMasterOn && micGranted) voiceSession.start()
        else voiceSession.release()
    }

    // Read-aloud (#94): fa-IR TTS, gated on its own switch. Null-safe init — a
    // `remember` initializer cannot reference the variable it assigns.
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    DisposableEffect(appContext) {
        val engine = android.speech.tts.TextToSpeech(appContext) { status ->
            ttsReady = status == android.speech.tts.TextToSpeech.SUCCESS
        }
        engine.language = Locale("fa", "IR")
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }
    // Speak each new step once (and «تکرار» re-fires it), only while the switch is on.
    LaunchedEffect(stepText, voiceReadAloud, ttsReady, repeatTick) {
        if (!voiceReadAloud || !ttsReady || stepText.isBlank()) return@LaunchedEffect
        tts?.speak(stepText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "tahdig-step")
    }

    // Transient echo of the last recognized command — big, fades on its own.
    LaunchedEffect(voiceEcho) {
        if (voiceEcho != null) { delay(1400); voiceEcho = null }
    }

    LaunchedEffect(running, current) {
        if (!running) {
            // Pause / step-move disarms the mirror alarm (#95).
            TimerScheduler.cancel(appContext, TimerScheduler.STEP_ID)
            return@LaunchedEffect
        }
        // Arm the step mirror so the notification still fires if the process
        // dies mid-step (#95 — this issue owns the mechanism for step timers too).
        TimerScheduler.scheduleStep(appContext, System.currentTimeMillis() + remaining * 1000L)
        while (isActive && running) {
            delay(1000)
            if (remaining <= 1L) {
                remaining = 0L
                running = false
                fired = true
                // Completed in-app — cancel so the receiver cannot double-fire.
                TimerScheduler.cancel(appContext, TimerScheduler.STEP_ID)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Haptics.confirm(view)
            } else {
                remaining -= 1L
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = { goBackInMode() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                }
                Text(
                    text = foodName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = YekanBakh,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                if (tools.isNotEmpty()) {
                    Text(
                        text = com.erfanbagheri.tahdig.ui.components.equipmentCountLabel(
                            readyTools.size, tools.size,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showTools = !showTools }
                            .padding(8.dp),
                    )
                }
                // Timer center entry (#95): named countdowns for the other pots.
                IconButton(onClick = { showTimers = true }) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = "تایمرها",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                // Keep-awake override (#98): session-scoped switch in the top bar.
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "بیدار",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(
                    checked = manualKeepAwake,
                    onCheckedChange = { manualKeepAwake = it },
                )
            }

            // Persistent hairline strip (#95): every running timer stays visible.
            Spacer(Modifier.height(4.dp))
            TimerStrip(onOpen = { showTimers = true })

            // Tools overlay: same chips as the detail screen, session-scoped toggles.
            if (showTools && tools.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                com.erfanbagheri.tahdig.ui.components.EquipmentRow(
                    labels = tools,
                    readyLabels = readyTools,
                    onToggle = { label ->
                        readyTools = if (label in readyTools) readyTools - label
                        else readyTools + label
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Read-only mise-en-place overview (#99): shows prep state, never toggles it.
            if (miseRows.isNotEmpty()) {
                androidx.compose.material3.TextButton(onClick = { showMise = !showMise }) {
                    androidx.compose.material3.Text(
                        text = if (showMise) "بستن مواد لازم"
                        else "مواد لازم · " + com.erfanbagheri.tahdig.ui.components.miseCounter(
                            miseRows.count { it.hash in miseChecked }, miseRows.size,
                        ),
                        fontFamily = YekanBakh,
                    )
                }
                if (showMise) {
                    com.erfanbagheri.tahdig.ui.components.MiseChecklist(
                        rows = miseRows,
                        checkedHashes = miseChecked,
                        onToggle = null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { if (steps.isEmpty()) 0f else (current + 1f) / steps.size },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "مرحله ${PersianText.toPersianDigits(current + 1)} از " +
                    PersianText.toPersianDigits(steps.size),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = YekanBakh,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = steps.getOrElse(current) { "" },
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                fontFamily = YekanBakh,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    // A11y (#96): TalkBack «مرحله بعد/قبلی» reach the exact same
                    // path as the nav buttons; the step counter is a live region
                    // so advances are announced.
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        customActions = listOf(
                            CustomAccessibilityAction("مرحله بعد") {
                                advanceFromHandsFree(); true
                            },
                            CustomAccessibilityAction("مرحله قبلی") {
                                goBackInMode(); true
                            },
                        )
                    }
                    // Horizontal swipe turns pages (#93). Direction comes from
                    // CookSessionMath.stepForSwipe, so RTL advances on dx > 0.
                    .pointerInput(steps) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onHorizontalDrag = { _, dx -> total += dx },
                            onDragEnd = {
                                goTo(CookSessionMath.stepForSwipe(total, isRtl, current, steps.lastIndex))
                                total = 0f
                            },
                            onDragCancel = { total = 0f },
                        )
                    },
            )

            // Technique deep-links (#101): linked terms of THIS step as tappable chips.
            // Back from the technique returns to this exact step because `current`
            // lives in this screen's state, which stays on the back stack.
            val stepTechIds = remember(current, steps) {
                com.erfanbagheri.tahdig.util.TechniqueLinker
                    .linkify(steps.getOrElse(current) { "" }, com.erfanbagheri.tahdig.util.TechniqueRegistry.all())
                    .mapNotNull { it.techniqueId }
                    .distinct()
            }
            if (stepTechIds.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    stepTechIds.forEach { id ->
                        val tech = com.erfanbagheri.tahdig.util.TechniqueRegistry.byId(id)
                        if (tech != null) {
                            Text(
                                text = tech.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { onTechnique(tech.id) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            // Inline timer — only when this step actually states a duration
            if (stepDuration != null) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (fired) "زمان تمام شد!"
                                else "زمان این مرحله: ${stepDuration.raw}",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = YekanBakh,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            // Heat glyph beside the timer (#97) — flat text, no icons.
                            if (stepHeat != null) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = com.erfanbagheri.tahdig.util.HeatTagger
                                        .glyph(stepHeat) + " " + stepHeat.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = YekanBakh,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = DurationParser.mmss(remaining),
                            style = MaterialTheme.typography.displaySmall,
                            fontFamily = YekanBakh,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    Haptics.tap(view)
                                    running = !running
                                    // Pause/resume is a state change → persist (#93).
                                    persistSession()
                                },
                                enabled = remaining > 0L,
                            ) {
                                Text(if (running) "توقف" else "شروع", fontFamily = YekanBakh)
                            }
                            TextButton(
                                onClick = {
                                    remaining = stepDuration.seconds
                                    running = false
                                    fired = false
                                    persistSession()
                                },
                            ) {
                                Text("از نو", fontFamily = YekanBakh)
                            }
                        }
                    }
                }
            }

            // ── Doneness cues (#97) ────────────────────────────────────────
            // Persistently in step view AND under the countdown: same slot, so the
            // final-minute appearance can never cause a layout jump — only the
            // color changes when ≤60s remain.
            if (stepCues.isNotEmpty()) {
                val cuesHot = stepDuration != null && (running || fired) && remaining <= 60L
                Column(modifier = Modifier.fillMaxWidth()) {
                    stepCues.forEach { cue ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        ) {
                            Text(
                                text = "□",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = YekanBakh,
                                color = if (cuesHot) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = cue,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = YekanBakh,
                                color = if (cuesHot) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    // Last step finishes (#98) instead of a dead disabled button.
                    onClick = {
                        if (current >= steps.lastIndex) enterDone() else goTo(current + 1)
                    },
                ) {
                    Icon(
                        if (current >= steps.lastIndex) Icons.Default.Check
                        else Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (current >= steps.lastIndex) "پایان پخت" else "مرحله بعد",
                        fontFamily = YekanBakh,
                    )
                }
                Button(
                    onClick = { goTo(current - 1) },
                    enabled = current > 0,
                ) {
                    Text("مرحله قبل", fontFamily = YekanBakh)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Exit confirm (#93): only reachable on step 1 — mid-recipe back is
            // just "previous step", so no confirm there.
            if (showExitConfirm) {
                AlertDialog(
                    onDismissRequest = { showExitConfirm = false },
                    title = { Text("خروج از حالت پخت؟", fontFamily = YekanBakh) },
                    confirmButton = {
                        TextButton(onClick = {
                            showExitConfirm = false
                            // Normal exit clears the session; force-stop can't
                            // run this, so «ادامه بده» survives exactly the
                            // interrupted case (#93).
                            scope.launch { sessionDao.clear(foodId) }
                            onBack()
                        }) {
                            Text("خروج", fontFamily = YekanBakh)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitConfirm = false }) {
                            Text("ادامه", fontFamily = YekanBakh)
                        }
                    },
                )
            }

            // Voice control row + command echo (#94): sits INSIDE the Surface so
            // the echo overlays the screen without touching the page layout.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "کنترل صوتی",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                androidx.compose.material3.Switch(
                    checked = voiceMasterOn,
                    onCheckedChange = { on ->
                        if (on && !micGranted) {
                            micLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        } else {
                            SettingsStore.setVoiceControl(on)
                        }
                    },
                )
            }

            voiceEcho?.let { echo ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        text = echo,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = YekanBakh,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }
        }

            // Timer center (#95): covers the mode while open, back to the step after.
            if (showTimers) {
                TimerCenterOverlay(onClose = { showTimers = false })
            }

            // Done state (#98): covers the whole mode until a route is chosen.
            if (showDone) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "نوش جان!",
                            style = MaterialTheme.typography.headlineLarge,
                            fontFamily = YekanBakh,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = foodName,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = YekanBakh,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(32.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { Haptics.tap(view); onRate() }) {
                                Text("امتیاز بده", fontFamily = YekanBakh)
                            }
                            Button(onClick = { cookIt() }) {
                                Text("پختم", fontFamily = YekanBakh)
                            }
                        }
                        TextButton(onClick = {
                            Haptics.tap(view)
                            food?.let { ShareCard.share(uiContext, it) }
                        }) {
                            Text("اشتراک‌گذاری", fontFamily = YekanBakh)
                        }
                    }
                }
            }

            // Shake ack flash (#96): centered chip, no layout shift, brief.
            if (shakeFlashVisible) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                ) {
                    Text(
                        text = "مرحله بعد",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = YekanBakh,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }

            // Undo snackbar (#98): bottom of the Box, overlays whatever is behind.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
    }
    }
}

/** Unwrap a ContextWrapper chain to reach the hosting Activity (null in previews). */
private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
