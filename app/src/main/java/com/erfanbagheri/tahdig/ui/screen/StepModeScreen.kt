package com.erfanbagheri.tahdig.ui.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
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
