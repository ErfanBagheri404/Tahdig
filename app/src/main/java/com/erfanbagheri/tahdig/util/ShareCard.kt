package com.erfanbagheri.tahdig.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.compose.ui.graphics.toArgb
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.ui.screen.splitSteps
import java.io.File

/**
 * Which card to draw (#132). Same canvas, three readings of the same dish:
 * the standard card leads with the name, the text card drops the photo block
 * for ingredients, and the stats card leads with time/difficulty/nutrition.
 *
 * @param label the Farsi name shown in the share sheet.
 */
enum class ShareLayout(val label: String) {
    /** Name, photo block, ingredients — the original. */
    STANDARD("استاندارد"),

    /** No photo block: full height goes to ingredients and steps. */
    NO_PHOTO("بدون عکس"),

    /** Difficulty/time/tags first — for people deciding, not cooking. */
    STATS("آماری"),
}

/**
 * Renders a dish as a styled bitmap card and shares it as a PNG.
 *
 * Canvas-based — no Compose needed, works off the UI thread.
 * ponytail: text-only card; photos need async Coil load first. Add a real photo
 *   when the share action has a loaded Coil bitmap to hand — the layout enum
 *   already reserves the space.
 */
object ShareCard {
    private const val W = 1080
    private const val H = 1350

    fun share(context: Context, food: FoodEntity, layout: ShareLayout = ShareLayout.STANDARD) {
        val file = render(context, food, layout)
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText(food))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "اشتراک‌گذاری غذا"))
    }

    /** The text part — sent alongside the image so the caption still works. */
    fun shareText(food: FoodEntity): String = buildString {
        appendLine("🍽 ${food.name}")
        if (food.nameEn.isNotBlank()) appendLine("(${food.nameEn})")
        appendLine()
        if (food.ingredients.isNotBlank()) {
            appendLine("مواد لازم:")
            appendLine(food.ingredients)
            appendLine()
        }
        if (food.description.isNotBlank()) {
            appendLine(food.description)
            appendLine()
        }
        append("پیشنهاد از اپلیکیشن ته‌دیگ 🍚")
    }

    internal fun render(context: Context, food: FoodEntity, layout: ShareLayout): File {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Background
        c.drawColor(Color.rgb(18, 18, 18))

        // Accent bar
        val accentPaint = Paint().apply {
            // #128: the share card is a plain canvas, so it reads the store
            // directly — there is no composition to hang a local on here.
            color = FoodVisuals.accent(food.categoryId, SettingsStore.accentHex.value).toArgb()
            style = Paint.Style.FILL
        }
        c.drawRect(0f, 0f, W.toFloat(), 16f, accentPaint)

        val nameP = tp(64f, Typeface.DEFAULT_BOLD, Color.WHITE)
        c.drawText(food.name, 60f, 220f, nameP)

        var y = 280f
        if (food.nameEn.isNotBlank()) {
            c.drawText(food.nameEn, 60f, y, tp(36f, Typeface.DEFAULT, Color.GRAY))
            y += 100f
        } else {
            y += 80f
        }

        // The stats card leads with the numbers; the other two lead with pills.
        if (layout == ShareLayout.STATS) {
            y = statsBlock(c, food, y, accentPaint.color)
        } else {
            y = pillRow(c, food.prepTimeMin, food.difficulty, y, accentPaint.color)
        }
        y += 40f

        // Ingredients
        if (food.ingredients.isNotBlank()) {
            c.drawText("مواد لازم:", 60f, y, tp(44f, Typeface.DEFAULT_BOLD, Color.LTGRAY))
            y += 70f
            val bullet = tp(40f, Typeface.DEFAULT, Color.LTGRAY)
            val ings = food.ingredients.split('،', ',').map { it.trim() }.filter { it.isNotEmpty() }
            for (ing in ings) {
                c.drawText("• $ing", 90f, y, bullet)
                y += 56f
                if (y > H - 240) break
            }
            y += 20f
        }

        // Steps. The photo block owns this space in the standard card; the
        // text and stats cards give it to the recipe itself.
        if (layout != ShareLayout.STANDARD && food.description.isNotBlank()) {
            val steps = splitSteps(food.description)
            c.drawText("مراحل:", 60f, y, tp(44f, Typeface.DEFAULT_BOLD, Color.LTGRAY))
            y += 70f
            val body = tp(36f, Typeface.DEFAULT, Color.LTGRAY)
            for ((i, step) in steps.withIndex()) {
                if (y > H - 240) break
                c.drawText("${i + 1}. $step", 90f, y, body)
                y += 52f
            }
            y += 20f
        }

        // Description — the standard card has no steps, so it still needs this.
        if (layout == ShareLayout.STANDARD && food.description.isNotBlank() && y < H - 320f) {
            y = wrap(c, food.description, tp(38f, Typeface.DEFAULT, Color.LTGRAY), 60f, y, W - 120)
        }

        // Footer
        c.drawText("پیشنهاد از ته‌دیگ 🍚", 60f, H - 60f, tp(32f, Typeface.DEFAULT, Color.GRAY))

        val out = File(File(context.cacheDir, "share"), "tahdig-${food.id}-${layout.name.lowercase()}.png")
        out.parentFile?.mkdirs()
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return out
    }

    /**
     * Stats-first header for [ShareLayout.STATS]: calories and macros in a stat
     * row, then the time/difficulty pills. Nutrition is an *estimate* derived
     * from the dish name, so the card labels it as one rather than printing a
     * number that reads as measured.
     */
    private fun statsBlock(c: Canvas, food: FoodEntity, y0: Float, accent: Int): Float {
        var y = y0
        val n = NutritionEstimate.estimate(food.name, food.tags)
        val head = tp(40f, Typeface.DEFAULT_BOLD, Color.WHITE)
        val sub = tp(34f, Typeface.DEFAULT, Color.GRAY)
        c.drawText("انرژی", 60f, y, sub)
        c.drawText("${n.calories} کیلوکالری", 60f, y + 60f, head)
        val note = "برآوردی، بر پایهٔ نوع غذا"
        c.drawText(note, W - 60f - sub.measureText(note), y + 60f, tp(28f, Typeface.DEFAULT, Color.GRAY))
        y += 130f
        c.drawText("پروتئین ${n.protein} • چربی ${n.fat} • کربوهیدرات ${n.carb}", 60f, y, sub)
        y += 100f
        return pillRow(c, food.prepTimeMin, food.difficulty, y, accent)
    }

    private fun pillRow(c: Canvas, prep: Int, diff: String, y: Float, accent: Int): Float {
        val pillBg = Paint().apply { color = Color.rgb(42, 42, 42) }
        val pillTxt = tp(32f, Typeface.DEFAULT, Color.LTGRAY)
        var x = 60f
        for (text in listOf("${prep} دقیقه", difficultyLabel(diff))) {
            val w = pillTxt.measureText(text) + 40f
            c.drawRoundRect(RectF(x, y - 40f, x + w, y + 56f), 28f, 28f, pillBg)
            c.drawText(text, x + 20f, y + 38f, pillTxt)
            x += w + 20f
        }
        return y + 120f
    }

    private fun difficultyLabel(d: String) = when (d.uppercase()) {
        "EASY" -> "آسان"; "MEDIUM" -> "متوسط"; "HARD" -> "سخت"; else -> d
    }

    private fun wrap(c: Canvas, text: String, p: Paint, x: Float, startY: Float, maxW: Int): Float {
        val words = text.split(' ')
        var y = startY; var line = StringBuilder()
        for (w in words) {
            val c2 = if (line.isEmpty()) w else "$line $w"
            if (p.measureText(c2) > maxW && line.isNotEmpty()) {
                c.drawText(line.toString(), x, y, p); y += 52f
                line = StringBuilder(w)
            } else { if (line.isNotEmpty()) line.append(' '); line.append(w) }
        }
        if (line.isNotEmpty()) c.drawText(line.toString(), x, y, p)
        return y
    }

    private fun tp(size: Float, typeface: Typeface, color: Int) = Paint().apply {
        this.color = color; textSize = size; isAntiAlias = true; this.typeface = typeface
    }
}
