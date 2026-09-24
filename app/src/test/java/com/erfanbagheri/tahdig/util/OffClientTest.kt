package com.erfanbagheri.tahdig.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #116 OFF product parsing. The JSON below is the SHAPE a real v2 response
 * has, trimmed to the fields the app reads — OFF returns `status` as the int
 * 1, not the string "success" the v1 API used, and nutriments is a nested
 * object keyed by "<nutrient>_100g".
 */
class OffClientTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun product(body: String) =
        OffClient.parse("737628064502", json.parseToJsonElement(body).jsonObject)!!

    @Test
    fun readsNameBrandAndPer100gNutrients() {
        val p = product(
            """
            {
              "product_name": "Tomato Ketchup",
              "brands": "Heinz",
              "nutriments": {
                "energy-kcal_100g": 112.0,
                "proteins_100g": 1.2,
                "carbohydrates_100g": 25.8,
                "fat_100g": 0.1,
                "salt_100g": 2.3,
                "sugars_100g": 21.8
              }
            }
            """.trimIndent(),
        )
        assertEquals("Tomato Ketchup", p.name)
        assertEquals("Heinz", p.brands)
        assertEquals(112.0, p.kcal100g!!, 0.001)
        assertEquals(1.2, p.protein100g!!, 0.001)
        assertEquals(25.8, p.carb100g!!, 0.001)
        assertEquals(0.1, p.fat100g!!, 0.001)
        assertEquals(2.3, p.salt100g!!, 0.001)
        assertEquals(21.8, p.sugars100g!!, 0.001)
    }

    @Test
    fun missingNutrimentStaysNullNotZero() {
        // OFF omits a field nobody assayed. Reporting 0 would claim the food
        // contains none, which is a different statement.
        val p = product(
            """
            {
              "product_name": "Rice Crackers",
              "nutriments": { "energy-kcal_100g": 380.0 }
            }
            """.trimIndent(),
        )
        assertEquals(380.0, p.kcal100g!!, 0.001)
        assertNull(p.salt100g)
        assertNull(p.sugars100g)
        assertNull(p.nutriscore)
        assertTrue(p.allergens.isEmpty())
    }

    @Test
    fun energyKjConvertsToKcalWhenKcalIsAbsent() {
        val p = product(
            """
            {
              "product_name": "Lentils",
              "nutriments": { "energy_100g": 1452.0 }
            }
            """.trimIndent(),
        )
        // 1452 kJ / 4.184 = 347.0363 kcal (computed, not rounded to look tidy)
        assertEquals(347.0363288718929, p.kcal100g!!, 0.0001)
    }

    @Test
    fun energyKcalWinsOverKjWhenBothPresent() {
        val p = product(
            """
            {
              "product_name": "Both",
              "nutriments": { "energy-kcal_100g": 100.0, "energy_100g": 418.4 }
            }
            """.trimIndent(),
        )
        assertEquals(100.0, p.kcal100g!!, 0.001)
    }

    @Test
    fun englishNameIsTheFallback() {
        val p = product("""{"product_name_en": "Oat Drink"}""")
        assertEquals("Oat Drink", p.name)
    }

    @Test
    fun productWithNoNameAtAllIsNotAParsedProduct() {
        val body = """{"brands": "Nothing Co"}"""
        val parsed = OffClient.parse("123", json.parseToJsonElement(body).jsonObject)
        assertNull("a nameless OFF record is not a product", parsed)
    }

    @Test
    fun nutriscoreIsUppercasedAndSingleLetter() {
        assertEquals("A", product("""{"product_name":"x","nutriscore_grade":"a"}""").nutriscore)
        // A multi-char string is not a Nutri-Score grade.
        assertNull(product("""{"product_name":"x","nutriscore_grade":"unknown"}""").nutriscore)
    }

    @Test
    fun allergenTagsAreCleanedOfTheirLanguagePrefix() {
        val p = product(
            """
            {
              "product_name": "Biscuits",
              "allergens_tags": ["en:gluten", "en:milk", "en:gluten"]
            }
            """.trimIndent(),
        )
        assertEquals(listOf("gluten", "milk"), p.allergens)
    }

    @Test
    fun malformedNutrimentsObjectDoesNotThrow() {
        // OFF sometimes answers nutriments as a string for malformed imports.
        val p = product("""{"product_name":"Odd","nutriments":"n/a"}""")
        assertEquals("Odd", p.name)
        assertNull(p.kcal100g)
    }

    @Test
    fun novaGradeIsReadAsAnInt() {
        assertEquals(4, product("""{"product_name":"x","nova":4}""").nova)
        assertNull(product("""{"product_name":"x"}""").nova)
    }
}
