package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.FoodEntity

/**
 * One unreviewed recipe, from anywhere (#75).
 *
 * The share sheet, pasted text, and (later) OCR / video / web imports all
 * converge here so there is exactly one review UI and one path into the DB.
 * Extension rule for #76–#78: fill these six fields, reuse the review screen,
 * do not invent a second draft type.
 */
data class RecipeDraft(
    val title: String = "",
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val sourceUrl: String? = null,
    val photoUrl: String? = null,
    val notes: String = "",
) {
    /** Draft -> dish. Steps ride [FoodEntity.description]; step mode already splits it. */
    fun toFood(newId: Long = 0): FoodEntity = FoodEntity(
        id = newId,
        name = title.ifBlank { "دستور بدون نام" },
        nameEn = "",
        categoryId = 0,
        mealTime = "",
        cuisine = "",
        difficulty = "",
        prepTimeMin = 0,
        ingredients = ingredients.joinToString("\n"),
        description = steps.joinToString("\n"),
    )
}
