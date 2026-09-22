package com.erfanbagheri.tahdig.data.local.seed

import android.content.res.AssetManager
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.CategoryEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.util.PersianText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Loads the bundled seed dataset from the assets/seed JSON files into Room.
 *
 * Files are shipped as JSON (not a pre-built .db) so the dataset is diffable in git,
 * editable without Android tooling, and validated by the unit tests on every build.
 */
object SeedLoader {

    private const val CATEGORIES_FILE = "seed/categories.json"

    /** One JSON array per category, read from seed/foods/. Split so the dataset stays diffable. */
    private const val FOODS_DIR = "seed/foods"

    /**
     * Dish photos, keyed by food id as a string: {"12": "https://…/thumb.jpg"}.
     *
     * Kept in its own file rather than written into the 27 seed files so the
     * importer can re-run and refresh URLs without rewriting the whole dataset
     * (keeps the seed diffs readable). Missing / unparsable file = no photos,
     * and every screen falls back to the category emoji.
     */
    private const val IMAGES_FILE = "seed/images.json"

    /**
     * Baked per-dish equipment ({ "3": ["تابه", "فر"] }), keyed by food id.
     * Same ownership as images.json: written by scripts/bake_equipment.py so the
     * 27 seed files stay a plain dish diff. Missing file = runtime inference only.
     */
    private const val EQUIPMENT_FILE = "seed/equipment.json"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadInto(db: TahdigDatabase, assets: AssetManager) = withContext(Dispatchers.IO) {
        val categories = json.decodeFromString<List<CategorySeed>>(
            assets.open(CATEGORIES_FILE).bufferedReader().use { it.readText() }
        )
        val foods = assets.list(FOODS_DIR).orEmpty()
            .filter { it.endsWith(".json") }
            .sorted()
            .flatMap { file ->
                json.decodeFromString<List<FoodSeed>>(
                    assets.open("$FOODS_DIR/$file").bufferedReader().use { it.readText() }
                )
            }
        val images = readImages(assets)
        val equipment = readEquipment(assets)

        // Insert categories first — foods reference them by slug.
        db.categoryDao().insertAll(
            categories.map {
                CategoryEntity(
                    id = it.id,
                    name = it.name,
                    nameEn = it.nameEn,
                    sortOrder = it.sortOrder,
                    emoji = it.emoji,
                )
            }
        )

        db.foodDao().insertAll(
            foods.map {
                FoodEntity(
                    id = it.id,
                    name = it.name,
                    nameEn = it.nameEn,
                    categoryId = it.categoryId,
                    mealTime = it.mealTime,
                    cuisine = it.cuisine,
                    difficulty = it.difficulty,
                    prepTimeMin = it.prepTimeMin,
                    ingredients = it.ingredients,
                    tags = it.tags,
                    description = it.description,
                    imageUrl = it.imageUrl ?: images[it.id.toString()]?.url,
                    equipment = equipment[it.id].orEmpty().joinToString(","),
                    priority = it.priority,
                )
            }
        )
    }

    /**
     * Photos are optional and best-effort: any failure yields an empty map so the
     * app still seeds and renders emoji-only rather than failing to start.
     */
    private fun readImages(assets: AssetManager): Map<String, ImageSeed> = try {
        json.decodeFromString<Map<String, ImageSeed>>(
            assets.open(IMAGES_FILE).bufferedReader().use { it.readText() }
        )
    } catch (_: Exception) {
        emptyMap()
    }

    /** Best-effort like images: a missing/broken file degrades to inference, never a crash. */
    private fun readEquipment(assets: AssetManager): Map<Long, List<String>> = try {
        json.decodeFromString<Map<String, List<String>>>(
            assets.open(EQUIPMENT_FILE).bufferedReader().use { it.readText() }
        ).mapNotNull { (k, v) -> k.toLongOrNull()?.let { it to v } }.toMap()
    } catch (_: Exception) {
        emptyMap()
    }

    /** Normalized search key — kept here so tests and the DAO agree on one definition. */
    fun searchKey(name: String): String = PersianText.normalize(name)
}

@Serializable
private data class CategorySeed(
    val id: Long,
    val name: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val emoji: String = "",
)

@Serializable
private data class ImageSeed(
    val name: String = "",
    val url: String = "",
    val src: String? = null,
    val page: String? = null,
)

@Serializable
private data class FoodSeed(
    val id: Long,
    val name: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("category_id") val categoryId: Long,
    @SerialName("meal_time") val mealTime: String,
    val cuisine: String = "IRANI",
    val difficulty: String = "MEDIUM",
    @SerialName("prep_time_min") val prepTimeMin: Int = 30,
    val ingredients: String = "",
    val tags: String = "",
    val description: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    val priority: Int = 0,
)
