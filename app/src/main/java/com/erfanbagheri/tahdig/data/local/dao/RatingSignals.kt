package com.erfanbagheri.tahdig.data.local.dao

/**
 * A rating plus the one piece of dish context the taste scorer needs (#92).
 *
 * A projection rather than the entity because `categoryId` lives on `foods`:
 * learning "this category is loved" from three individually rated dishes is the
 * behavior #92's acceptance asks for, and a join is cheaper than a second query
 * per dish.
 *
 * No `@ColumnInfo` — all four come from the query's own aliases.
 */
data class RatingSignals(
    val foodId: Long,
    val stars: Int,
    val ratedAt: Long,
    val categoryId: Long,
)
