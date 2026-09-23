package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.NutritionLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The single place a «پختم» turns into a log row (#110). Both cook paths
 * (home card and cook-mode done state) call this, so the two can never drift.
 *
 * The estimate is snapshotted at log time — see [NutritionLogEntity].
 */
object NutritionLog {

    /** Local calendar day key, matching the DAO's `day` column. */
    fun dayKey(at: Long = System.currentTimeMillis()): String = LocalDate.now().toString()

    /** Parse the «۱۲g» strings [NutritionEstimate] produces. */
    internal fun grams(text: String): Int = text.filter { it.isDigit() }.toIntOrNull() ?: 0

    /** Stamp [food]'s per-serving estimate into today's log. */
    fun logCooked(db: TahdigDatabase, food: FoodEntity, scope: CoroutineScope) {
        val info = NutritionEstimate.estimateFromIngredients(food.ingredients)?.info
            ?: NutritionEstimate.estimate(food.name, food.tags)
        scope.launch {
            db.nutritionLogDao().insert(
                NutritionLogEntity(
                    foodId = food.id,
                    foodName = food.name,
                    day = dayKey(),
                    calories = info.calories,
                    protein = grams(info.protein),
                    fat = grams(info.fat),
                    carbs = grams(info.carb),
                ),
            )
        }
    }
}
