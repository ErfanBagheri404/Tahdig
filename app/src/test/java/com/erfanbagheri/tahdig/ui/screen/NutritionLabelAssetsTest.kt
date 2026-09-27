package com.erfanbagheri.tahdig.ui.screen

import com.erfanbagheri.tahdig.util.NutritionDB
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs the label against the REAL shipped assets (#111) — the same
 * nutrition.json and foods json that go into the APK. A pure-unit test on
 * fixtures proves the maths; this proves the baked data actually reaches the
 * label, which is the part that could silently regress to all-estimates.
 *
 * Reads from the module dir via kotlinx.serialization (org.json is a stub in
 * unit tests), so it needs no device and no instrumentation.
 */
class NutritionLabelAssetsTest {

    private val assets = File("src/main/assets/seed")
    private val json = Json { ignoreUnknownKeys = true }

    private fun loadNutrition(): Map<String, NutritionDB.Entry> {
        val obj = json.parseToJsonElement(File(assets, "nutrition.json").readText()).jsonObject
        return obj.mapValues { (_, v) ->
            val e = v.jsonObject
            fun num(key: String) = e[key]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
            NutritionDB.Entry(
                calories = num("calories").toInt(),
                protein = num("protein"),
                fat = num("fat"),
                carbs = num("carbs"),
                sugars = num("sugars"),
                saturatedFat = num("sat_fat"),
                fiber = num("fiber"),
                salt = num("salt"),
                energyKj = num("kj"),
            )
        }
    }

    private fun dishes(): List<Triple<String, String, String>> {
        val out = mutableListOf<Triple<String, String, String>>()
        File(assets, "foods").listFiles { f -> f.name.endsWith(".json") }?.forEach { fp ->
            json.parseToJsonElement(fp.readText()).jsonArray.forEach { el ->
                val o = el.jsonObject
                out += Triple(
                    o["name"]?.jsonPrimitive?.content ?: "",
                    o["tags"]?.jsonPrimitive?.content ?: "",
                    o["ingredients"]?.jsonPrimitive?.content ?: "",
                )
            }
        }
        return out
    }

    @Test
    fun nutritionAssetIsBaked() {
        val table = loadNutrition()
        assertTrue("nutrition.json is empty — label would be estimate-only", table.size > 200)
        // Hand-checked SR Legacy values.
        assertEquals(387, table["شکر"]?.calories)
        assertEquals(717, table["کره"]?.calories)
        assertEquals(365, table["برنج"]?.calories)
        assertEquals(884, table["روغن زیتون"]?.calories)
        assertEquals(364, table["flour"]?.calories)
        assertEquals(717, table["butter"]?.calories)
    }

    @Test
    fun mostDishesReachRealData() {
        NutritionDB.reset()
        NutritionDB.installEntries(loadNutrition())
        val all = dishes()
        assertTrue("no dishes read from assets", all.size > 1000)

        val real = all.count { (n, t, i) -> !NutritionLabelData.of(n, t, i).estimated }
        val pct = 100.0 * real / all.size
        println("real coverage: $real/${all.size} = ${"%.1f".format(pct)}%")
        // 91% measured; the floor guards against a resolver regression that
        // quietly drops everything back to the heuristic.
        assertTrue("coverage collapsed to ${"%.1f".format(pct)}%", pct >= 85.0)
    }

    @Test
    fun realDishCarriesGradeAndEstimateDoesNot() {
        NutritionDB.reset()
        NutritionDB.installEntries(loadNutrition())
        val all = dishes()

        val labels = all.map { (n, t, i) -> NutritionLabelData.of(n, t, i) }
        val real = labels.first { !it.estimated }
        assertNotNull("real dish produced no grade", real.score)
        assertNotNull("real dish produced no NOVA", real.nova)
        assertTrue(real.calories > 0)

        // Whatever dish the heuristic owns, it must show no grade.
        labels.firstOrNull { it.estimated }?.let { est ->
            assertEquals(null, est.score)
            assertEquals(null, est.nova)
        }
    }

    @Test
    fun noDishReportsZeroCalories() {
        NutritionDB.reset()
        NutritionDB.installEntries(loadNutrition())
        val bad = dishes().map { (n, t, i) -> n to NutritionLabelData.of(n, t, i) }
            .filter { it.second.calories <= 0 }
        assertTrue("dishes with a zero-calorie label: ${bad.take(5)}", bad.isEmpty())
    }
}
