package com.erfanbagheri.tahdig.util

/**
 * Aisle presentation rules for the shopping list (#108) — pure so the AC's
 * "group reordering" can be unit-tested without prefs or a DB.
 *
 * Renames and hides are VIEW-ONLY transformations: the canonical aisle key
 * (and every row's data) is untouched, only what gets rendered changes.
 * A hidden aisle's items are not dropped — they move into [OTHER] so nothing
 * the user needs in-store disappears.
 */
object AislePlanner {

    /** Always rendered last, whatever the order list says. */
    const val OTHER = "سایر"

    /**
     * Canonical aisle order the manager edits (#108) — the store's walking
     * route. OTHER is deliberately absent: it is always last by [plan].
     */
    val KNOWN = listOf(
        "پروتئین", "لبنیات", "سبزیجات", "حبوبات", "غلات",
        "ادویه", "روغن و سس",
    )

    /** One rendered section: header text + the rows under it (order preserved). */
    data class Section<T>(val header: String, val rows: List<T>)

    /**
     * Turn raw aisle-groups into render sections:
     * custom order first (as persisted, oldest-favorite first), then any aisle
     * not in the order list in natural order, OTHER strictly last — renames
     * applied to headers, hidden aisles folded into OTHER.
     */
    fun <T> plan(
        groups: Map<String, List<T>>,
        order: List<String> = emptyList(),
        renames: Map<String, String> = emptyMap(),
        hidden: Set<String> = emptySet(),
    ): List<Section<T>> {
        // A hidden aisle must not vanish with its items: merge into OTHER first.
        val folded = HashMap<String, List<T>>()
        for ((aisle, rows) in groups) {
            val target = if (aisle in hidden) OTHER else aisle
            folded[target] = (folded[target].orEmpty()) + rows
        }
        val rest = folded.keys.filterNot { it in order }.sorted()
        val ordered = (order.filter { it in folded.keys } + rest)
            .filterNot { it == OTHER }
        val header: (String) -> String = { renames[it] ?: it }
        return ordered.map { Section(header(it), folded[it].orEmpty()) } +
            listOfNotNull(
                folded[OTHER]?.takeIf { it.isNotEmpty() }?.let { Section(header(OTHER), it) },
            )
    }

    /**
     * Snapshot numbers for a shopping trip archive (#108): how many rows the
     * trip finished with, and how many of those were ticked off in-store.
     */
    fun tripCounts(rows: List<Pair<String, Boolean>>): Pair<Int, Int> =
        rows.size to rows.count { it.second }
}
