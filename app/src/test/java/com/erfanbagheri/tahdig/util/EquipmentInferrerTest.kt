package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Equipment keyword inference (#100): the acceptance fixtures plus the
 * false-positive cases the boundary rule exists for.
 */
class EquipmentInferrerTest {

    @Test
    fun `oven keyword in a step`() {
        assertTrue("فر" in EquipmentInferrer.infer("شیرینی را در فر با دمای ۱۸۰ بپزید"))
    }

    @Test
    fun `pan keyword in a step`() {
        assertTrue("تابه" in EquipmentInferrer.infer("تابه را روی شعله ملایم داغ کنید"))
    }

    @Test
    fun `pot and colander`() {
        val got = EquipmentInferrer.infer("قابلمه تفلون، سپس در آبکش بریزید")
        assertTrue("قابلمه" in got)
        assertTrue("آبکش" in got)
    }

    @Test
    fun `no match inside a longer word`() {
        // فر inside فرش / فرایند must NOT infer an oven.
        assertFalse("فر" in EquipmentInferrer.infer("روی فرش آشپزخانه را بپوشانید"))
        assertFalse("فر" in EquipmentInferrer.infer("فرایند پخت را ادامه دهید"))
    }

    @Test
    fun `zwnj and spaced variants both match`() {
        // normalize strips ZWNJ: «مخلوط‌کن» in text matches the same keyword.
        assertTrue("مخلوط‌کن" in EquipmentInferrer.infer("با مخلوط‌کن بزنید"))
    }

    @Test
    fun `results are deduped across repeated keywords`() {
        val got = EquipmentInferrer.infer("تابه، تابه داغ، باز هم تابه")
        assertEquals(1, got.count { it == "تابه" })
    }

    @Test
    fun `empty or tool-free text infers nothing`() {
        assertEquals(emptyList<String>(), EquipmentInferrer.infer(""))
        assertEquals(emptyList<String>(), EquipmentInferrer.infer("خورشت را هم بزنید و بچشید"))
    }

    @Test
    fun `seed first then fallback inference`() {
        // Explicit seed list wins, even when keywords would add more.
        assertEquals(
            listOf("قابلمه"),
            EquipmentInferrer.forDish("قابلمه", "در فر بگذارید"),
        )
        // Blank seed falls back to inference.
        assertEquals(
            listOf("فر"),
            EquipmentInferrer.forDish("", "در فر بگذارید"),
        )
    }

    @Test
    fun `every label the inferrer emits is a known label`() {
        // Guards the seed bake: whatever keywords fire, the label must be table-known.
        val got = EquipmentInferrer.infer("شیرینی در فر، تابه و آبکش و چرخ‌گوشت و رنده و سینی و گریل")
        assertTrue(got.isNotEmpty())
        assertEquals(emptyList<String>(), got.filterNot { it in EquipmentInferrer.knownLabels })
    }

    @Test
    fun `every baked seed label exists in the table`() {
        // scripts/bake_equipment.py mirrors the table; a drift shows up here, not on a
        // device where an unknown label would render as a raw untranslated slug.
        val raw = java.io.File("src/main/assets/seed/equipment.json").readText()
        val table = kotlinx.serialization.json.Json.decodeFromString<Map<String, List<String>>>(raw)
        assertTrue("seed file non-empty", table.isNotEmpty())
        val unknown = table.values.flatten().distinct().filterNot { it in EquipmentInferrer.knownLabels }
        assertEquals(emptyList<String>(), unknown)
    }
}
