package com.erfanbagheri.tahdig.util

import androidx.compose.ui.graphics.Color

/** Maps category_id → (emoji, accent color). Used for visual hero cards. */
object FoodVisuals {
    private data class Visual(val emoji: String, val accent: Color)

    private val visuals = mapOf(
        1  to Visual("🍲", Color(0xFF8B5A00)),   // خورش
        2  to Visual("🍚", Color(0xFF5D4037)),   // پلو
        3  to Visual("🍢", Color(0xFFBF360C)),   // کباب
        4  to Visual("🥣", Color(0xFF6D4C41)),   // آش
        5  to Visual("🍜", Color(0xFFD84315)),   // سوپ
        6  to Visual("🍳", Color(0xFFF9A825)),   // کوکو
        7  to Visual("🥚", Color(0xFFFFCA28)),   // املت
        8  to Visual("🥬", Color(0xFF558B2F)),   // دلمه
        9  to Visual("🍛", Color(0xFFA1887F)),   // آبگوشت
        10 to Visual("🐟", Color(0xFF0277BD)),   // ماهی
        11 to Visual("🍝", Color(0xFFEF6C00)),   // پاستا
        12 to Visual("🍕", Color(0xFFC62828)),   // پیتزا
        13 to Visual("🍔", Color(0xFFE65100)),   // ساندویچ
        14 to Visual("🥗", Color(0xFF2E7D32)),   // سالاد
        15 to Visual("🫓", Color(0xFF8D6E63)),   // پیش‌غذا
        16 to Visual("🥖", Color(0xFFD7CCC8)),   // نان
        17 to Visual("🧀", Color(0xFFFFCA28)),   // پنیر
        18 to Visual("🍯", Color(0xFFF57F17)),   // صبحانه
        19 to Visual("🍮", Color(0xFFAD1457)),   // حلوا
        20 to Visual("🍪", Color(0xFFA1887F)),   // شیرینی
        21 to Visual("🍨", Color(0xFF4FC3F7)),   // بستنی
        22 to Visual("🥜", Color(0xFF795548)),   // خشکبار
        23 to Visual("🥤", Color(0xFF00ACC1)),   // نوشیدنی
        24 to Visual("🍉", Color(0xFF43A047)),   // میوه
        25 to Visual("🍽️", Color(0xFF6D4C41)),  // خوراک
        26 to Visual("🌍", Color(0xFF1565C0)),   // بین‌المللی
        27 to Visual("🧒", Color(0xFF7B1FA2)),   // کودک
    )

    private val defaultVisual = Visual("🍽️", Color(0xFF5D4037))

    fun emoji(categoryId: Long): String = visuals[categoryId.toInt()]?.emoji ?: defaultVisual.emoji
    fun accent(categoryId: Long): Color = visuals[categoryId.toInt()]?.accent ?: defaultVisual.accent
}
