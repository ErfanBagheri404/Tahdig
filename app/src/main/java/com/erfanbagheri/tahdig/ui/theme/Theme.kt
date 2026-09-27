package com.erfanbagheri.tahdig.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.erfanbagheri.tahdig.data.prefs.SettingsStore

private val DarkColors = darkColorScheme(
    primary       = Color(0xFFFFB74D),
    onPrimary     = Color(0xFF211A00),
    primaryContainer = Color(0xFF3E2B00),
    onPrimaryContainer = Color(0xFFFFDEA1),
    secondary     = Color(0xFFEF6C00),
    onSecondary   = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF4A2400),
    onSecondaryContainer = Color(0xFFFFDCC2),
    tertiary      = Color(0xFF5C6B2F),
    onTertiary    = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF809546),
    onTertiaryContainer = Color(0xFF0E1B00),
    background    = Color(0xFF1A1200),
    onBackground  = Color(0xFFEDE0CA),
    surface       = Color(0xFF1A1200),
    onSurface     = Color(0xFFEDE0CA),
    surfaceVariant = Color(0xFF4E4534),
    onSurfaceVariant = Color(0xFFD1C5B0),
    outline       = Color(0xFF9A8E79),
)

private val LightColors = lightColorScheme(
    primary       = Color(0xFF8B5A00),
    onPrimary     = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDEA1),
    onPrimaryContainer = Color(0xFF2C1600),
    secondary     = Color(0xFFBF360C),
    onSecondary   = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCC2),
    onSecondaryContainer = Color(0xFF3B0E00),
    tertiary      = Color(0xFF556B27),
    onTertiary    = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD8E9B0),
    onTertiaryContainer = Color(0xFF132000),
    background    = Color(0xFFFFFBFF),
    onBackground  = Color(0xFF1E1B16),
    surface       = Color(0xFFFFFBFF),
    onSurface     = Color(0xFF1E1B16),
    surfaceVariant = Color(0xFFEDE0CA),
    onSurfaceVariant = Color(0xFF4C4639),
    outline       = Color(0xFF7E7667),
)

/**
 * Hairline weight in dp. Components read this instead of hard-coding `1.dp`,
 * which is what makes the high-contrast switch (#128) reach them all at once —
 * a second value per site would drift the first time anyone edits one.
 */
val LocalHairline = staticCompositionLocalOf { 1.dp }

/**
 * The chosen accent, or null for the app default. Components that paint their
 * own category color (FoodVisuals) override it when this is set.
 */
val LocalAccent = staticCompositionLocalOf<Color?> { null }

@Composable
fun TahdigTheme(content: @Composable () -> Unit) {
    val themeMode by SettingsStore.themeMode.collectAsState()
    val accentHex by SettingsStore.accentHex.collectAsState()
    val highContrast by SettingsStore.highContrast.collectAsState()
    val systemDark = isSystemInDarkTheme()

    val darkTheme = when (themeMode) {
        ThemeTokens.LIGHT -> false
        ThemeTokens.DARK -> true
        else -> systemDark // system, or dynamic (darkness still follows the user)
    }

    val context = LocalContext.current
    val colorScheme = when {
        // #128: dynamic is a *mode*, not the default — below API 31 it falls
        // back to the fixed palettes rather than pretending the wallpaper was
        // read, and the settings row says so.
        ThemeTokens.isDynamic(themeMode) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else      -> LightColors
    }

    val accent = ThemeTokens.parseHex(accentHex)
    val finalScheme = buildScheme(colorScheme, accent, highContrast)

    CompositionLocalProvider(
        LocalHairline provides ThemeTokens.hairlineDp(highContrast).dp,
        LocalAccent provides accent,
    ) {
        MaterialTheme(
            colorScheme = finalScheme,
            typography = TahdigTypography,
            content = content,
        )
    }
}

/**
 * Accent + high-contrast overlay, kept out of the composable so the issue's
 * acceptance («7:1 computed in test over theme tokens») has something to call.
 *
 * Accent recolors the primary family — switches, chips and pills all read
 * primary/primaryContainer, so they follow the swatch. High contrast then lifts
 * the text tokens above [ThemeTokens.TARGET_CONTRAST] against the surface they
 * sit on. Order matters: lifting first and recoloring after would undo it.
 */
fun buildScheme(
    base: ColorScheme,
    accent: Color?,
    highContrast: Boolean,
): ColorScheme {
    // primaryContainer is a 16% wash of the accent, so onPrimaryContainer is
    // lifted against that wash and not against the surface — reading it against
    // the surface would compute a ratio for a pairing that never appears.
    val container = accent?.copy(alpha = 0.16f) ?: base.primaryContainer
    val withAccent = if (accent == null) base else base.copy(
        primary = accent,
        onPrimary = ThemeTokens.readableOn(accent, listOf(Color.White, Color.Black)),
        primaryContainer = container,
        onPrimaryContainer = ThemeTokens.ensureContrast(accent, container),
    )
    if (!highContrast) return withAccent

    // Two different bars, deliberately:
    //  · text on surfaces aims at 7:1 — the surfaces are near-black/near-white,
    //    so the number is reachable and the issue's acceptance holds there;
    //  · labels on primary/secondary/tertiary aim at what the color actually
    //    admits (see ThemeTokens.achievableTarget) — no hand-picked brand color
    //    can deliver 7:1 to both a light and a dark label at once.
    return withAccent.copy(
        onBackground = ThemeTokens.ensureContrast(withAccent.onBackground, withAccent.background),
        onSurface = ThemeTokens.ensureContrast(withAccent.onSurface, withAccent.surface),
        onSurfaceVariant = ThemeTokens.ensureContrast(withAccent.onSurfaceVariant, withAccent.surfaceVariant),
        onPrimary = ThemeTokens.ensureContrast(
            withAccent.onPrimary, withAccent.primary,
            ThemeTokens.achievableTarget(withAccent.primary),
        ),
        onSecondary = ThemeTokens.ensureContrast(
            withAccent.onSecondary, withAccent.secondary,
            ThemeTokens.achievableTarget(withAccent.secondary),
        ),
        onTertiary = ThemeTokens.ensureContrast(
            withAccent.onTertiary, withAccent.tertiary,
            ThemeTokens.achievableTarget(withAccent.tertiary),
        ),
        outline = ThemeTokens.ensureContrast(withAccent.outline, withAccent.surface),
        outlineVariant = ThemeTokens.ensureContrast(withAccent.outlineVariant, withAccent.surfaceVariant),
    )
}
