package com.erfanbagheri.tahdig.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The filter+sort snapshot a preset stores (#87).
 *
 * Enums travel as their NAME, so a payload written by a newer build (unknown enum
 * constant) still loads: [toFilters] resolves each name with a fallback to the
 * "no constraint" value instead of throwing. That is why the axes are Strings here
 * and enums in the app — never make this class hold the enums directly.
 */
@Serializable
data class FilterPresetPayload(
    val categoryId: Long? = null,
    val diet: String? = null,
    val ingredients: String = "",
    val excluded: String = "",
    val time: String = TimeBucket.ANY.name,
    val difficulty: String = DifficultyFilter.ANY.name,
    val cuisine: String? = null,
    val sort: String = SortOrder.SUGGESTED.name,
)

/** A preset as the UI needs it: the row plus its decoded payload. */
data class ResolvedPreset(
    val categoryId: Long?,
    val diet: DietFilter?,
    val ingredients: String,
    val excluded: String,
    val time: TimeBucket,
    val difficulty: DifficultyFilter,
    val cuisine: String?,
    val sort: SortOrder,
)

object FilterPresetCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(payload: FilterPresetPayload): String = json.encodeToString(payload)

    /** Null only when the stored string is not JSON at all — a corrupt row is dropped, not fatal. */
    fun decode(raw: String): FilterPresetPayload? =
        runCatching { json.decodeFromString<FilterPresetPayload>(raw) }.getOrNull()

    /** Unknown enum names degrade to the "no constraint" value rather than crashing the screen. */
    fun resolve(payload: FilterPresetPayload): ResolvedPreset = ResolvedPreset(
        categoryId = payload.categoryId,
        diet = payload.diet?.let { n -> enumValues<DietFilter>().firstOrNull { it.name == n } },
        ingredients = payload.ingredients,
        excluded = payload.excluded,
        time = enumValues<TimeBucket>().firstOrNull { it.name == payload.time } ?: TimeBucket.ANY,
        difficulty = enumValues<DifficultyFilter>().firstOrNull { it.name == payload.difficulty }
            ?: DifficultyFilter.ANY,
        cuisine = payload.cuisine,
        sort = enumValues<SortOrder>().firstOrNull { it.name == payload.sort } ?: SortOrder.SUGGESTED,
    )
}
