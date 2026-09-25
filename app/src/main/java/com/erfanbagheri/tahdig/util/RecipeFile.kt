package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * `.tahdig.json` — the portable recipe format (#132).
 *
 * Without a file format, recipes cannot travel between devices or people, and
 * Tahdig has no backend to move them. A shared file is the whole story: the
 * app writes one, a messenger carries it, the other app imports it.
 *
 * Design notes:
 * - [version] is checked *before* anything else. A future file with fields we
 *   do not know about must fail loudly rather than import half a recipe.
 * - Everything is optional except the name, so a hand-written or truncated
 *   file still imports the parts that are there instead of being rejected.
 * - The photo travels as base64 only when present — a bundle of 100 recipes
 *   with photos would otherwise be tens of megabytes of JSON.
 * - No `steps` field. Step mode already derives steps from [description] by
 *   sentence split, so a second copy here would only be a way to disagree.
 */
object RecipeFile {

    /** Bump when a field's meaning changes; the guard is exact-match. */
    const val VERSION = 1

    const val MIME = "application/json"
    const val EXTENSION = "tahdig.json"

    @Serializable
    data class Recipe(
        val name: String,
        @SerialName("name_en") val nameEn: String = "",
        val description: String = "",
        val category: String = "",
        val cuisine: String = "",
        val mealTimes: List<String> = emptyList(),
        val tags: List<String> = emptyList(),
        val ingredients: List<String> = emptyList(),
        val difficulty: String = "",
        @SerialName("prep_time_min") val prepTimeMin: Int = 0,
        /** Base64 JPEG/PNG, optional — see the class doc. */
        val photo: String? = null,
    )

    @Serializable
    data class File(
        val version: Int = VERSION,
        val recipes: List<Recipe> = emptyList(),
    )

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = false // a version guard is worthless if unknown keys slide
        encodeDefaults = true
    }

    /**
     * Header-only read. A file from a newer app carries fields this build has
     * never heard of, so the strict parser would reject it as "invalid" — the
     * wrong message for the most likely real case. Peek at the version leniently
     * first, so "update Tahdig" wins over "this file is broken".
     */
    private val header = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Thrown for anything the user should see as a Farsi message, not a stack trace. */
    class InvalidRecipeFile(message: String) : Exception(message)

    fun encode(recipes: List<Recipe>): String =
        json.encodeToString(File.serializer(), File(version = VERSION, recipes = recipes))

    fun encodeOne(recipe: Recipe): String = encode(listOf(recipe))

    /**
     * Parse a file, rejecting a future version before touching the payload.
     *
     * @throws InvalidRecipeFile with a user-facing Farsi message.
     */
    fun decode(text: String): List<Recipe> {
        val version = try {
            header.parseToJsonElement(text).jsonObject["version"]?.jsonPrimitive?.int ?: VERSION
        } catch (e: Exception) {
            throw InvalidRecipeFile("فایل دستور معتبر نیست")
        }
        if (version > VERSION) {
            throw InvalidRecipeFile(
                "این فایل با نسخهٔ جدیدتری از ته‌دیگ ساخته شده (نسخهٔ $version). برنامه را به‌روز کن.",
            )
        }
        val parsed = try {
            json.decodeFromString(File.serializer(), text)
        } catch (e: Exception) {
            throw InvalidRecipeFile("فایل دستور معتبر نیست")
        }
        if (parsed.recipes.isEmpty()) {
            throw InvalidRecipeFile("این فایل هیچ دستوری ندارد")
        }
        parsed.recipes.forEach {
            if (it.name.isBlank()) throw InvalidRecipeFile("دستور بی‌نام در فایل هست")
        }
        return parsed.recipes
    }

    /** Dish -> portable form. The inverse of [toFood]; round-trip is tested. */
    fun fromFood(food: FoodEntity): Recipe = Recipe(
        name = food.name,
        nameEn = food.nameEn,
        description = food.description,
        category = food.categoryId.toString(),
        cuisine = food.cuisine,
        mealTimes = splitList(food.mealTime),
        tags = splitList(food.tags),
        ingredients = splitLines(food.ingredients),
        difficulty = food.difficulty,
        prepTimeMin = food.prepTimeMin,
        photo = null, // photos are DB blobs, not part of the portable file by default
    )

    /**
     * Portable form -> dish. [newId] is supplied by the caller because the DB
     * assigns ids; importing must never overwrite an existing dish.
     */
    fun toFood(recipe: Recipe, newId: Long): FoodEntity = FoodEntity(
        id = newId,
        name = recipe.name,
        nameEn = recipe.nameEn,
        description = recipe.description,
        categoryId = recipe.category.toLongOrNull() ?: 0,
        cuisine = recipe.cuisine,
        mealTime = recipe.mealTimes.joinToString(","),
        tags = recipe.tags.joinToString(","),
        ingredients = recipe.ingredients.joinToString("\n"),
        difficulty = recipe.difficulty,
        prepTimeMin = recipe.prepTimeMin,
        imageUrl = null,
    )

    private fun splitList(s: String): List<String> =
        s.split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }

    private fun splitLines(s: String): List<String> =
        s.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
}
