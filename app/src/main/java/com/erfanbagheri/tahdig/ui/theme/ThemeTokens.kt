package com.erfanbagheri.tahdig.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Pure theme math for #128 — no Compose runtime, so it is unit-testable.
 *
 * The issue's acceptance is «high-contrast meets 7:1, computed in test over
 * theme tokens», which is only possible if the numbers live here rather than
 * inside a @Composable. The screen layer reads these values; it never does the
 * arithmetic itself.
 */
object ThemeTokens {

    /** Matches the SettingsStore ints: 0 system, 1 light, 2 dark, 3 dynamic. */
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
    const val DYNAMIC = 3

    /** WCAG AAA — the bar for body text sitting on a surface. */
    const val TARGET_CONTRAST = 7.0

    /**
     * WCAG AA — the bar for a label sitting on the user's own accent swatch.
     *
     * 7:1 is genuinely unreachable there: on the saffron swatch the best of
     * white/black is 5.9:1, because a mid-tone color has no room for either
     * extreme. Forcing it would mean darkening the swatch the user picked, so
     * the standard is applied where it can actually be met and the ceiling is
     * reported honestly instead of silently missed.
     */
    const val MIN_CONTRAST = 4.5

    /** Hairline weight in dp — doubles under high contrast (1px -> 2px). */
    fun hairlineDp(highContrast: Boolean): Int = if (highContrast) 2 else 1

    /** Whether a mode follows the system wallpaper rather than the fixed palettes. */
    fun isDynamic(mode: Int): Boolean = mode == DYNAMIC

    /**
     * WCAG 2.1 relative luminance.
     *
     * The 0.03928 knee and the 2.4 exponent are from the spec, not tuning —
     * the linear segment near black is what keeps dark-theme ratios honest.
     */
    fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val v = value.toDouble()
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    /** WCAG contrast ratio between two colors: 1.0 (identical) .. 21.0. */
    fun contrastRatio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /**
     * Push [foreground] toward white or black until it clears [target] against
     * [background]; returned unchanged when it already clears.
     *
     * This is the entire high-contrast mechanism. Hand-picking a second palette
     * would drift the moment anyone edits the first, so the token layer derives
     * one instead. Direction comes from the background's luminance, so dark
     * themes lighten text and light themes darken it.
     *
     * When even pure white/black cannot reach the target (a mid-grey surface),
     * the extreme is returned: the best available beats a label that vanishes.
     */
    fun ensureContrast(
        foreground: Color,
        background: Color,
        target: Double = TARGET_CONTRAST,
    ): Color {
        if (contrastRatio(foreground, background) >= target) return foreground
        val towards = if (luminance(background) < 0.5) Color.White else Color.Black
        // Contrast is monotonic in the blend factor, so a fixed bisection is
        // enough; a fixed count also cannot fail to terminate on a rounding edge.
        var lo = 0f
        var hi = 1f
        repeat(14) {
            val mid = (lo + hi) / 2f
            if (contrastRatio(lerp(foreground, towards, mid), background) >= target) hi = mid
            else lo = mid
        }
        return lerp(foreground, towards, hi)
    }

    /**
     * The bar to actually apply on [background]: 7:1 where the color has
     * headroom, otherwise the best of black/white, never below [MIN_CONTRAST].
     *
     * Used for labels on the brand colors. On near-black or near-white
     * surfaces 7:1 is the answer; on a mid-tone brand color it is not, and
     * aiming at an unreachable number would read as a failed check forever.
     */
    fun achievableTarget(background: Color): Double =
        maxOf(MIN_CONTRAST, minOf(TARGET_CONTRAST, bestAchievable(background)))

    /**
     * The best ratio achievable on [background] with pure black or white.
     *
     * Lets a caller (and its test) distinguish «not lifted enough» from «cannot
     * be lifted further» on a mid-tone background.
     */
    fun bestAchievable(background: Color): Double =
        maxOf(contrastRatio(Color.Black, background), contrastRatio(Color.White, background))

    /**
     * The readable member of [candidates] on [background] — highest ratio wins.
     * Used for text that sits on a user-chosen accent, where the correct
     * foreground depends on the swatch and cannot be known in advance.
     */
    // A list, not a vararg: Color is an @Immutable inline value class, which
    // Kotlin refuses to box behind a spread parameter.
    fun readableOn(background: Color, candidates: List<Color>): Color =
        candidates.maxByOrNull { contrastRatio(it, background) } ?: background

    /**
     * A flat accent swatch. Two variants because one color cannot be legible on
     * both a near-black and a near-white surface — the same reason Material
     * ships separate light and dark schemes.
     */
    data class Accent(val name: String, val light: Color, val dark: Color) {
        fun forDarkTheme(darkTheme: Boolean): Color = if (darkTheme) dark else light
    }

    /**
     * Eight flat swatches. Deliberately no gradient and no rainbow: the house
     * style is flat squares, and a picker that out-shouts the food is the wrong
     * trade for a recipe app.
     */
    val ACCENTS: List<Accent> = listOf(
        Accent("زعفرانی", Color(0xFF8B5A00), Color(0xFFFFB74D)),
        Accent("زرشکی", Color(0xFF9B1B30), Color(0xFFFF8095)),
        Accent("نارنجی", Color(0xFFB35400), Color(0xFFFFB77C)),
        Accent("زیتونی", Color(0xFF556B27), Color(0xFFB6CC80)),
        Accent("فیروزه‌ای", Color(0xFF00695C), Color(0xFF6FDCCB)),
        Accent("نیلی", Color(0xFF2E4A8B), Color(0xFFAEC6FF)),
        Accent("بادمجانی", Color(0xFF6A3D8F), Color(0xFFD5B3F5)),
        Accent("خاکی", Color(0xFF6D4C41), Color(0xFFD7B8AC)),
    )

    /** Swatch count, pinned so the picker and its test agree. */
    val ACCENT_COUNT: Int get() = ACCENTS.size

    /**
     * Parse a `#RRGGBB` or `RRGGBB` entry. Returns null for anything else so the
     * caller can keep the previous accent and say so, rather than silently
     * falling back to a color the user did not choose.
     */
    fun parseHex(input: String): Color? {
        val hex = input.trim().removePrefix("#")
        if (hex.length != 6) return null
        if (!hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
        return try {
            Color(0xFF000000.toInt() or hex.toLong(16).toInt())
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** Canonical form for storing and re-displaying a parsed color. */
    fun toHex(color: Color): String {
        fun part(v: Float): String {
            val i = Math.round(v * 255f).coerceIn(0, 255)
            return i.toString(16).padStart(2, '0')
        }
        return "#${part(color.red)}${part(color.green)}${part(color.blue)}".uppercase()
    }

    /** Default when no accent is chosen: the app's own saffron. */
    val DEFAULT_ACCENT: Accent get() = ACCENTS[0]
}
