package com.erfanbagheri.tahdig.util

/**
 * Scales the leading quantity token of each ingredient line by [factor].
 * Handles ASCII digits, Persian digits, ASCII fractions (1/2), and fraction glyphs (¼ ½ ¾).
 * Result always shows a number followed by one space, e.g. "6 قاشق برنج".
 */
object ServingScaler {
    private val GLYPHS = mapOf('¼' to 0.25, '½' to 0.5, '¾' to 0.75)

    private val NUMBER = Regex("""^([۰-۹]+|\d+(?:[.,/]\d+)?|[¼½¾])""")

    fun scale(ingredients: String, factor: Double): String =
        ingredients.lineSequence().joinToString("\n") { line -> scaleLine(line, factor) }

    private fun scaleLine(line: String, factor: Double): String {
        val trimmed = line.trimStart()
        val m = NUMBER.find(trimmed) ?: return line
        val token = m.value
        val value = parse(token) ?: return line
        val scaled = value * factor
        val scaledStr = format(scaled)
        // Everything after the token, dropping original whitespace, then re-added uniformly.
        val rest = trimmed.substring(token.length).trimStart()
        return if (rest.isEmpty()) scaledStr else "$scaledStr $rest"
    }

    private fun parse(token: String): Double? {
        if (token.length == 1 && token[0] in GLYPHS) return GLYPHS[token[0]]
        if (token.all { it in '۰'..'۹' }) return token.map { it.code - '۰'.code }.joinToString("").toDoubleOrNull()
        if (token.contains('/')) {
            val parts = token.split('/')
            val a = parts.getOrNull(0)?.toDoubleOrNull() ?: return null
            val b = parts.getOrNull(1)?.toDoubleOrNull() ?: return null
            return if (b == 0.0) null else a / b
        }
        return token.replace('٬', '.').toDoubleOrNull()
    }

    private fun format(value: Double): String =
        if (value == value.toInt().toDouble()) value.toInt().toString()
        else (Math.round(value * 10) / 10.0).toString()
}
