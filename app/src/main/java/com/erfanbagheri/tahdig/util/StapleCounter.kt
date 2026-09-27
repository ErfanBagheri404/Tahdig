package com.erfanbagheri.tahdig.util

/**
 * Tap-counter rules for pantry staples (#109) — pure, so the AC's bounds and
 * bulk-undo behaviour are unit-testable without prefs or a DB.
 *
 * Bounds are the whole point: a count can never go negative, and never past
 * [MAX] (a staple you own 99 of is a data-entry mistake, not a pantry).
 */
object StapleCounter {

    /** Highest count a single staple can hold. */
    const val MAX = 99

    /** Clamp any raw count into the legal range — 0 included (row still shown). */
    fun clamp(value: Int): Int = value.coerceIn(0, MAX)

    /** One tap of the − / + stepper. */
    fun step(current: Int, delta: Int): Int = clamp(current + delta)

    /**
     * What «همه پر شد» sets every staple to: one of each. Deliberately not MAX —
     * "stocked" means "I have some", and 99 would be a lie the user must undo.
     */
    fun filledValue(): Int = 1

    /**
     * The undo snapshot for a bulk clear: the exact rows (ids + counts) that
     * were removed, so restoring is a byte-for-byte re-insert rather than a
     * best-effort re-add. Empty list = nothing to restore, so no undo is armed.
     */
    fun <T> snapshotForClear(rows: List<T>): List<T> = rows.toList()
}
