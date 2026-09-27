package com.erfanbagheri.tahdig.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

/**
 * Open Food Facts product lookup by barcode (#116).
 *
 * Stdlib [HttpURLConnection] on purpose: OkHttp is present only as Coil's
 * transitive dependency, and a second HTTP stack for one GET is not worth it.
 *
 * FREE and key-less by design — OFF needs no key for the v2 product API, and a
 * key would be one more secret to leak. The custom User-Agent is required by
 * OFF's terms; without it the API answers 403.
 */
object OffClient {

    private const val ENDPOINT = "https://world.openfoodfacts.org/api/v2/product"
    private const val UA = "Tahdig/0.1.2 (Android; offline recipe app)"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * One product's facts. Every nutrient is nullable: OFF leaves a field out
     * when a lab never assayed it, and a missing assay is not a measured zero.
     */
    data class Product(
        val barcode: String,
        val name: String,
        val brands: String,
        val imageUrl: String?,
        val kcal100g: Double?,
        val protein100g: Double?,
        val carb100g: Double?,
        val fat100g: Double?,
        val salt100g: Double?,
        val sugars100g: Double?,
        val allergens: List<String>,
        val nutriscore: String?,
        val nova: Int?,
    )

    sealed interface Result2 {
        data class Found(val product: Product) : Result2
        /** OFF answered cleanly, but the barcode is not in their database. */
        data object NotFound : Result2
        /** Offline, timed out, or the API failed. The caller says so plainly. */
        data object Offline : Result2
    }

    /**
     * Fetch one product. Never throws — every failure is a value, because the
     * AC wants an honest offline hint, not an error modal.
     */
    fun lookup(barcode: String): Result2 {
        val code = barcode.trim()
        if (code.isEmpty()) return Result2.NotFound
        val url = URL("$ENDPOINT/$code.json?fields=" + FIELDS)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode != 200) return Result2.Offline
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(body).jsonObject
            // OFF returns status as the INT 1 on a hit and 0 on a miss — not
            // the string "success" the v1 API used. Accept both shapes so a
            // cache hit is never mistaken for "not found".
            val status = root["status"]?.jsonPrimitive?.content
            if (status != null && status != "1" && status != "success") {
                return Result2.NotFound
            }
            val product = root["product"] ?: return Result2.NotFound
            if (product is kotlinx.serialization.json.JsonNull) return Result2.NotFound
            return parse(code, product.jsonObject)?.let { Result2.Found(it) } ?: Result2.NotFound
        } catch (_: Exception) {
            // Any network/parse failure is the same honest answer: we could not
            // reach OFF. Distinguishing them would leak implementation detail
            // into the UI for no user benefit.
            return Result2.Offline
        } finally {
            conn.disconnect()
        }
    }

    /** Parse the product object. Returns null when OFF found no name. */
    fun parse(barcode: String, product: JsonObject): Product? {
        fun str(key: String): String? = product[key]?.jsonPrimitive?.contentOrNullTrimmed()

        val name = str("product_name")?.takeIf { it.isNotBlank() }
            ?: str("product_name_en")?.takeIf { it.isNotBlank() }
            ?: return null

        // nutriments is a nested object: { "energy-kcal_100g": 123, "salt_100g": 1.2 }.
        // Each read is independent so a food assayed for protein but not salt
        // reports what it has and leaves the rest null.
        val nutriments = product["nutriments"]?.let { el ->
            runCatching { el.jsonObject }.getOrNull()
        }

        fun per100(key: String): Double? =
            nutriments?.get(key)?.jsonPrimitive?.doubleOrNull

        return Product(
            barcode = barcode,
            name = name,
            brands = str("brands").orEmpty(),
            imageUrl = str("image_front_small_url")?.takeIf { it.isNotBlank() },
            kcal100g = per100("energy-kcal_100g") ?: per100("energy_100g")?.div(4.184),
            protein100g = per100("proteins_100g"),
            carb100g = per100("carbohydrates_100g"),
            fat100g = per100("fat_100g"),
            salt100g = per100("salt_100g"),
            sugars100g = per100("sugars_100g"),
            allergens = readAllergens(product),
            nutriscore = str("nutriscore_grade")?.uppercase()?.takeIf { it.length == 1 },
            nova = product["nova"]?.jsonPrimitive?.content?.toIntOrNull(),
        )
    }

    /** The exact field list requested — a leaner query is a faster mobile fetch. */
    private const val FIELDS =
        "code,product_name,product_name_en,brands,image_front_small_url," +
            "nutriments,nutriscore_grade,nova,allergens_tags"

    private fun readAllergens(product: JsonObject): List<String> {
        val tags = product["allergens_tags"] ?: return emptyList()
        // OFF repeats tags across its own language/edits entries and sometimes
        // stores a scalar instead of an array, so both shapes must be tolerated.
        val list = runCatching { tags.jsonArray }.getOrNull() ?: return emptyList()
        return list.mapNotNull { el ->
            el.jsonPrimitive.content.removePrefix("en:")
                .replace('-', ' ')
                .trim()
                .takeIf { it.isNotEmpty() }
        }.distinct()
    }

    /** OFF ids are strings, but a null/absent key must not throw. */
    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullTrimmed(): String? =
        runCatching { content }.getOrNull()?.trim()
}
