package com.erfanbagheri.tahdig.data.local.dao

/**
 * How many times a dish was cooked (#92).
 *
 * No `@ColumnInfo`: the query aliases its columns to `foodId`/`cooks` and Room
 * maps by field name, so a `name = "food_id"` annotation would make it look
 * for a snake_case column the query never returns.
 */
data class CookCount(
    val foodId: Long,
    val cooks: Int,
)
