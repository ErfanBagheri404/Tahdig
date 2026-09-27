package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.CollectionCount

/**
 * #79 — the pure part of the collection filters: which chips the Favorites tab
 * shows, and what happens to the selected one when a collection is deleted.
 *
 * No Compose, no DB, so the whole rule is unit-testable.
 */
object CollectionMath {

    /** One tab-strip entry. `collectionId == null` is the «همه» chip. */
    data class Chip(val collectionId: Long?, val label: String)

    /**
     * The chip row: «همه» plus one chip per collection, each carrying its dish
     * count. Rendered flat and hairline-separated — no rounded cards.
     */
    fun chips(counts: List<CollectionCount>): List<Chip> =
        listOf(Chip(null, "همه")) + counts.map { Chip(it.id, countLabel(it.count)) }

    /** «۳ غذا» — Persian digits, singular for 1. */
    fun countLabel(count: Int): String = when (count) {
        0 -> "خالی"
        1 -> "۱ غذا"
        else -> "${PersianText.toPersianDigits(count)} غذا"
    }

    /**
     * Keeps a valid selection. Deleting the collection the user was filtering by
     * must fall back to «همه», or the list keeps filtering on an id that is gone.
     */
    fun resolveSelection(selectedId: Long?, counts: List<CollectionCount>): Long? =
        if (selectedId != null && counts.none { it.id == selectedId }) null else selectedId
}
