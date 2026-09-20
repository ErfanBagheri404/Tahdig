package com.erfanbagheri.tahdig.util

/** Scales leading quantities in each ingredient line by a factor. Handles "2", "۱", "2.5", "¼". */
object ServingScaler {
    private val TOKENS = mapOf("¼" to 0.25, "½" to 0.5, "¾" to 0.75, "۱" to 1.0, "۲" to 2.0, "۳" to 3.0,
        "۴" to 4.0, "۵" to 5.0, "۶" to 6.0, "۷" to 7.0, "۸" to 8.0, "۹" to 9.0, "۰" to 0.0)

    /** Multiply the first numeric token in each ingredient by [factor]; leaves non-numeric lines unchanged. */
    fun scale(ingredients: String, factor: Double): String =
        ingredients.lineSequence().joinToString("\n") { scaleLine(it, factor) }

    private fun scaleLine(line: String, factor: Double): String {
        val m = Regex("""^(\d+([.,/]\d+)?|\d*[¼½¾]|[۰-۹]+)""").find(line.trimStart()) ?: return line
        val token = m.value
        val scaled = parse(token) * factor
        val newTok = if (scaled == scaled.toInt().toDouble()) scaled.toInt().toString()
        else (Math.round(scaled * 10) / 10.0).toString()
        return newTok + line.substringAfter(token, "").let { r ->
            // preserve the space after the token
            if (line[m.range.last + 1].isWhitespace()) " $r" else r
        }
    }

    private fun parse(token: String): Double {
        if (token in TOKENS) return TOKENS[token]!!
        if (token.contains('/')) {
            val (a, b) = token.split('/')
            return a.toDoubleOrNull()?.div(b.toDoubleOrNull() ?: 1.0) ?: 1.0
        }
        return token.replace('.', '.').toDoubleOrNull() ?: 1.0
    }
}
