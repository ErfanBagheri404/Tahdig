package com.erfanbagheri.tahdig.util

/**
 * Batch-selection state for dish lists (#81).
 *
 * Immutable so Compose can hold it in `mutableStateOf` and re-render on copy.
 * Food ids are `Long`. An empty set means "not selecting" — there is no
 * separate mode flag to drift out of sync with the contents.
 */
data class SelectionSet(val ids: Set<Long> = emptySet()) {

    /** Number of selected dishes. */
    val count: Int get() = ids.size

    /** True while at least one dish is selected (selection mode is on). */
    val isActive: Boolean get() = ids.isNotEmpty()

    /** Toggle one dish in/out. */
    fun toggle(id: Long): SelectionSet =
        if (id in ids) copy(ids = ids - id) else copy(ids = ids + id)

    /** Select one dish (long-press entry keeps existing picks). */
    fun add(id: Long): SelectionSet = copy(ids = ids + id)

    /** Deselect one dish. */
    fun remove(id: Long): SelectionSet = copy(ids = ids - id)

    /** Exit selection mode without applying anything. */
    fun clear(): SelectionSet = copy(ids = emptySet())
}
