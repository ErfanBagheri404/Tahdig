package com.erfanbagheri.tahdig.util

/**
 * OCR confidence thresholding (#76).
 *
 * ML Kit text recognition returns a block tree whose lines carry no confidence
 * score directly — only symbols and (on-device) words do. This object maps
 * that tree output into what the review screen needs: for each recognized
 * line, the minimum word/symbol confidence, so the review can flag shaky lines
 * («fix me») while still showing the raw text editable.
 *
 * The input is a plain data stub, NOT ML Kit types, so this is unit-testable
 * without a device: callers collapse [com.google.mlkit.vision.text.Text]
 * into [OcrLine]s in the capture flow.
 */
object OcrConfidence {

    /** Lines at or below this confidence are flagged for manual review. */
    const val LOW_CONFIDENCE_THRESHOLD = 0.6f

    /** One recognized line + its word/symbol confidences (0..1, null = unknown). */
    data class OcrLine(
        val text: String,
        val confidences: List<Float?> = emptyList(),
    )

    /**
     * Lines whose effective confidence falls below [threshold].
     *
     * The returned string is the ORIGINAL line text, untrimmed: the review
     * screen flags these lines by matching the string it is displaying, so a
     * trimmed copy would flag nothing the user can find. Blank lines are
     * dropped rather than flagged — silence is not an error.
     */
    fun lowConfidenceLines(
        lines: List<OcrLine>,
        threshold: Float = LOW_CONFIDENCE_THRESHOLD,
    ): List<String> =
        lines.mapNotNull { line ->
            if (line.text.isBlank()) return@mapNotNull null
            // ponytail: min-of-words; upgrade to weighted avg when ML Kit v2
            // exposes per-word weights worth trusting.
            val worst = line.confidences.filterNotNull().minOrNull()
            // Strictly below: a word scoring exactly the threshold is trusted.
            if (worst != null && worst < threshold) line.text else null
        }

    /**
     * Effective confidence of one line: the minimum known score, or null when
     * no word carries one (treated as trusted — nothing to flag on).
     */
    fun confidenceOf(line: OcrLine): Float? =
        line.confidences.filterNotNull().minOrNull()
}
