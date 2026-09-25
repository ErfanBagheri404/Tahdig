package com.erfanbagheri.tahdig.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Local pull-to-refresh (#127).
 *
 * Offline-first, so this never implies a network fetch: a pull re-shuffles
 * the local feed and the indicator says so. Written here rather than using
 * `material3`'s `PullToRefreshBox`, which is experimental at the pinned BOM
 * (2024.09.03) — opt-in annotating the whole Home screen is a larger diff than
 * the gesture deserves.
 *
 * The [scrollState] is the same one the scrollable content uses: the pull
 * only accumulates when the list is already at the top
 * (![ScrollState.canScrollBackward]), so mid-list drags scroll normally.
 *
 * ponytail: linear resistance clamp, no overscroll, no nested-scroll chaining.
 * Flavour the curve when a designer asks.
 */
@Composable
fun LocalPullToRefresh(
    scrollState: ScrollState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    var pull by remember { mutableFloatStateOf(0f) }
    val haptics = LocalHapticFeedback.current
    val offset by animateFloatAsState(
        targetValue = pull,
        animationSpec = tween(120),
        label = "pullOffset",
    )
    val armed = pull > TRIGGER * 0.6f

    val connection = remember(scrollState, enabled) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabled) return Offset.Zero
                // Downward drag at the very top: steal it into the pull.
                if (available.y > 0 && !scrollState.canScrollBackward) {
                    pull = (pull + available.y * RESISTANCE).coerceIn(0f, MAX_PULL)
                    return available
                }
                // Dragging back up releases an armed pull and returns the slack
                // to the content so the list does not fight the gesture.
                if (available.y < 0 && pull > 0f) {
                    val taken = (available.y * RESISTANCE).coerceAtLeast(-pull)
                    pull += taken
                    return Offset(0f, taken / RESISTANCE)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Deliberately a no-op: clearing `pull` here zeroes it before
                // onPostFling reads it, so the threshold could never be met and
                // refresh silently never fired. onPostFling owns the settle.
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Nothing is left in the list, and we hold a pull: the finger
                // lifted. This is the reliable settle point.
                if (pull > TRIGGER) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRefresh()
                }
                pull = 0f
                return Velocity.Zero
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().nestedScroll(connection)) {
        if (abs(offset) > 2f) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, (offset - 56f).roundToInt().coerceAtLeast(-44)) }
                    .size(if (armed) 28.dp else 20.dp),
                strokeWidth = 2.dp,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .offset { IntOffset(0, offset.roundToInt()) },
        ) { content() }
    }
}

private const val TRIGGER = 72f
private const val MAX_PULL = 150f
private const val RESISTANCE = 0.45f
