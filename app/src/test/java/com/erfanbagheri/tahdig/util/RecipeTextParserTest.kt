package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeTextParserTest {

    @Test
    fun `numbered steps split from unnumbered ingredients`() {
        val draft = RecipeTextParser.parse(
            """
            کیک شکلاتی
            آرد
            شکر
            تخم مرغ
            1. آرد و شکر را مخلوط کنید
            2. تخم مرغ را اضافه کنید
            3. در فر بگذارید
            """.trimIndent(),
        )

        assertEquals(listOf("آرد", "شکر", "تخم مرغ"), draft.ingredients)
        assertEquals(
            listOf("آرد و شکر را مخلوط کنید", "تخم مرغ را اضافه کنید", "در فر بگذارید"),
            draft.steps,
        )
        assertEquals("کیک شکلاتی", draft.title)
    }

    @Test
    fun `persian digits count as numbered steps`() {
        val draft = RecipeTextParser.parse(
            """
            خورش قورمه
            لوبیا
            سبزی
            ۱. لوبیا را بپزید
            ۲. سبزی را اضافه کنید
            """.trimIndent(),
        )

        assertEquals(listOf("لوبیا", "سبزی"), draft.ingredients)
        assertEquals(listOf("لوبیا را بپزید", "سبزی را اضافه کنید"), draft.steps)
    }

    @Test
    fun `bullet characters are treated as steps`() {
        val draft = RecipeTextParser.parse(
            """
            سالاد
            کاهو
            گوجه
            - کاهو را بشوید
            • گوجه را خرد کنید
            """.trimIndent(),
        )

        assertEquals(listOf("کاهو", "گوجه"), draft.ingredients)
        assertEquals(listOf("کاهو را بشوید", "گوجه را خرد کنید"), draft.steps)
    }

    @Test
    fun `headings split the blob into their own sections`() {
        val draft = RecipeTextParser.parse(
            """
            کوکو سبزی
            مواد لازم:
            تخم مرغ: ۳ عدد
            سیب‌زمینی: ۲ عدد
            طرز تهیه:
            ۱. سبزی‌ها را خرد کنید
            ۲. مخلوط کنید و سرخ کنید
            """.trimIndent(),
        )

        assertEquals("کوکو سبزی", draft.title)
        assertEquals(listOf("تخم مرغ: ۳ عدد", "سیب‌زمینی: ۲ عدد"), draft.ingredients)
        assertEquals(listOf("سبزی‌ها را خرد کنید", "مخلوط کنید و سرخ کنید"), draft.steps)
    }

    @Test
    fun `unstructured blob is best effort and lands in ingredients`() {
        val draft = RecipeTextParser.parse("آرد یک لیوان\nشکر نصف لیوان\nروی هم بریزید و در فر بگذارید")

        assertEquals(
            listOf("آرد یک لیوان", "شکر نصف لیوان", "روی هم بریزید و در فر بگذارید"),
            draft.ingredients,
        )
        assertTrue(draft.steps.isEmpty())
    }

    @Test
    fun `parseAny sends a bare url to the url branch`() {
        val draft = RecipeTextParser.parseAny("https://example.invalid/some-fancy-dish")

        assertEquals("https://example.invalid/some-fancy-dish", draft.sourceUrl)
        assertEquals("some fancy dish", draft.title)
        assertTrue(draft.ingredients.isEmpty())
        assertTrue(draft.steps.isEmpty())
    }

    @Test
    fun `parseAny sends recipe text to the text branch`() {
        val draft = RecipeTextParser.parseAny("کوکو\n1. سبزی را خرد کنید")

        assertEquals("کوکو", draft.title)
        assertEquals(listOf("سبزی را خرد کنید"), draft.steps)
    }

    @Test
    fun `a bare url is recognised as a link not a recipe`() {
        val text = "https://example.invalid/khoresh-recipe"

        assertTrue(RecipeTextParser.looksLikeUrlOnly(text))
        assertEquals("https://example.invalid/khoresh-recipe", RecipeTextParser.parse(text).sourceUrl)
        assertTrue(RecipeTextParser.parse(text).ingredients.isEmpty())
    }

    @Test
    fun `url inside a caption is kept as the source but text still parses`() {
        val draft = RecipeTextParser.parse(
            """
            مواد لازم:
            برنج
            این دستور خوبه
            https://example.invalid/a
            """.trimIndent(),
        )

        assertEquals("https://example.invalid/a", draft.sourceUrl)
        assertEquals(listOf("برنج", "این دستور خوبه"), draft.ingredients)
        assertFalse(RecipeTextParser.looksLikeUrlOnly(draft.title))
    }

    @Test
    fun `empty input is an empty draft not a crash`() {
        val draft = RecipeTextParser.parse("   \n  \n")

        assertEquals("", draft.title)
        assertTrue(draft.ingredients.isEmpty())
        assertTrue(draft.steps.isEmpty())
        assertNull(draft.sourceUrl)
        assertEquals("دستور بدون نام", draft.toFood().name)
    }
}
