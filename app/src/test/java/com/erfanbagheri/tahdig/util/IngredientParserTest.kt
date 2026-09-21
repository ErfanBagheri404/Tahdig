package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IngredientParserTest {

    @Test
    fun `parses quantity unit item`() {
        val p = IngredientParser.parse("۲ پیمانه آرد")
        assertEquals(2.0, p.quantity!!, 0.001)
        assertEquals("پیمانه", p.unit)
        assertEquals("آرد", p.item)
    }

    @Test
    fun `parses ascii digits`() {
        val p = IngredientParser.parse("3 عدد تخم مرغ")
        assertEquals(3.0, p.quantity!!, 0.001)
        assertEquals("عدد", p.unit)
        assertEquals("تخم مرغ", p.item)
    }

    @Test
    fun `bare item has no quantity`() {
        val p = IngredientParser.parse("پیاز")
        assertNull(p.quantity)
        assertNull(p.unit)
        assertEquals("پیاز", p.item)
    }

    @Test
    fun `longest unit wins`() {
        val p = IngredientParser.parse("۱ قاشق غذاخوری روغن")
        assertEquals("قاشق غذاخوری", p.unit)
        assertEquals("روغن", p.item)
    }

    @Test
    fun `quantity without unit keeps the whole remainder as item`() {
        val p = IngredientParser.parse("۲ لیمو عمانی")
        assertEquals(2.0, p.quantity!!, 0.001)
        assertNull(p.unit)
        assertEquals("لیمو عمانی", p.item)
    }

    @Test
    fun `lone number is not turned into an empty item`() {
        val p = IngredientParser.parse("۲")
        assertNull(p.quantity)
        assertEquals("۲", p.item)
    }

    @Test
    fun `merges duplicate quantities`() {
        val merged = IngredientParser.merge(listOf("۲ عدد پیاز", "۱ عدد پیاز"))
        assertEquals(1, merged.size)
        assertEquals(3.0, merged[0].quantity!!, 0.001)
        assertEquals("۳ عدد پیاز", merged[0].display())
    }

    @Test
    fun `does not merge across different units`() {
        val merged = IngredientParser.merge(listOf("۲ پیمانه آرد", "۱۰۰ گرم آرد"))
        assertEquals(2, merged.size)
    }

    @Test
    fun `merges bare duplicates without inventing a quantity`() {
        val merged = IngredientParser.merge(listOf("پیاز", "پیاز"))
        assertEquals(1, merged.size)
        assertNull(merged[0].quantity)
        assertEquals("پیاز", merged[0].display())
    }

    @Test
    fun `mixed quantity and bare do not sum`() {
        val merged = IngredientParser.merge(listOf("۲ عدد پیاز", "پیاز"))
        assertEquals(2, merged.size)
    }

    @Test
    fun `ZWNJ variants merge`() {
        val merged = IngredientParser.merge(listOf("۱ پیمانه سیب‌زمینی", "۲ پیمانه سیب زمینی"))
        assertEquals(1, merged.size)
        assertEquals(3.0, merged[0].quantity!!, 0.001)
    }

    @Test
    fun `groups into known buckets`() {
        assertEquals("سبزیجات", IngredientParser.categoryOf("پیاز"))
        assertEquals("پروتئین", IngredientParser.categoryOf("گوشت گوسفند"))
        assertEquals("حبوبات", IngredientParser.categoryOf("لوبیا قرمز"))
        assertEquals("ادویه", IngredientParser.categoryOf("زردچوبه"))
    }

    @Test
    fun `unknown item falls into the catch-all bucket`() {
        assertEquals("سایر", IngredientParser.categoryOf("چیز عجیب"))
    }

    @Test
    fun `group puts catch-all last`() {
        val entries = IngredientParser.merge(listOf("پیاز", "چیز عجیب", "برنج"))
        val groups = IngredientParser.group(entries)
        assertEquals("سایر", groups.last().first)
    }

    @Test
    fun `display round-trips through parse`() {
        // The list stores display text, and merging re-parses it — so a rendered row
        // must parse back to the same unit+item identity, or rows stop merging.
        val first = IngredientParser.parse("۲ عدد پیاز")
        val again = IngredientParser.parse(first.display())
        assertEquals(
            IngredientParser.mergeKey(first.unit, first.item),
            IngredientParser.mergeKey(again.unit, again.item),
        )
        assertEquals(2.0, again.quantity!!, 0.001)
    }

    @Test
    fun `display keeps original spelling of the item`() {
        // normalize() maps آ→ا for matching; the list must not show that mangling.
        assertEquals("۲ پیمانه آرد", IngredientParser.parse("۲ پیمانه آرد").display())
    }
}
