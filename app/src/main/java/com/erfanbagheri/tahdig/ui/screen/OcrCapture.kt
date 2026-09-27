package com.erfanbagheri.tahdig.ui.screen

import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.erfanbagheri.tahdig.util.OcrConfidence
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * OCR capture for recipe cards (#76) as a launcher factory.
 *
 * Gallery pick rather than camera: `GetContent` needs no runtime permission, so
 * this ships without touching the permission surface — swap in
 * `TakePicturePreview` when the camera permission lands and nothing else here
 * changes.
 *
 * Recognition is ML Kit's **bundled** on-device model (not the Play-services
 * download), so a card can be scanned with no network, no API key, and no bytes
 * leaving the phone — the same offline-first guarantee the rest of the app makes.
 *
 * The caller gets three things and nothing else: the raw text (to parse), the
 * flagged low-confidence lines (to warn about), and the photo URI (to keep as
 * the card's «کارت اصلی» provenance image). No DB access happens here.
 */
@Composable
fun rememberOcrLauncher(
    /** (rawText, lowConfidenceLines, photoUri) */
    onRecognized: (String, List<String>, Uri) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bitmap: Bitmap = runCatching {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }.getOrNull() ?: return@rememberLauncherForActivityResult

        val task = runCatching {
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
        }.getOrNull() ?: return@rememberLauncherForActivityResult

        task.addOnSuccessListener { text ->
            val lines = text.textBlocks.flatMap { block ->
                block.lines.map { line ->
                    OcrConfidence.OcrLine(
                        text = line.text,
                        confidences = line.elements.flatMap { el ->
                            el.symbols.map { sym -> sym.confidence.takeIf { it >= 0 } }
                        },
                    )
                }
            }
            onRecognized(
                lines.joinToString("\n") { it.text },
                OcrConfidence.lowConfidenceLines(lines),
                uri,
            )
        }
    }

    return { picker.launch("image/*") }
}
