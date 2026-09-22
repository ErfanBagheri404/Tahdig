package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.util.CookVoiceCommands.Command
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * #94 acceptance: alias matching + PersianText normalization edge cases
 * (ZWNJ, Arabic letters, recognizer padding), garbage → null (no state change).
 */
class CookVoiceCommandsTest {

    // ── Basic aliases ─────────────────────────────────────────────
    @Test fun `next aliases`() {
        assertEquals(Command.NEXT, CookVoiceCommands.match("بعدی"))
        assertEquals(Command.NEXT, CookVoiceCommands.match("مرحله بعد"))
        assertEquals(Command.NEXT, CookVoiceCommands.match("قدم بعد"))
    }

    @Test fun `previous aliases`() {
        assertEquals(Command.PREVIOUS, CookVoiceCommands.match("قبلی"))
        assertEquals(Command.PREVIOUS, CookVoiceCommands.match("مرحله قبل"))
        assertEquals(Command.PREVIOUS, CookVoiceCommands.match("مرحله قبلی"))
    }

    @Test fun `timer stop repeat done aliases`() {
        assertEquals(Command.TIMER, CookVoiceCommands.match("تایمر"))
        assertEquals(Command.STOP, CookVoiceCommands.match("متوقف"))
        assertEquals(Command.STOP, CookVoiceCommands.match("توقف"))
        assertEquals(Command.REPEAT, CookVoiceCommands.match("تکرار"))
        assertEquals(Command.DONE, CookVoiceCommands.match("تمام شد"))
    }

    // ── Normalization edge cases (AC: ZWNJ, Arabic letters) ───────
    @Test fun `ZWNJ does not break a two-word command`() {
        // «مرحله‌بعد» with a ZWNJ instead of the space must still match NEXT.
        assertEquals(Command.NEXT, CookVoiceCommands.match("مرحله‌بعد"))
        // «تمام‌شد» likewise.
        assertEquals(Command.DONE, CookVoiceCommands.match("تمام‌شد"))
    }

    @Test fun `Arabic Yeh and Kaf fold to Persian forms`() {
        // Arabic Yeh ي in «بَعْدي»-style recognizer output → ی.
        assertEquals(Command.NEXT, CookVoiceCommands.match("بعدي"))
        // Arabic Kaf ك in «توقف»-shaped output → ک.
        assertEquals(Command.STOP, CookVoiceCommands.match("توقف"))
        // Arabic Kaf in «تكرار».
        assertEquals(Command.REPEAT, CookVoiceCommands.match("تكرار"))
    }

    @Test fun `diacritics and tatweel are ignored`() {
        assertEquals(Command.NEXT, CookVoiceCommands.match("بَعْدِي"))
        assertEquals(Command.TIMER, CookVoiceCommands.match("تَـايمر"))
    }

    // ── Multi-word beats first-token (alias table order) ──────────
    @Test fun `multi-word alias wins over its first token`() {
        // «مرحله قبلی» must be PREVIOUS, not fall through to something else.
        assertEquals(Command.PREVIOUS, CookVoiceCommands.match("مرحله قبلی"))
        // «شروع تایمر» is still TIMER (exact match).
        assertEquals(Command.TIMER, CookVoiceCommands.match("شروع تایمر"))
    }

    // ── Prefix padding (recognizers append polite words) ───────────
    @Test fun `command followed by padding still matches`() {
        assertEquals(Command.NEXT, CookVoiceCommands.match("بعدی لطفا"))
        assertEquals(Command.DONE, CookVoiceCommands.match("تمام شد دیگه"))
    }

    // ── Garbage → null, no state change (AC) ──────────────────────
    @Test fun `garbage returns null`() {
        assertNull(CookVoiceCommands.match(null))
        assertNull(CookVoiceCommands.match(""))
        assertNull(CookVoiceCommands.match("   "))
        assertNull(CookVoiceCommands.match("چه غذایی بپزم"))
        assertNull(CookVoiceCommands.match("hello world"))
        assertNull(CookVoiceCommands.match("۱۲۳"))
    }

    @Test fun `substring of a word does not match`() {
        // «بعد» inside an unrelated longer word must not fire NEXT.
        assertNull(CookVoiceCommands.match("بعید"))
    }

    // ── Echo strings are Farsi and stable (UI feedback contract) ───
    @Test fun `echo text is Farsi`() {
        assertEquals("مرحله بعد", CookVoiceCommands.echoOf(Command.NEXT))
        assertEquals("متوجه نشدم", CookVoiceCommands.UNKNOWN_ECHO)
    }
}
