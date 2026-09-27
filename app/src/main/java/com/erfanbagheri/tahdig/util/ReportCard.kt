package com.erfanbagheri.tahdig.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File

/**
 * Shareable PNG of the weekly nutrition summary (#114).
 *
 * Same canvas pipeline as [ShareCard] — dark card, flat Paint text, no
 * chart library. It draws a [WeeklyReport.Summary] rather than a dish:
 * the average line, the macro split, the totals, then the top dishes.
 */
object ReportCard {

    private const val W = 1080
    private const val H = 1200

    fun share(
        context: Context,
        summary: WeeklyReport.Summary,
        top: List<Pair<String, Int>>,
    ) {
        val file = render(context, summary, top)
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file,
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText(summary, top))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "اشتراک‌گذاری گزارش هفتگی"))
    }

    /** The text part — sent alongside the image so the caption still works. */
    fun shareText(summary: WeeklyReport.Summary, top: List<Pair<String, Int>>): String = buildString {
        appendLine("📊 گزارش هفتگی ته‌دیگ")
        appendLine()
        appendLine("میانگین ${PersianText.toPersianDigits(summary.avgCalories.toDouble())} کیلوکالری در روز")
        appendLine("مجموع ${PersianText.toPersianDigits(summary.totalCalories.toDouble())} کیلوکالری در ${PersianText.toPersianDigits(summary.loggedDays.toDouble())} روز")
        appendLine()
        top.forEachIndexed { i, (name, count) ->
            appendLine("${PersianText.toPersianDigits((i + 1).toDouble())}. $name — ${PersianText.toPersianDigits(count.toDouble())} بار")
        }
        append("پیشنهاد از اپلیکیشن ته‌دیگ 🍚")
    }

    internal fun render(
        context: Context,
        summary: WeeklyReport.Summary,
        top: List<Pair<String, Int>>,
    ): File {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(18, 18, 18))

        val accentPaint = Paint().apply { color = Color.rgb(255, 179, 0); style = Paint.Style.FILL }
        c.drawRect(0f, 0f, W.toFloat(), 16f, accentPaint)
        c.drawText("📊 گزارش هفتگی", 60f, 220f, tp(64f, Typeface.DEFAULT_BOLD, Color.WHITE))

        var y = 340f
        val stat = tp(52f, Typeface.DEFAULT_BOLD, Color.WHITE)
        val sub = tp(40f, Typeface.DEFAULT, Color.LTGRAY)
        c.drawText("میانگین ${PersianText.toPersianDigits(summary.avgCalories.toDouble())} کیلوکالری در روز", 60f, y, stat)
        y += 90f
        c.drawText(
            "پروتئین ${PersianText.toPersianDigits(summary.avgProtein.toDouble())} · چربی ${PersianText.toPersianDigits(summary.avgFat.toDouble())} · کربوهیدرات ${PersianText.toPersianDigits(summary.avgCarbs.toDouble())}",
            60f, y, sub,
        )
        y += 90f
        c.drawText(
            "مجموع ${PersianText.toPersianDigits(summary.totalCalories.toDouble())} در ${PersianText.toPersianDigits(summary.loggedDays.toDouble())} روز",
            60f, y, sub,
        )
        y += 150f

        if (top.isNotEmpty()) {
            c.drawText("پرتکرارترین‌ها:", 60f, y, tp(44f, Typeface.DEFAULT_BOLD, Color.LTGRAY))
            y += 80f
            val row = tp(40f, Typeface.DEFAULT, Color.LTGRAY)
            for ((i, pair) in top.withIndex()) {
                c.drawText(
                    "• ${PersianText.toPersianDigits((i + 1).toDouble())} ${pair.first} — ${PersianText.toPersianDigits(pair.second.toDouble())} بار",
                    90f, y, row,
                )
                y += 60f
                if (y > H - 160) break
            }
        }

        c.drawText("پیشنهاد از ته‌دیگ 🍚", 60f, H - 60f, tp(32f, Typeface.DEFAULT, Color.GRAY))

        val out = File(File(context.cacheDir, "share"), "tahdig-weekly.png")
        out.parentFile?.mkdirs()
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return out
    }

    private fun tp(size: Float, typeface: Typeface, color: Int) = Paint().apply {
        this.color = color; textSize = size; isAntiAlias = true; this.typeface = typeface
    }
}
