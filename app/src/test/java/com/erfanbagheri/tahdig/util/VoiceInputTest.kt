package com.erfanbagheri.tahdig.util

import android.content.Intent
import android.speech.RecognizerIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Covers the transcript cleanup, which is the part that can silently corrupt the field. */
class VoiceInputTest {

    private fun result(vararg texts: String) = texts.toList()

    @Test
    fun `takes the first transcript`() {
        assertEquals("پیاز", VoiceInput.parse(result("پیاز", "پیاز و")))
    }

    @Test
    fun `strips the trailing full stop recognisers add`() {
        assertEquals("گوشت گوسفند", VoiceInput.parse(result("گوشت گوسفند.")))
    }

    @Test
    fun `strips a trailing farsi comma`() {
        assertEquals("رب انار", VoiceInput.parse(result("رب انار،")))
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertEquals("مرغ", VoiceInput.parse(result("   مرغ  ")))
    }

    @Test
    fun `blank transcript is treated as nothing`() {
        assertNull(VoiceInput.parse(result("   ")))
        assertNull(VoiceInput.parse(result()))
        assertNull(VoiceInput.parse(null))
    }

    @Test
    fun `appends to an empty field without a separator`() {
        assertEquals("پیاز", VoiceInput.append("", "پیاز"))
    }

    @Test
    fun `appends with a farsi comma separator`() {
        assertEquals("پیاز، مرغ", VoiceInput.append("پیاز", "مرغ"))
    }

    @Test
    fun `appending to a field ending in a comma does not double it`() {
        assertEquals("پیاز، مرغ", VoiceInput.append("پیاز،", "مرغ"))
    }
}
