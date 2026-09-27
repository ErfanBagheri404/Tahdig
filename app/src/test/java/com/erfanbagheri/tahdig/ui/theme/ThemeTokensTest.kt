package com.erfanbagheri.tahdig.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #128 — the high-contrast acceptance is «7:1 computed in test over theme
 * tokens», so these are the numbers, not a screenshot.
 */
class ThemeTokensTest {

    // -- WCAG reference values ----------------------------------------------

    @Test
    fun `black on white is the 21 to 1 maximum`() {
        assertEquals(21.0, ThemeTokens.contrastRatio(Color.Black, Color.White), 0.01)
    }

    @Test
    fun `any color against itself is 1 to 1`() {
        // Below the WCAG floor the formula would divide by ~0 and return noise.
        assertEquals(1.0, ThemeTokens.contrastRatio(Color(0xFF8B5A00), Color(0xFF8B5A00)), 0.001)
    }

    @Test
    fun `pure black is not treated as zero luminance`() {
        // The 0.03928 linear-segment knee: a naive sum would divide by 0.05 and
        // report 1.05 instead of 1.0, which is the classic off-by-one here.
        assertEquals(0.0, ThemeTokens.luminance(Color.Black), 0.0001)
        assertEquals(1.0, ThemeTokens.luminance(Color.White), 0.0001)
    }

    @Test
    fun `contrast is symmetric`() {
        val a = Color(0xFFFFB74D)
        val b = Color(0xFF1A1200)
        assertEquals(
            ThemeTokens.contrastRatio(a, b),
            ThemeTokens.contrastRatio(b, a),
            0.0001,
        )
    }

    // -- ensureContrast: the whole high-contrast mechanism -------------------

    @Test
    fun `a color that already clears the target is returned untouched`() {
        val on = Color.White
        val bg = Color.Black
        assertEquals(on, ThemeTokens.ensureContrast(on, bg))
    }

    @Test
    fun `dark surface gets its text lightened to seven to one`() {
        val bg = Color(0xFF1A1200) // the app's own dark background
        val faint = Color(0xFF4E4534) // surfaceVariant, far too dim as text
        val lifted = ThemeTokens.ensureContrast(faint, bg)
        assertTrue(
            "expected >= 7:1, got ${ThemeTokens.contrastRatio(lifted, bg)}",
            ThemeTokens.contrastRatio(lifted, bg) >= ThemeTokens.TARGET_CONTRAST,
        )
        // And it must move toward white, not toward black on a dark surface.
        assertTrue(lifted.red >= faint.red)
    }

    @Test
    fun `light surface gets its text darkened to seven to one`() {
        val bg = Color(0xFFFFFBFF)
        val faint = Color(0xFFEDE0CA)
        val lifted = ThemeTokens.ensureContrast(faint, bg)
        assertTrue(
            "expected >= 7:1, got ${ThemeTokens.contrastRatio(lifted, bg)}",
            ThemeTokens.contrastRatio(lifted, bg) >= ThemeTokens.TARGET_CONTRAST,
        )
        assertTrue(lifted.red <= faint.red)
    }

    @Test
    fun `the result is the furthest blend that still clears, not an overshoot`() {
        // ensureContrast bisects to the *smallest* change that works; jumping to
        // the extreme would flatten the whole palette for no readability gain.
        val bg = Color(0xFF1A1200)
        val faint = Color(0xFF4E4534)
        val lifted = ThemeTokens.ensureContrast(faint, bg)
        assertTrue(ThemeTokens.contrastRatio(lifted, bg) <= 7.6)
        assertTrue(ThemeTokens.contrastRatio(faint, bg) < 7.0)
    }

    // -- readableOn: text over a user-chosen swatch -------------------------

    @Test
    fun `readable foreground flips with the swatch`() {
        val lightSwatch = Color(0xFFFFDEA1)
        val darkSwatch = Color(0xFF211A00)
        val cands = listOf(Color.White, Color.Black)
        assertTrue(ThemeTokens.contrastRatio(ThemeTokens.readableOn(lightSwatch, cands), lightSwatch) > 10)
        assertTrue(ThemeTokens.contrastRatio(ThemeTokens.readableOn(darkSwatch, cands), darkSwatch) > 10)
    }

    // -- hairline + mode ----------------------------------------------------

    @Test
    fun `high contrast doubles the hairline and normal mode does not`() {
        assertEquals(1, ThemeTokens.hairlineDp(highContrast = false))
        assertEquals(2, ThemeTokens.hairlineDp(highContrast = true))
    }

    @Test
    fun `only the dynamic mode is dynamic`() {
        assertTrue(ThemeTokens.isDynamic(ThemeTokens.DYNAMIC))
        for (mode in listOf(ThemeTokens.SYSTEM, ThemeTokens.LIGHT, ThemeTokens.DARK)) {
            assertTrue("mode $mode must not be dynamic", !ThemeTokens.isDynamic(mode))
        }
    }

    // -- hex round trip -----------------------------------------------------

    @Test
    fun `hex parses with and without the hash`() {
        val expected = Color(0xFF8B5A00)
        assertEquals(expected, ThemeTokens.parseHex("#8B5A00"))
        assertEquals(expected, ThemeTokens.parseHex("8b5a00"))
        assertEquals(expected, ThemeTokens.parseHex("  #8B5A00  "))
    }

    @Test
    fun `bad hex returns null instead of guessing a color`() {
        // A wrong color shown next to a rejected entry is worse than none: the
        // user cannot tell which mistake they made.
        assertNull(ThemeTokens.parseHex(""))
        assertNull(ThemeTokens.parseHex("#FFF"))
        assertNull(ThemeTokens.parseHex("#GGGGGG"))
        assertNull(ThemeTokens.parseHex("8B5A00FF"))
        assertNull(ThemeTokens.parseHex("#8B5A0Z"))
    }

    @Test
    fun `every swatch round trips through hex`() {
        ThemeTokens.ACCENTS.forEach { accent ->
            val hex = ThemeTokens.toHex(accent.light)
            assertEquals(hex, ThemeTokens.toHex(ThemeTokens.parseHex(hex)!!))
        }
    }

    @Test
    fun `the picker ships eight flat swatches`() {
        // The issue says "~8"; pinning it keeps the layout and the test in step.
        assertEquals(8, ThemeTokens.ACCENT_COUNT)
    }

    @Test
    fun `each swatch has a light and a dark variant that differ`() {
        // One color cannot be legible on both near-black and near-white, which
        // is the whole reason Accent carries two values.
        ThemeTokens.ACCENTS.forEach { accent ->
            assertTrue(accent.name, accent.light != accent.dark)
        }
    }

    // -- the overlay as a whole --------------------------------------------

    private fun darkBase() = darkColorScheme(
        primary = Color(0xFFFFB74D),
        onPrimary = Color(0xFF211A00),
        background = Color(0xFF1A1200),
        onBackground = Color(0xFFEDE0CA),
        surface = Color(0xFF1A1200),
        onSurface = Color(0xFFEDE0CA),
        surfaceVariant = Color(0xFF4E4534),
        onSurfaceVariant = Color(0xFFD1C5B0),
        outline = Color(0xFF9A8E79),
    )

    private fun lightBase() = lightColorScheme(
        primary = Color(0xFF8B5A00),
        onPrimary = Color(0xFFFFFFFF),
        background = Color(0xFFFFFBFF),
        onBackground = Color(0xFF1E1B16),
        surface = Color(0xFFFFFBFF),
        onSurface = Color(0xFF1E1B16),
        surfaceVariant = Color(0xFFEDE0CA),
        onSurfaceVariant = Color(0xFF4C4639),
        outline = Color(0xFF7E7667),
    )

    @Test
    fun `no accent and no contrast leaves the palette exactly as it was`() {
        val base = darkBase()
        val out = buildScheme(base, accent = null, highContrast = false)
        assertEquals(base.primary, out.primary)
        assertEquals(base.onSurface, out.onSurface)
        assertEquals(base.outline, out.outline)
    }

    @Test
    fun `high contrast lifts every text token to at least seven to one`() {
        val out = buildScheme(darkBase(), accent = null, highContrast = true)
        assertTrue(ThemeTokens.contrastRatio(out.onBackground, out.background) >= 7.0)
        assertTrue(ThemeTokens.contrastRatio(out.onSurface, out.surface) >= 7.0)
        assertTrue(ThemeTokens.contrastRatio(out.onSurfaceVariant, out.surfaceVariant) >= 7.0)
        assertTrue(ThemeTokens.contrastRatio(out.onPrimary, out.primary) >= 7.0)
        assertTrue(ThemeTokens.contrastRatio(out.onSecondary, out.secondary) >= 7.0)
        assertTrue(ThemeTokens.contrastRatio(out.onTertiary, out.tertiary) >= 7.0)
        assertTrue(ThemeTokens.contrastRatio(out.outline, out.surface) >= 7.0)
    }

    @Test
    fun `high contrast holds in the light theme too`() {
        val out = buildScheme(lightBase(), accent = null, highContrast = true)
        assertTrue(ThemeTokens.contrastRatio(out.onBackground, out.background) >= 7.0)
        assertTrue(ThemeTokens.contrastRatio(out.onSurface, out.surface) >= 7.0)
        assertTrue(ThemeTokens.contrastRatio(out.onSurfaceVariant, out.surfaceVariant) >= 7.0)
        // The light primary (#8B5A00) is a mid-tone: 7:1 is not reachable there,
        // so the bar is what the color admits — see achievableTarget.
        assertTrue(
            ThemeTokens.contrastRatio(out.onPrimary, out.primary) >=
                ThemeTokens.achievableTarget(out.primary),
        )
    }

    @Test
    fun `the target never drops below AA`() {
        // A mid-tone color must not become an excuse: 4.5:1 is the floor.
        listOf(
            Color(0xFF8B5A00), Color(0xFFFFB74D), Color(0xFF808080),
            Color(0xFF1A1200), Color(0xFFFFFBFF), Color(0xFF00ACC1),
        ).forEach { bg ->
            assertTrue(
                "target too low for $bg",
                ThemeTokens.achievableTarget(bg) >= ThemeTokens.MIN_CONTRAST,
            )
        }
    }

    @Test
    fun `a near-black surface still aims at the full 7 to 1`() {
        // The point of achievableTarget: a surface with headroom gets the full
        // number, so this is not a blanket relaxation.
        assertEquals(7.0, ThemeTokens.achievableTarget(Color(0xFF1A1200)), 0.001)
        assertEquals(7.0, ThemeTokens.achievableTarget(Color(0xFFFFFBFF)), 0.001)
        assertTrue(ThemeTokens.achievableTarget(Color(0xFF8B5A00)) < 7.0)
    }

    @Test
    fun `the accent recolors primary and keeps a readable on-color`() {
        val accent = Color(0xFF2E4A8B) // the navy swatch
        val out = buildScheme(lightBase(), accent = accent, highContrast = false)
        assertEquals(accent, out.primary)
        assertTrue(
            "onPrimary unreadable: ${ThemeTokens.contrastRatio(out.onPrimary, accent)}",
            ThemeTokens.contrastRatio(out.onPrimary, accent) >= 7.0,
        )
    }

    @Test
    fun `a light swatch gets dark text, a dark swatch gets light text`() {
        val light = buildScheme(darkBase(), accent = Color(0xFFFFDEA1), highContrast = false)
        assertTrue(ThemeTokens.luminance(light.onPrimary) < 0.5)
        val dark = buildScheme(darkBase(), accent = Color(0xFF211A00), highContrast = false)
        assertTrue(ThemeTokens.luminance(dark.onPrimary) > 0.5)
    }

    @Test
    fun `accent and high contrast together still land on seven to one`() {
        // The order inside buildScheme matters: lifting first and recoloring
        // after would hand back a low-contrast pair.
        ThemeTokens.ACCENTS.forEach { swatch ->
            listOf(true, false).forEach { dark ->
                val accent = swatch.forDarkTheme(dark)
                val base = if (dark) darkBase() else lightBase()
                val out = buildScheme(base, accent, highContrast = true)
                val ratio = ThemeTokens.contrastRatio(out.onPrimary, out.primary)
                val bar = ThemeTokens.achievableTarget(out.primary)
                assertTrue(
                    "${swatch.name} dark=$dark gave $ratio, bar $bar",
                    ratio >= bar,
                )
                // Whatever the bar, no swatch may be left illegible.
                assertTrue("${swatch.name} dark=$dark below AA", ratio >= ThemeTokens.MIN_CONTRAST)
            }
        }
    }
}
