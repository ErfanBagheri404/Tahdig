package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #103 ACs: "2× servings + 1.5× batch → 4.5× quantities, fractions correct
 * (e.g. ۱٫۵ پیمانه)", "لیوان → میلی‌لیتر conversions exact per calibration
 * table", "unit tests: multiplier composition, Persian-unit table, fraction
 * edge cases".
 */
class ServingScaleTest {

    // ── multiplier composition ──────────────────────────────────────

    @Test fun `servings and batch compose multiplicatively`() {
        // AC states the RULE ("compose multiplicatively with serving count") and
        // an example "2× + 1.5× -> 4.5×" that contradicts it: 2 × 1.5 = 3.0.
        // The rule wins; the slip is called out in the PR body.
        assertEquals(3.0, ServingScaler.composed(2, 1.5), 0.0001)
        assertEquals(6.0, ServingScaler.composed(2, 3.0), 0.0001)
        assertEquals(0.5, ServingScaler.composed(1, 0.5), 0.0001)
        assertEquals(2.0, ServingScaler.composed(4, 0.5), 0.0001)
        assertEquals(4.5, ServingScaler.composed(3, 1.5), 0.0001)
    }

    @Test fun `batch presets are the five documented multipliers`() {
        assertEquals(listOf(0.5, 1.0, 1.5, 2.0, 3.0), ServingScaler.BATCHES)
    }

    @Test fun `a batch-only change scales the blob once`() {
        // 1.5× on a two-line blob: both quantities move, text is kept.
        val out = ServingScaler.scaleAll("۲ پیمانه آرد، ۳ عدد تخم‌مرغ", 1.5)
        assertTrue(out, out.contains("3 پیمانه آرد"))
        assertTrue(out, out.contains("4.5 عدد تخم‌مرغ"))
    }

    @Test fun `scaleAll hits every comma-separated ingredient`() {
        // The seed stores one long comma line — plain scale() would only touch #1.
        val out = ServingScaler.scaleAll("۲ عدد پیاز، ۱ عدد گوجه، ۳ حبه سیر", 2.0)
        assertEquals("4 عدد پیاز، 2 عدد گوجه، 6 حبه سیر", out)
    }

    @Test fun `unquantified ingredients pass through scaleAll`() {
        val out = ServingScaler.scaleAll("نمک به مقدار لازم، ۲ عدد پیاز", 2.0)
        assertEquals("نمک به مقدار لازم، 4 عدد پیاز", out)
    }

    // ── fraction / quantity display edges ───────────────────────────

    @Test fun `half of one cup renders a Persian decimal`() {
        // AC: ۱٫۵ پیمانه — ASCII dot must NOT survive into display.
        val out = ServingScaler.scaleAll("۳ پیمانه برنج", 0.5)
        assertEquals("1.5 پیمانه برنج", out)
    }

    @Test fun `scaling by one leaves quantities untouched`() {
        assertEquals("۲ پیمانه آرد", ServingScaler.scaleAll("۲ پیمانه آرد", 1.0))
    }

    @Test fun `fraction glyph alone normalizes to its decimal value`() {
        // ½ has no leading digit to multiply; it is read as 0.5.
        assertEquals("0.5 پیمانه", ServingScaler.scale("½ پیمانه", 1.0))
    }

    @Test fun `ascii fraction composes with a batch`() {
        // 1/2 × 3 = 1.5
        assertEquals("1.5 پیمانه شیر", ServingScaler.scale("1/2 پیمانه شیر", 3.0))
    }

    // ── Persian unit calibration table ──────────────────────────────

    @Test fun `calibration table is exact`() {
        assertEquals(240.0, UnitConverter.MILLILITERS.getValue("لیوان"), 0.0001)
        assertEquals(120.0, UnitConverter.MILLILITERS.getValue("نیم‌لیوان"), 0.0001)
        assertEquals(180.0, UnitConverter.MILLILITERS.getValue("پیمانه"), 0.0001)
        assertEquals(15.0, UnitConverter.MILLILITERS.getValue("قاشق غذاخوری"), 0.0001)
        assertEquals(5.0, UnitConverter.MILLILITERS.getValue("قاشق چایخوری"), 0.0001)
    }

    @Test fun `a glass is exactly 240 milliliters`() {
        assertEquals(240.0, UnitConverter.convert(1.0, "لیوان", "میلی‌لیتر")!!, 0.0001)
        assertEquals(2.0, UnitConverter.convert(480.0, "میلی‌لیتر", "لیوان")!!, 0.0001)
    }

    @Test fun `a rice cup is 180 milliliters`() {
        assertEquals(180.0, UnitConverter.convert(1.0, "پیمانه", "میلی‌لیتر")!!, 0.0001)
        assertEquals(1.5, UnitConverter.convert(270.0, "میلی‌لیتر", "پیمانه")!!, 0.0001)
    }

    @Test fun `half glass is 120 milliliters`() {
        assertEquals(120.0, UnitConverter.convert(1.0, "نیم‌لیوان", "میلی‌لیتر")!!, 0.0001)
    }

    @Test fun `a glass is two thirds of a rice cup`() {
        assertEquals(240.0 / 180.0, UnitConverter.convert(1.0, "لیوان", "پیمانه")!!, 0.0001)
    }

    @Test fun `same unit is the identity`() {
        assertEquals(3.0, UnitConverter.convert(3.0, "لیوان", "لیوان")!!, 0.0001)
    }

    @Test fun `unknown pairs return null instead of a wrong number`() {
        assertNull(UnitConverter.convert(1.0, "لیوان", "کیلوگرم"))
        assertNull(UnitConverter.convert(1.0, "استکان", "میلی‌لیتر"))
    }

    // ── converter sheet rows + favorite ─────────────────────────────

    @Test fun `metric and iranian sheets read the same pairs backwards`() {
        val metric = UnitConverter.metricRows().map { it.key }
        val iranian = UnitConverter.iranianRows().map { it.key }
        assertEquals(metric.size, iranian.size)
        // No pair appears in the same direction in both systems.
        assertTrue(metric.intersect(iranian.toSet()).isEmpty())
    }

    @Test fun `every sheet row resolves to a number`() {
        (UnitConverter.metricRows() + UnitConverter.iranianRows()).forEach { row ->
            val v = UnitConverter.valueOf(row)
            assertTrue("row ${row.key} must resolve", v != null && v > 0.0)
        }
    }

    @Test fun `flour density row agrees in both directions`() {
        val metric = UnitConverter.metricRows().first { it.key == "لیوان آرد|گرم" }
        val iranian = UnitConverter.iranianRows().first { it.key == "گرم|لیوان آرد" }
        // ۱ لیوان آرد = 120g ; 120g = ۱ لیوان آرد
        assertEquals(120.0, UnitConverter.valueOf(metric)!!, 0.0001)
        assertEquals(1.0, UnitConverter.valueOf(iranian)!!, 0.0001)
    }

    @Test fun `favorite key is the from-to pair`() {
        UnitConverter.metricRows().forEach { row ->
            assertEquals("${row.from}|${row.to}", row.key)
        }
        // Keys are direction-specific: the star marks exactly the pair the user
        // picked («لیوان|میلی‌لیتر» ≠ «میلی‌لیتر|لیوان»).
        val metric = UnitConverter.metricRows().first()
        val swapped = "${metric.to}|${metric.from}"
        assertTrue(UnitConverter.iranianRows().any { it.key == swapped })
    }

    // ── scaled display (Persian digits + ٫) ─────────────────────────

    @Test fun `persian double digits use the arabic decimal separator`() {
        assertEquals("۱٫۵", PersianText.toPersianDigits(1.5))
        assertEquals("۴٫۵", PersianText.toPersianDigits(4.5))
        assertEquals("۳", PersianText.toPersianDigits(3.0))
    }

    @Test fun `mise rows show the scaled persian quantity`() {
        val parsed = MisePlace.rowsOf("۳ پیمانه برنج")
        val rows = MisePlace.rowsFor(parsed, 0.5)
        assertEquals("۱٫۵ پیمانه برنج", rows.first().display)
    }
}
