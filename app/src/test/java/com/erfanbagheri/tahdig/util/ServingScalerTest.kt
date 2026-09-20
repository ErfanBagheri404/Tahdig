package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ServingScalerTest {

    @Test
    fun `scales ASCII digit`() {
        assertEquals("4 قاشق برنج", ServingScaler.scale("2 قاشق برنج", 2.0))
    }

    @Test
    fun `scales Persian digit`() {
        assertEquals("4 قاشق برنج", ServingScaler.scale("۲ قاشق برنج", 2.0))
    }

    @Test
    fun `scales decimal`() {
        assertEquals("5 قاشق برنج", ServingScaler.scale("2.5 قاشق برنج", 2.0))
    }

    @Test
    fun `scales fraction glyph`() {
        assertEquals("1 فنجان شیر", ServingScaler.scale("½ فنجان شیر", 2.0))
    }

    @Test
    fun `scales ASCII fraction`() {
        assertEquals("1 فنجان شیر", ServingScaler.scale("1/2 فنجان شیر", 2.0))
    }

    @Test
    fun `non numeric line unchanged`() {
        assertEquals("نمک به مقدار لازم", ServingScaler.scale("نمک به مقدار لازم", 2.0))
    }

    @Test
    fun `no space after token gets normalized to one space`() {
        assertEquals("4 قاشق", ServingScaler.scale("2قاشق", 2.0))
    }

    @Test
    fun `rounds to one decimal`() {
        assertEquals("2.4 قاشق برنج", ServingScaler.scale("2.4 قاشق برنج", 1.0))
    }
}
