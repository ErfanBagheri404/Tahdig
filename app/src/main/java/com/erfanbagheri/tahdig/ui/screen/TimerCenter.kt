package com.erfanbagheri.tahdig.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.prefs.TimerStore
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.erfanbagheri.tahdig.util.DurationParser
import com.erfanbagheri.tahdig.util.PersianText
import kotlinx.coroutines.delay
import com.erfanbagheri.tahdig.ui.components.oneA11yStop

/** Wall-clock tick shared by the center and the strip — one clock, no drift between them. */
@Composable
fun rememberNow(intervalMs: Long = 1000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(intervalMs)
        }
    }
    return now
}

/**
 * The persistent hairline strip (#95) — timers stay visible while cook mode is
 * open. One row of hairline-separated slots, no cards.
 */
@Composable
fun TimerStrip(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val timers by TimerStore.timers.collectAsState()
    if (timers.isEmpty()) return
    val now = rememberNow()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                // #129: one tap target that spoke N separate timer rows; joined
                // so a swipe reads "شام: ۱۲:۰۰، صبحانه: ۱۵:۰۰" in one stop.
                .oneA11yStop(
                    timers.joinToString("، ") { "${it.name}: ${it.display(now)}" }
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            timers.forEach { t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = t.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = t.display(now),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = YekanBakh,
                        fontWeight = FontWeight.Bold,
                        color = if (t.fired) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
                // Hairline separator between slots, never a card border.
                Box(
                    Modifier
                        .size(width = 1.dp, height = 12.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    }
}

/**
 * Timer center overlay (#95): named countdowns, each with pause/resume/reset.
 * Max [TimerStore.MAX] concurrent — the add row disables with a Farsi hint.
 */
@Composable
fun TimerCenterOverlay(onClose: () -> Unit, defaultName: String = "تایمر") {
    val context = LocalContext.current
    val timers by TimerStore.timers.collectAsState()
    val now = rememberNow()
    // AC: the name defaults to the step context, editable from the first keystroke.
    var draftName by remember { mutableStateOf(defaultName) }
    var draftTime by remember { mutableStateOf("") }
    val atLimit = timers.size >= TimerStore.MAX

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تایمرها",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = YekanBakh,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "چند تا کار هم‌زمان رو با هم بپز — تا ${PersianText.toPersianDigits(TimerStore.MAX.toString())} تایمر",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onClose) {
                    Text("بستن", fontFamily = YekanBakh)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Add: name + free-form duration («۱۰ دقیقه» / «45s» / «1:30») ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("نام (ته‌دیگ)", fontFamily = YekanBakh) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = timerFieldColors(),
                )
                OutlinedTextField(
                    value = draftTime,
                    onValueChange = { draftTime = it },
                    modifier = Modifier.weight(0.8f),
                    placeholder = { Text("۱۰ دقیقه", fontFamily = YekanBakh) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = timerFieldColors(),
                )
                // «۱۰ دقیقه» parses as text; a bare «45» means seconds — the hint
                // field must never accept input the parser would silently drop.
                val secs = durationSeconds(draftTime)
                val canAdd = !atLimit && secs != null
                IconButton(
                    onClick = {
                        if (secs != null && TimerStore.add(context, draftName, secs * 1000L)) {
                            draftName = defaultName
                            draftTime = ""
                        }
                    },
                    // AC: UI disables add beyond the max.
                    enabled = canAdd,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "تایمر جدید",
                        tint = if (canAdd) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (atLimit) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "بیشتر از ${PersianText.toPersianDigits(TimerStore.MAX.toString())} تایمر هم‌زمان نمی‌شه — یکی رو پاک کن",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = YekanBakh,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            if (timers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "هنوز تایمری نداری",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "مثلاً «ته‌دیگ» و «۱۰ دقیقه» رو بنویس و + رو بزن",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = YekanBakh,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    timers.forEach { t ->
                        TimerRow(
                            timer = t,
                            now = now,
                            onToggle = {
                                if (t.running) TimerStore.pause(context, t.id)
                                else TimerStore.resume(context, t.id)
                            },
                            onReset = { TimerStore.reset(context, t.id) },
                            onRemove = { TimerStore.remove(context, t.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerRow(
    timer: com.erfanbagheri.tahdig.util.TimerState,
    now: Long,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = YekanBakh,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = when {
                        timer.fired -> "زمان تمام شد"
                        timer.running -> "در حال شمارش"
                        else -> "متوقف"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = YekanBakh,
                    color = if (timer.fired) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = timer.display(now),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = YekanBakh,
                fontWeight = FontWeight.Bold,
                color = if (timer.fired) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onToggle) {
                Text(if (timer.running) "توقف" else "ادامه", fontFamily = YekanBakh)
            }
            TextButton(onClick = onReset) {
                Text("از نو", fontFamily = YekanBakh)
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف تایمر",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

/** «۱۰ دقیقه» via DurationParser; a bare «45» / «۴۵» is read as seconds. */
private fun durationSeconds(raw: String): Long? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    DurationParser.first(text)?.let { return it.seconds }
    return PersianText.toAsciiDigits(text).toLongOrNull()?.takeIf { it > 0 }
}

@Composable
private fun timerFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
)
