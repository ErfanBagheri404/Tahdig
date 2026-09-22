package com.erfanbagheri.tahdig.util

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Offline substitution table: which canonical ingredients can stand in for which.
 *
 * Missing saffron shouldn't kill a recipe, and the pantry matcher's main failure is
 * coverage gaps — a dish is "not cookable" when one spice is absent. This table lets
 * both the matcher and the UI answer "what can I use instead?" with no network.
 *
 * `ratio` is how much of the replacement equals one unit of the original, so amounts
 * can be shown through the same scaling math as everything else.
 *
 * ponytail: a flat curated list, not a graph. A swap of a swap (ماست ← خامه ← شیر) is
 * not chained — one hop is what the caveats are written for. Walk the table
 * transitively only if users start asking for second-order swaps.
 */
object SubstitutionRegistry {

    /** One possible replacement for an ingredient. */
    data class Swap(
        val toId: String,
        val toName: String,
        val ratio: Double,
        val caveat: String,
    )

    @Serializable
    private data class Table(val substitutions: List<Row>)

    @Serializable
    private data class Row(val from: String, val subs: List<Sub> = emptyList())

    @Serializable
    private data class Sub(val to: String, val ratio: Double = 1.0, val caveat: String = "")

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var table: Map<String, List<Swap>> = emptyMap()

    /** Number of ingredients that have at least one swap. */
    val size: Int get() = table.size

    fun load(context: Context) {
        if (table.isNotEmpty()) return
        synchronized(this) {
            if (table.isNotEmpty()) return
            val text = context.assets.open("seed/substitutions.json")
                .bufferedReader().use { it.readText() }
            installFromJson(text)
        }
    }

    /** Parse+install without Android so unit tests can drive the real bundled data. */
    fun installFromJson(text: String) {
        val parsed = json.decodeFromString<Table>(text)
        val map = HashMap<String, List<Swap>>(parsed.substitutions.size)
        for (row in parsed.substitutions) {
            // Names resolve through the glossary so the UI never shows a raw id, and an
            // entry whose id drifted out of the glossary is dropped rather than rendered
            // as a bare slug.
            val named = row.subs.mapNotNull { s ->
                val ing = IngredientRegistry.byId(s.to)
                if (ing == null) null
                else Swap(toId = s.to, toName = ing.fa, ratio = s.ratio, caveat = s.caveat)
            }
            if (named.isNotEmpty()) map[row.from] = named
        }
        table = map
    }

    /** Swaps for a canonical id, or empty when there are none. */
    fun swapsFor(id: String): List<Swap> = table[id].orEmpty()

    /** Swaps for a free-text ingredient name, resolving it first. */
    fun swapsForName(name: String): List<Swap> =
        IngredientRegistry.resolve(name)?.let { swapsFor(it.id) }.orEmpty()

    /** Every id that has at least one swap, for coverage reporting. */
    fun coveredIds(): Set<String> = table.keys

    /**
     * Pantry expansion: for each ingredient the user is missing, the canonical ids that
     * would satisfy it. Used by the pantry matcher so having ماست counts as covering a
     * need for ماست چکیده.
     *
     * Only substitutes the user actually has are returned — an expansion to something
     * they also lack would just move the gap.
     */
    fun availableSubstitutesFor(missingId: String, haveIds: Set<String>): List<String> =
        swapsFor(missingId).map { it.toId }.filter { it in haveIds }
}
