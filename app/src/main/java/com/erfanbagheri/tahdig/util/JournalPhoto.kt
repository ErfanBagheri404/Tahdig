package com.erfanbagheri.tahdig.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Journal photo capture (#124): decode → downscale → JPEG bytes.
 *
 * Photos are stored in the DB row, so they must be small — a 12 MP camera
 * shot would bloat every backup by megabytes. [targetSize] picks the scale
 * factor from the *decoded bounds only* (inJustDecodeBounds), so a huge image
 * is never fully decoded just to be thrown away.
 */
object JournalPhoto {

    /** Long edge of the stored image. Enough for a phone-sized journal card. */
    const val MAX_EDGE = 720

    /** JPEG quality — 80 is the usual "no visible artifacts at this size" point. */
    const val QUALITY = 80

    /**
     * Power-of-two downscale factor for [sourceEdge] to fit [maxEdge].
     * Always ≥ 1: BitmapFactory rejects factors below 1, and upscaling a small
     * image here would only cost bytes for no detail.
     */
    fun sampleSize(sourceEdge: Int, maxEdge: Int = MAX_EDGE): Int {
        if (sourceEdge <= 0 || maxEdge <= 0) return 1
        var sample = 1
        while (sourceEdge / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    /** Scaled dimensions preserving aspect ratio, long edge ≤ [maxEdge]. */
    fun scaledSize(width: Int, height: Int, maxEdge: Int = MAX_EDGE): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return 1 to 1
        val longest = max(width, height)
        if (longest <= maxEdge) return width to height
        val ratio = maxEdge.toFloat() / longest
        return max(1, (width * ratio).roundToInt()) to max(1, (height * ratio).roundToInt())
    }

    /** Read [uri] and return JPEG bytes, or null when the image can't be read. */
    fun read(context: Context, uri: Uri): ByteArray? = runCatching {
        // Pass 1: bounds only — no pixels allocated.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Pass 2: decode at the sample factor, then scale the remainder.
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(max(bounds.outWidth, bounds.outHeight))
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val (w, h) = scaledSize(decoded.width, decoded.height)
        val scaled = if (w == decoded.width && h == decoded.height) {
            decoded
        } else {
            Bitmap.createScaledBitmap(decoded, w, h, true).also {
                if (it !== decoded) decoded.recycle()
            }
        }

        ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            scaled.recycle()
            out.toByteArray()
        }
    }.getOrNull()
}
