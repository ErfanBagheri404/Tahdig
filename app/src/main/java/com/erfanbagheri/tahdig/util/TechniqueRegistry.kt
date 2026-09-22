package com.erfanbagheri.tahdig.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Technique library (#101) — bundled knowledge, entity-free: versioned with the
 * app as an asset, no Room table. Loaded once at app start; installable in JVM
 * tests from the real asset file (see [installFromJson]).
 */
@Serializable
data class Technique(
    val id: String,
    val name: String,
    val keywords: List<String>,
    val body: String,
)

object TechniqueRegistry {

    private var table: List<Technique> = emptyList()
    private val json = Json { ignoreUnknownKeys = true }

    /** App-start load; never throws — an empty library degrades to no links. */
    fun load(text: String) {
        table = runCatching { json.decodeFromString<List<Technique>>(text) }
            .getOrDefault(emptyList())
    }

    /** Context overload for Application.onCreate: reads the bundled asset, never throws. */
    fun load(context: android.content.Context) {
        load(
            runCatching {
                context.assets.open("seed/techniques.json").bufferedReader().use { it.readText() }
            }.getOrDefault("[]"),
        )
    }

    /** Test hook: install directly from the asset text. */
    fun installFromJson(text: String) = load(text)

    fun all(): List<Technique> = table

    fun byId(id: String): Technique? = table.firstOrNull { it.id == id }

    /** True when any keyword of [tech] boundary-matches [text] — used for reverse links. */
    fun matches(tech: Technique, text: String): Boolean = TechniqueLinker.hasKeyword(text, tech)
}
