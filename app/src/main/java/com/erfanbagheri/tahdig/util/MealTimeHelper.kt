package com.erfanbagheri.tahdig.util

import java.util.Calendar

/**
 * Maps system clock to a meal-time bucket used as the DB query key.
 * Gaps between ranges default to SNACK (میان‌وعده).
 */
object MealTimeHelper {

    /** DB bucket keys — must match meal_time column values in FoodEntity. */
    const val BREAKFAST = "BREAKFAST"
    const val LUNCH = "LUNCH"
    const val DINNER = "DINNER"
    const val SNACK = "SNACK"
    const val LIGHT_DINNER = "LIGHT_DINNER"

    /** Farsi labels for display */
    const val BREAKFAST_FA = "صبحانه"
    const val LUNCH_FA = "ناهار"
    const val DINNER_FA = "شام"
    const val SNACK_FA = "میان‌وعده"
    const val LIGHT_DINNER_FA = "شام سبک"

    /** Current hour (0–23) from system clock. */
    private fun currentHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    /** Current minute within the hour. */
    private fun currentMinute(): Int = Calendar.getInstance().get(Calendar.MINUTE)

    /** Time as minutes-since-midnight for easy comparison. */
    private fun nowMinutes(): Int = currentHour() * 60 + currentMinute()

    /**
     * Returns the meal-time bucket key for the current system time.
     *
     * Breakdown per overview doc:
     * - 06:00 – 10:30  → BREAKFAST
     * - 11:30 – 14:30  → LUNCH
     * - 15:00 – 17:30  → SNACK
     * - 18:30 – 22:00  → DINNER
     * - 22:00 – 01:00  → LIGHT_DINNER
     * - outside ranges  → SNACK
     */
    fun currentBucket(): String {
        val t = nowMinutes()
        return when {
            t in 360..629   -> BREAKFAST    // 06:00 – 10:29
            t in 690..869   -> LUNCH        // 11:30 – 14:29
            t in 900..1049  -> SNACK        // 15:00 – 17:29
            t in 1110..1319 -> DINNER       // 18:30 – 21:59
            t in 1320..1439 -> LIGHT_DINNER // 22:00 – 23:59
            t in 0..59      -> LIGHT_DINNER // 00:00 – 00:59
            else            -> SNACK
        }
    }

    /** Farsi label for the current bucket. */
    fun currentLabel(): String = labelFor(currentBucket())

    fun labelFor(bucket: String): String = when (bucket) {
        BREAKFAST     -> BREAKFAST_FA
        LUNCH         -> LUNCH_FA
        DINNER        -> DINNER_FA
        SNACK         -> SNACK_FA
        LIGHT_DINNER  -> LIGHT_DINNER_FA
        else          -> SNACK_FA
    }
}
