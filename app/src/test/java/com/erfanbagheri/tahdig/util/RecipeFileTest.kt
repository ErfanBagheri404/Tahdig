package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RecipeFileTest {

    private val dish = FoodEntity(
        id = 7,
        name = "قورمه سبزی",
        nameEn = "Ghormeh Sabzi",
        description = "خورش ایرانی با سبزی و لوبیا",
        categoryId = 3,
        cuisine = "ایرانی",
        mealTime = "ناهار,شام",
        tags = "گیاهی,جشن",
        ingredients = "سبزی خوردن\nلوبیا قرمز\nگوشت گوسفند",
        difficulty = "MEDIUM",
        prepTimeMin = 90,
        imageUrl = "https://example.invalid/q.jpg",
    )

    @Test
    fun `dish survives a full round trip`() {
        val text = RecipeFile.encodeOne(RecipeFile.fromFood(dish))
        val back = RecipeFile.decode(text).single()

        assertEquals(dish.name, back.name)
        assertEquals(dish.nameEn, back.nameEn)
        assertEquals(dish.description, back.description)
        assertEquals(dish.cuisine, back.cuisine)
        assertEquals("3", back.category)
        assertEquals(listOf("ناهار", "شام"), back.mealTimes)
        assertEquals(listOf("گیاهی", "جشن"), back.tags)
        assertEquals(listOf("سبزی خوردن", "لوبیا قرمز", "گوشت گوسفند"), back.ingredients)
        assertEquals(dish.difficulty, back.difficulty)
        assertEquals(90, back.prepTimeMin)
    }

    @Test
    fun `db id is not carried across the wire`() {
        // A new device assigns its own ids; a foreign id must not clobber a local row.
        val back = RecipeFile.decode(RecipeFile.encodeOne(RecipeFile.fromFood(dish))).single()
        val imported = RecipeFile.toFood(back, newId = 99)
        assertEquals(99L, imported.id)
    }

    @Test
    fun `nameEn is preserved exactly`() {
        val back = RecipeFile.decode(RecipeFile.encodeOne(RecipeFile.fromFood(dish))).single()
        assertEquals("Ghormeh Sabzi", back.nameEn)
    }

    @Test
    fun `a bundle of several recipes keeps its order`() {
        val many = (1..5).map { dish.copy(id = it.toLong(), name = "غذای $it") }
        val back = RecipeFile.decode(RecipeFile.encode(many.map(RecipeFile::fromFood)))
        assertEquals(5, back.size)
        back.forEachIndexed { i, r -> assertEquals("غذای ${i + 1}", r.name) }
    }

    @Test
    fun `version is stamped on write`() {
        val text = RecipeFile.encodeOne(RecipeFile.fromFood(dish))
        assertTrue("version field missing", text.contains("\"version\""))
        assertTrue("wrong version", text.contains("\"version\": ${RecipeFile.VERSION}"))
    }

    @Test
    fun `a future version is rejected before the payload matters`() {
        val future = """{"version":${RecipeFile.VERSION + 1},"recipes":[
            {"name":"آش","nonsense_field_we_never_read":123}]}"""
        try {
            RecipeFile.decode(future)
            fail("a newer file must not import")
        } catch (e: RecipeFile.InvalidRecipeFile) {
            assertTrue("message should be user-facing Persian: ${e.message}",
                e.message!!.contains("به‌روز"))
        }
    }

    @Test
    fun `an older version still imports`() {
        val v1 = """{"version":1,"recipes":[{"name":"آش رشته"}]}"""
        assertEquals("آش رشته", RecipeFile.decode(v1).single().name)
    }

    @Test
    fun `truncated json is rejected with a Persian message`() {
        try {
            RecipeFile.decode("""{"version":1,"recipes":[{"name":"آش"""")
            fail("truncated file must not import")
        } catch (e: RecipeFile.InvalidRecipeFile) {
            assertEquals("فایل دستور معتبر نیست", e.message)
        }
    }

    @Test
    fun `empty recipes list is rejected`() {
        try {
            RecipeFile.decode("""{"version":1,"recipes":[]}""")
            fail("an empty file must not import")
        } catch (e: RecipeFile.InvalidRecipeFile) {
            assertTrue(e.message!!.contains("هیچ دستوری"))
        }
    }

    @Test
    fun `a nameless recipe is rejected`() {
        try {
            RecipeFile.decode("""{"version":1,"recipes":[{"name":"   "}]}""")
            fail("a nameless recipe must not import")
        } catch (e: RecipeFile.InvalidRecipeFile) {
            assertTrue(e.message!!.contains("بی‌نام"))
        }
    }

    @Test
    fun `garbage is rejected, not crash`() {
        for (junk in listOf("", "not json", "[]", "{}", "null")) {
            try {
                RecipeFile.decode(junk)
                fail("must reject: '$junk'")
            } catch (_: RecipeFile.InvalidRecipeFile) {
                // expected
            }
        }
    }

    @Test
    fun `a missing optional field defaults instead of failing`() {
        val sparse = """{"version":1,"recipes":[{"name":"کوکو"}]}"""
        val r = RecipeFile.decode(sparse).single()
        assertEquals("کوکو", r.name)
        assertEquals("", r.nameEn)
        assertEquals(emptyList<String>(), r.ingredients)
        assertEquals(0, r.prepTimeMin)
        assertNull(r.photo)
    }

    @Test
    fun `photo base64 rides along when present`() {
        val r = RecipeFile.decode(
            RecipeFile.encodeOne(RecipeFile.fromFood(dish).copy(photo = "aGk=")),
        ).single()
        assertNotNull(r.photo)
        assertEquals("aGk=", r.photo)
    }

    @Test
    fun `an unknown field in a known version is rejected`() {
        // ignoreUnknownKeys is off on purpose: silent field loss is how recipes
        // get quietly mangled between versions.
        val bad = """{"version":1,"recipes":[{"name":"آش","typo_field":1}]}"""
        try {
            RecipeFile.decode(bad)
            fail("unknown field must not import silently")
        } catch (_: RecipeFile.InvalidRecipeFile) {
            // expected
        }
    }

    @Test
    fun `imported dish keeps a valid category and falls back cleanly`() {
        val good = RecipeFile.toFood(RecipeFile.Recipe(name = "x", category = "5"), 1)
        assertEquals(5, good.categoryId)
        val bad = RecipeFile.toFood(RecipeFile.Recipe(name = "x", category = "abc"), 1)
        assertEquals(0, bad.categoryId)
    }

    @Test
    fun `persian and ascii separators both parse`() {
        assertEquals(
            listOf("الف", "ب", "ج"),
            RecipeFile.decode(
                """{"version":1,"recipes":[{"name":"x","mealTimes":["الف","ب","ج"]}]}""",
            ).single().mealTimes,
        )
        // and the encoder normalises the Farsi comma on the way out
        val farsi = dish.copy(mealTime = "الف،ب،ج")
        assertEquals(listOf("الف", "ب", "ج"), RecipeFile.fromFood(farsi).mealTimes)
    }

    @Test
    fun `multiline ingredients survive line to list and back`() {
        val back = RecipeFile.toFood(RecipeFile.fromFood(dish), 1)
        assertEquals(dish.ingredients, back.ingredients)
    }
}
