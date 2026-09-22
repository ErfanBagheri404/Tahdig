package com.erfanbagheri.tahdig.util

/**
 * Scales the leading quantity token of each ingredient line by [factor].
 * Handles ASCII digits, Persian digits, ASCII fractions (1/2), and fraction glyphs (¼ ½ ¾).
 * Result always shows a number followed by one space, e.g. "6 قاشق برنج".
 */
object ServingScaler {

    /** Batch multiplier presets (#103): halve up to triple a whole recipe. */
    val BATCHES = listOf(0.5, 1.0, 1.5, 2.0, 3.0)

    private val GLYPHS = mapOf('¼' to 0.25, '½' to 0.5, '¾' to 0.75)

    private val NUMBER = Regex("""^([۰-۹]+|\d+(?:[.,/]\d+)?|[¼½¾])""")

    fun scale(ingredients: String, factor: Double): String =
        ingredients.lineSequence().joinToString("\n") { line -> scaleLine(line, factor) }

    /**
     * Servings × batch multiplier compose MULTIPLICATIVELY (#103 AC: 2× servings
     * + 1.5× batch → 4.5×). Single source of truth for every scaled surface.
     */
    fun composed(servings: Int, batch: Double): Double = servings * batch

    /**
     * Scale every comma/«،»/newline-separated ingredient of a blob — seed rows
     * are often one long comma line, where [scale] would only hit the first
     * quantity. Output joins with «، » (the shopping split accepts it).
     */
    fun scaleAll(ingredients: String, factor: Double): String {
        if (factor == 1.0) return ingredients
        return ingredients.split(',', '،', '\n')
            .map { scale(it.trim(), factor) }
            .filter { it.isNotBlank() }
            .joinToString("، ")
    }

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
