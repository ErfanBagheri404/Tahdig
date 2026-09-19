package com.erfanbagheri.tahdig.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

// Warm food-inspired palette — dark
private val DarkColors = darkColorScheme(
    primary       = Color(0xFFFFB74D),  // warm amber
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

// Warm food-inspired palette — light
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

@Composable
fun TahdigTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Force RTL globally — this app is Farsi-only.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        val colorScheme = when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColors
            else      -> LightColors
        }

        MaterialTheme(
            colorScheme = colorScheme,
            typography = TahdigTypography,
            content = content,
        )
    }
}
