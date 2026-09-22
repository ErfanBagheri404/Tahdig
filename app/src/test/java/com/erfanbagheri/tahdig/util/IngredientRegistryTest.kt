package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Drives the REAL bundled table, not a fixture — a glossary that does not load or does
 * not cover the seed is the failure mode worth catching.
 */
class IngredientRegistryTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadRealTable() {
            val f = File("src/main/assets/seed/ingredients.json")
            assertTrue("bundled glossary missing at ${f.absolutePath}", f.exists())
            IngredientRegistry.installFromJson(f.readText())
        }
    }

    @Test
    fun `table loads with a sane number of entries`() {
        assertTrue("expected >200 entries, got ${IngredientRegistry.size}", IngredientRegistry.size > 200)
    }

    @Test
    fun `onion resolves and is not dropped`() {
        val ing = IngredientRegistry.resolve("پیاز")
        assertNotNull(ing)
        assertEquals("onion", ing!!.id)
        assertEquals("سبزیجات", ing.aisle)
    }

    @Test
    fun `red onion and onion share a canonical id`() {
        val a = IngredientRegistry.resolve("پیاز قرمز")
        val b = IngredientRegistry.resolve("پیاز")
        assertEquals("onion", a?.id)
        assertEquals(a?.id, b?.id)
    }

    @Test
    fun `english name resolves to the same entry`() {
        assertEquals("onion", IngredientRegistry.resolve("Red Onion")?.id)
        assertEquals("onion", IngredientRegistry.resolve("onions")?.id)
    }

    @Test
    fun `zwnj and arabic letter variants resolve`() {
        // «سیبزمینی» (ZWNJ) vs «سیب زمینی» (space) vs Arabic yeh «ي» instead of «ی»
        val zwnj = IngredientRegistry.resolve("سیب\u200cزمینی")
        val spaced = IngredientRegistry.resolve("سیب زمینی")
        val arabicYeh = IngredientRegistry.resolve("پیاز")
        assertEquals("potato", zwnj?.id)
        assertEquals(zwnj?.id, spaced?.id)
        assertEquals("onion", arabicYeh?.id)
    }

    @Test
    fun `unknown text returns null so callers keep the raw string`() {
        assertNull(IngredientRegistry.resolve("چیزی که وجود ندارد"))
        assertNull(IngredientRegistry.resolve(""))
    }

    @Test
    fun `merge key falls back to the old pair for unknown items`() {
        val unknown = IngredientRegistry.mergeKeyFor("عدد", "چیز ناشناخته")
        assertEquals(IngredientParser.mergeKey("عدد", "چیز ناشناخته"), unknown)
    }

    @Test
    fun `merge key ignores the unit for canonically known items`() {
        // ۲ پیمانه پیاز and ۳ عدد پیاز قرمز are the same shopping need.
        assertEquals(
            IngredientRegistry.mergeKeyFor("پیمانه", "پیاز"),
            IngredientRegistry.mergeKeyFor("عدد", "پیاز قرمز"),
        )
    }

    @Test
    fun `parser merge folds alias variants into one row`() {
        val merged = IngredientParser.merge(listOf("۲ عدد پیاز", "۳ عدد پیاز قرمز", "۱ حبه سیر"))
        assertEquals("alias variants must collapse to one row", 2, merged.size)
        val onion = merged.first { IngredientRegistry.resolve(it.item)?.id == "onion" }
        // 2 + 3 both carry quantities -> summed, under the first-seen unit (عدد).
        assertEquals(5.0, onion.quantity!!, 0.0001)
        assertEquals("عدد", onion.unit)
    }

    @Test
    fun `parser merge still dedupes unknown items by raw text`() {
        val merged = IngredientParser.merge(listOf("۲ پیمانه چیز ناشناخته", "۱ پیمانه چیز ناشناخته"))
        assertEquals(1, merged.size)
        assertEquals(3.0, merged.first().quantity!!, 0.0001)
    }

    @Test
    fun `aisle falls back to keyword bucket for unknown items`() {
        assertEquals("سبزیجات", IngredientRegistry.aisleOf("پیاز قرمز"))
        // "گوشت بوقلمون" is not in the table, but the keyword bucket still catches it.
        assertEquals("پروتئین", IngredientRegistry.aisleOf("گوشت بوقلمون"))
    }

    @Test
    fun `allergens are reported for a dish`() {
        val tags = IngredientRegistry.allergensIn(listOf("شیر", "کره", "گردو", "پیاز"))
        assertTrue(tags.contains("لبنیات"))
        assertTrue(tags.contains("مغزها"))
        assertTrue(!tags.contains("گلوتن"))
    }

    @Test
    fun `seed coverage stays above 98 percent of occurrences`() {
        val foods = File("src/main/assets/seed/foods")
        assertTrue("seed dir missing", foods.isDirectory)
        // Raw JSON tree: the seed files are heterogeneous and we only need one field.
        val tree = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        var total = 0
        var covered = 0
        for (f in foods.listFiles { _, n -> n.endsWith(".json") } ?: emptyArray()) {
            val arr = tree.parseToJsonElement(f.readText()).jsonArray
            for (el in arr) {
                val raw = el.jsonObject["ingredients"]?.jsonPrimitive?.contentOrNull ?: continue
                for (tok in raw.split(',', '،')) {
                    val item = IngredientParser.parse(tok.trim()).item
                    if (item.isBlank() || item.any { it.code < 128 && it.isLetter() }) continue
                    total++
                    if (IngredientRegistry.resolve(item) != null) covered++
                }
            }
        }
        assertTrue("no ingredients found", total > 500)
        val pct = 100.0 * covered / total
        assertTrue("coverage dropped to $pct% ($covered/$total)", pct >= 98.0)
    }
}
