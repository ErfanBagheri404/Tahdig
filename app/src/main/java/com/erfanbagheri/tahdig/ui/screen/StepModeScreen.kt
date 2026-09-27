package com.erfanbagheri.tahdig.ui.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.DurationParser
import com.erfanbagheri.tahdig.util.Haptics
import com.erfanbagheri.tahdig.util.PersianText
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Guided cooking: one step at a time, with an inline timer when the step states a
 * duration, and the screen held awake so it does not sleep mid-recipe.
 */
@Composable
fun StepModeScreen(
    foodId: Long,
    onBack: () -> Unit,
    /** Opens a linked technique page (#101); back returns to this step. */
    onTechnique: (String) -> Unit = {},
) {
    val appContext = LocalContext.current.applicationContext
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    var food by remember {
        mutableStateOf<com.erfanbagheri.tahdig.data.local.entity.FoodEntity?>(null)
    }
    LaunchedEffect(foodId) {
        food = com.erfanbagheri.tahdig.data.local.TahdigDatabase
            .getInstance(appContext).foodDao().getById(foodId)
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
            .map { com.erfanbagheri.tahdig.util.MisePlace.rowsFor(listOf(it), 1).first() }
    }

    // ── Equipment (#100): count in the top bar, full list in an overlay ──
    var showTools by remember { mutableStateOf(false) }
    val tools = remember(food) {
        com.erfanbagheri.tahdig.util.EquipmentInferrer.forDish(
            food?.equipment ?: "", food?.description ?: "", food?.ingredients ?: "",
        )
    }
    // Session-scoped readiness, cleared when the cook session ends (screen leaves).
    var readyTools by remember(foodId) { mutableStateOf(emptySet<String>()) }

    val steps = remember(description) {
        description.split(Regex("[.!؟\\n]+")).map { it.trim() }.filter { it.isNotBlank() }
            .ifEmpty { listOf(description) }
    }
    var current by remember { mutableIntStateOf(0) }

    // Hold the display on while cooking — the user's hands are busy, and the
    // screen sleeping between steps is the most annoying thing here.
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
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
    var remaining by remember(current) { mutableLongStateOf(stepDuration?.seconds ?: 0L) }
    var running by remember(current) { mutableStateOf(false) }
    var fired by remember(current) { mutableStateOf(false) }

    LaunchedEffect(current, stepDuration) {
        remaining = stepDuration?.seconds ?: 0L
        running = false
        fired = false
    }

    LaunchedEffect(running, current) {
        if (!running) return@LaunchedEffect
        while (isActive && running) {
            delay(1000)
            if (remaining <= 1L) {
                remaining = 0L
                running = false
                fired = true
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
                IconButton(onClick = onBack) {
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
            }

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
                modifier = Modifier.weight(1f),
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
                    onClick = {
                        Haptics.tap(view)
                        if (current < steps.lastIndex) current++
                    },
                    enabled = current < steps.lastIndex,
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("مرحله بعد", fontFamily = YekanBakh)
                }
                Button(
                    onClick = {
                        Haptics.tap(view)
                        if (current > 0) current--
                    },
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
