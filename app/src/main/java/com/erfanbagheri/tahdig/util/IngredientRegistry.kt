package com.erfanbagheri.tahdig.util

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Canonical ingredient vocabulary, loaded once from `assets/seed/ingredients.json`.
 *
 * Why it exists: the seed stores ingredients as free text, so «پیاز قرمز», «پیاز» and
 * "Red Onion" were three unrelated strings. Everything downstream that compares
 * ingredients — shopping dedupe, pantry matching, substitution, missing-diff — needs
 * one answer to "are these the same thing?".
 *
 * Resolution is alias → canonical id. Unknown text returns null and callers keep the
 * raw string: a word we don't know must never vanish from a shopping list.
 *
 * ponytail: exact alias matching after normalization, no fuzz. The seed's spelling is
 * consistent enough (99.7% of tokens resolve) that edit distance would only add false
 * merges — «پسته» vs «پستا» is not worth risk. Add a token-overlap fallback if real
 * user recipes start missing.
 */
object IngredientRegistry {

    /** One canonical ingredient. */
    data class Ingredient(
        val id: String,
        val fa: String,
        val en: String,
        val aisle: String,
        val baseUnit: String,
        val aliases: List<String>,
        val densityGPerCup: Double?,
        val allergen: String?,
    )

    @Serializable
    private data class Table(val ingredients: List<Row>)

    @Serializable
    private data class Row(
        val id: String,
        val fa: String,
        val en: String = "",
        val aisle: String = "",
        val baseUnit: String = "",
        val aliases: List<String> = emptyList(),
        val densityGPerCup: Double? = null,
        val allergen: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var byAlias: Map<String, Ingredient> = emptyMap()

    @Volatile
    private var byId: Map<String, Ingredient> = emptyMap()

    val size: Int get() = byId.size

    /** Load the bundled table. Idempotent; safe to call from Application.onCreate. */
    fun load(context: Context) {
        if (byId.isNotEmpty()) return
        synchronized(this) {
            if (byId.isNotEmpty()) return
            val text = context.assets.open("seed/ingredients.json")
                .bufferedReader().use { it.readText() }
            installFromJson(text)
        }
    }

    /**
     * Parse+install without Android, so unit tests can drive the real bundled data.
     *
     * ponytail: kotlinx.serialization rather than org.json — org.json is a stubbed
     * no-op in JVM unit tests ("not mocked"), which would force Robolectric onto a pure
     * data table. kotlinx is already a dependency and parses this shape identically.
     */
    fun installFromJson(text: String) {
        val table = json.decodeFromString<Table>(text)
        val aliasMap = HashMap<String, Ingredient>(table.ingredients.size * 2)
        val idMap = HashMap<String, Ingredient>(table.ingredients.size)
        for (r in table.ingredients) {
            val ing = Ingredient(
                id = r.id,
                fa = r.fa,
                en = r.en.ifBlank { r.fa },
                aisle = r.aisle.ifBlank { "سایر" },
                baseUnit = r.baseUnit,
                aliases = r.aliases,
                densityGPerCup = r.densityGPerCup,
                allergen = r.allergen,
            )
            idMap[ing.id] = ing
            // Canonical names win over aliases: install them last so an alias listed
            // by two entries cannot steal a canonical spelling.
            for (a in ing.aliases) aliasMap[keyKey(a)] = ing
            aliasMap[keyKey(ing.fa)] = ing
            aliasMap[keyKey(ing.en)] = ing
        }
        byId = idMap
        byAlias = aliasMap
    }

    /** Canonical ingredient for a free-text name, or null when unknown. */
    fun resolve(freeText: String): Ingredient? =
        byAlias[keyKey(freeText)]

    fun byId(id: String): Ingredient? = byId[id]

    /**
     * [IngredientParser.key]'s normalization: ZWNJ and Arabic letter variants folded,
     * all whitespace removed so «سیب‌زمینی» and «سیب زمینی» are one key.
     */
    fun keyKey(raw: String): String =
        PersianText.normalize(raw)
            .lowercase()
            .filterNot { it.isWhitespace() }

    /**
     * Merge key for a parsed line: the canonical id when known, else the old
     * normalized (unit|item) pair. Shopping rows built from canonical ids collapse
     * «پیاز قرمز» into «پیاز» while unknown items keep their previous behaviour.
     */
    fun mergeKeyFor(unit: String?, item: String): String {
        val ing = resolve(item)
        return if (ing != null) "id:" + ing.id else IngredientParser.mergeKey(unit, item)
    }

    /** Display bucket for an item: the canonical aisle, else the old keyword bucket. */
    /**
     * The ~20 staples worth a one-tap counter row (#109), taken from the real
     * glossary so every entry resolves and carries its aisle. Order is the
     * store route, so the counter block reads like the shopping list.
     */
    fun commonStaples(): List<Ingredient> =
        byId.values
            .filter { it.aisle in StapleAisles }
            .sortedWith(compareBy({ StapleAisles.indexOf(it.aisle) }, { it.fa }))
            .take(20)

    private val StapleAisles = listOf("غلات", "پروتئین", "لبنیات", "سبزیجات", "حبوبات")

    fun aisleOf(item: String): String =
        resolve(item)?.aisle ?: IngredientParser.categoryOf(item)

    /**
     * Allergen tags present in a set of ingredient names.
     * Used by the diet filter so "nut-free" is a real check, not a keyword guess.
     */
    fun allergensIn(items: List<String>): Set<String> =
        items.mapNotNull { resolve(it)?.allergen }.toSet()
}
