package com.erfanbagheri.tahdig.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Grow an element's HIT BOX to [min] without changing how it looks (#129).
 *
 * The house style keeps controls tight — a 28dp dismiss button, a 16dp close
 * glyph — and that is a deliberate visual choice, not an oversight. It is
 * still a target a fingertip and a switch user have to hit, so the tap area
 * needs to be 48dp on its own.
 *
 * `layout` is used rather than `size` on purpose: `size` would force the
 * element to actually BE 48dp, which re-spaces every row this touches and
 * changes the design. This measures the laid-out size and reports a larger
 * one to the parent, so padding and gaps are untouched and only the clickable
 * bounds grow. Corners stay put, so a small control inside a large row never
 * swallows the row's own tap.
 *
 * ponytail: no semantics side effects. Compose already reports the touch
 * bounds to AccessibilityNodeInfo, so Accessibility Scanner sees 48dp without
 * any extra property. Upgrade to `minimumInteractiveComponentSize()` only if
 * a screen turns out to need the bounds to grow the LAYOUT too.
 */
fun Modifier.minTouchTarget(min: Dp = 48.dp): Modifier = composed {
    val density = LocalDensity.current
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val minPx = with(density) { min.roundToPx() }
        val w = maxOf(placeable.width, minPx)
        val h = maxOf(placeable.height, minPx)
        // Report the padded size to the parent so the hit box is really 48dp…
        layout(w, h) {
            // …but place the child centred, so the visual does not move.
            placeable.place((w - placeable.width) / 2, (h - placeable.height) / 2)
        }
    }
}

/** True when this dp value already meets the minimum target on its own. */
internal fun Dp.meetsTouchTarget(min: Dp = 48.dp): Boolean = this >= min
