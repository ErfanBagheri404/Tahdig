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
import java.io.File

/**
 * Renders a dish as a styled bitmap card and shares it as a PNG.
 *
 * Canvas-based — no Compose needed, works off the UI thread.
 * ponytail: text-only card; photos need async Coil load first.
 */
object ShareCard {
    private const val W = 1080
    private const val H = 1350

    fun share(context: Context, food: FoodEntity) {
        val file = render(context, food)
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

    internal fun render(context: Context, food: FoodEntity): File {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Background
        c.drawColor(Color.rgb(18, 18, 18))

        // Accent bar
        val accentPaint = Paint().apply {
            color = FoodVisuals.accent(food.categoryId).toArgb()
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

        // Pills
        y = pillRow(c, food.prepTimeMin, food.difficulty, y, accentPaint.color)
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

        // Description
        if (food.description.isNotBlank() && y < H - 320f) {
            y = wrap(c, food.description, tp(38f, Typeface.DEFAULT, Color.LTGRAY), 60f, y, W - 120)
        }

        // Footer
        c.drawText("پیشنهاد از ته‌دیگ 🍚", 60f, H - 60f, tp(32f, Typeface.DEFAULT, Color.GRAY))

        val out = File(File(context.cacheDir, "share"), "tahdig-${food.id}.png")
        out.parentFile?.mkdirs()
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return out
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
