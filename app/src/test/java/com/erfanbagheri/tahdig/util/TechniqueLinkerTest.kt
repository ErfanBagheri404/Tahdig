package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Technique linkification (#101) — the acceptance criterion is boundary safety:
 * link the term, never the word that merely contains it.
 */
class TechniqueLinkerTest {

    private fun linkify(text: String): List<TechniqueLinker.Segment> =
        TechniqueLinker.linkify(text, TechniqueRegistry.all())

    private fun linkedIds(text: String): List<String> =
        linkify(text).mapNotNull { it.techniqueId }

    @Test
    fun `linked term in a step gets a technique id`() {
        val segs = linkify("شیرینی را در فر بگذارید")
        assertEquals(listOf("oven"), linkedIds("شیرینی را در فر بگذارید"))
        assertTrue(segs.any { it.text == "در فر" && it.techniqueId == "oven" })
    }

    @Test
    fun `text outside the term stays plain`() {
        val segs = linkify("برنج را دم کشیدن، سپس سرو کنید")
        assertEquals(listOf(null, "damkeshi", null), segs.map { it.techniqueId })
        assertEquals("برنج را ", segs.first().text)
    }

    @Test
    fun `no link inside a longer word`() {
        // «در فر» IS a substring of «در فرش» — the boundary must reject it.
        assertEquals(emptyList<String>(), linkedIds("سینی را در فرش بکشید"))
        assertFalse(TechniqueLinker.hasKeyword("سینی در فرش", TechniqueRegistry.byId("oven")!!))
    }

    @Test
    fun `plain sentence links nothing`() {
        assertEquals(emptyList<String>(), linkedIds("خورشت را هم بزنید و بچشید"))
    }

    @Test
    fun `two techniques in one step both link`() {
        val ids = linkedIds("زعفران را دم کنید و برنج را آبکش کنید")
        assertTrue("saffron" in ids)
        assertTrue("abkesh" in ids)
    }

    @Test
    fun `repeated term links every occurrence`() {
        val segs = linkify("در فر بگذارید، دوباره در فر برگردانید")
        assertEquals(2, segs.count { it.techniqueId == "oven" })
    }

    @Test
    fun `empty text yields one plain segment`() {
        assertEquals(listOf(TechniqueLinker.Segment("", null)), linkify(""))
    }

    @Test
    fun `reverse link matches keyword-tagged dishes only`() {
        val tahdig = TechniqueRegistry.byId("tahdig")!!
        assertTrue(TechniqueRegistry.matches(tahdig, "قورمه‌سبزی با ته‌دیگ برنج"))
        // Suffix-attached «ته‌دیگی» is NOT a whole-word match — strict boundary.
        assertFalse(TechniqueRegistry.matches(tahdig, "این ته‌دیگیِ خوبی شد"))
        assertFalse(TechniqueRegistry.matches(tahdig, "فقط هم زدن"))
    }

    @Test
    fun `registry loaded from the real asset`() {
        assertTrue("library has 20+ entries", TechniqueRegistry.all().size >= 20)
        assertNull(TechniqueRegistry.byId("does-not-exist"))
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun installRegistry() {
            TechniqueRegistry.installFromJson(
                java.io.File("src/main/assets/seed/techniques.json").readText(),
            )
        }
    }
}
