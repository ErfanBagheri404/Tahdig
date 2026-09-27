package com.erfanbagheri.tahdig.util

/**
 * Turns step text into linkable segments (#101): technique terms become tappable,
 * everything else stays plain.
 *
 * Matching uses the same Persian-letter boundary rule as [EquipmentInferrer] —
 * «در فر» must link in «در فر بگذارید» but NOT in «در فرش آشپزخانه» (no false
 * positives inside words; that is the acceptance criterion).
 */
object TechniqueLinker {

    /** One run of text: linked when [techniqueId] is non-null. */
    data class Segment(val text: String, val techniqueId: String?)

    /**
     * Split [text] at boundary-safe keyword hits of [techniques]. Matches are
     * non-overlapping; the earliest match wins, then scanning continues after it.
     */
    fun linkify(text: String, techniques: List<Technique>): List<Segment> {
        if (text.isEmpty() || techniques.isEmpty()) return listOf(Segment(text, null))

        // (start, length, techniqueId) of every real hit, earliest first.
        val hits = mutableListOf<Triple<Int, Int, String>>()
        for (tech in techniques) {
            for (kw in tech.keywords) {
                var from = 0
                val hay = text
                while (true) {
                    val i = hay.indexOf(kw, from)
                    if (i < 0) break
                    if (boundaryOk(hay, i, kw.length)) hits += Triple(i, kw.length, tech.id)
                    from = i + 1
                }
            }
        }
        hits.sortBy { it.first }

        val segments = mutableListOf<Segment>()
        var cursor = 0
        for ((start, len, techId) in hits) {
            if (start < cursor) continue // overlap: earlier hit already claimed it
            if (start > cursor) segments += Segment(text.substring(cursor, start), null)
            segments += Segment(text.substring(start, start + len), techId)
            cursor = start + len
        }
        if (cursor < text.length) segments += Segment(text.substring(cursor), null)
        return segments
    }

    /** Reverse-link check: does [text] mention [tech] as a whole word anywhere? */
    fun hasKeyword(text: String, tech: Technique): Boolean {
        for (kw in tech.keywords) {
            var from = 0
            while (true) {
                val i = text.indexOf(kw, from)
                if (i < 0) break
                if (boundaryOk(text, i, kw.length)) return true
                from = i + 1
            }
        }
        return false
    }

    /** Neighbors of a hit must not be letters — Persian comma/digits are not letters. */
    private fun boundaryOk(s: String, start: Int, length: Int): Boolean {
        val before = if (start == 0) null else s[start - 1]
        val end = start + length
        val after = if (end >= s.length) null else s[end]
        return !isLetter(before) && !isLetter(after)
    }

    private fun isLetter(c: Char?): Boolean =
        c != null && Character.isLetter(c)
}
