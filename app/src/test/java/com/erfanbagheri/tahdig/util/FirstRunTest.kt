package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunTest {

    // --- starter chips (#126 AC: bundled, offline, no history) ---

    @Test fun `starter chips are non-empty and farsi-only`() {
        assertTrue(FirstRun.starterChips.size >= 6)
        assertTrue(FirstRun.starterChips.all { it.isNotBlank() })
        // No Latin letters — a Farsi install must not show English chips.
        assertTrue(FirstRun.starterChips.none { it.any { c -> c in 'a'..'z' || c in 'A'..'Z' } })
    }

    @Test fun `starter chips are unique`() {
        assertEquals(FirstRun.starterChips.size, FirstRun.starterChips.toSet().size)
    }

    @Test fun `starter chips carry no literal space between compound words`() {
        // SQLite matches a raw substring: «زرشک پلو» (space) finds nothing
        // while the seed spells it «زرشک‌پلو» (ZWNJ). A space in a starter
        // chip is a silent dead end, so no starter may contain one.
        assertTrue(FirstRun.starterChips.none { ' ' in it })
    }

    @Test fun `starter chips preserve zwnj in compound names`() {
        assertEquals("قورمه‌سبزی", FirstRun.starterChips.first()) // contains U+200C
        assertTrue(FirstRun.starterChips.first().contains('‌'))
    }

    // --- relaxed query (zero-result recovery) ---

    @Test fun `relaxed drops the overly specific last token`() {
        assertEquals("کباب زعفرانی", FirstRun.relaxed("کباب زعفرانی مخصوص"))
        assertEquals("کباب", FirstRun.relaxed("کباب زعفرانی"))
    }

    @Test fun `relaxed of a single token returns null`() {
        // Nothing to relax — offering the button would re-run the same search.
        assertNull(FirstRun.relaxed("کباب"))
        assertNull(FirstRun.relaxed("  "))
        assertNull(FirstRun.relaxed(""))
    }

    @Test fun `relaxed of three tokens keeps the first two`() {
        assertEquals("قورمه سبزی", FirstRun.relaxed("قورمه سبزی مخصوص"))
    }

    @Test fun `relaxed differs from the original query`() {
        val relaxed = FirstRun.relaxed("عدس پلو با گوشت")
        assertTrue(relaxed != null && relaxed != "عدس پلو با گوشت")
    }

    @Test fun `shouldOfferRelaxed only when zero results`() {
        assertTrue(FirstRun.shouldOfferRelaxed("کباب زعفرانی", 0))
        // Non-zero results: the button would be noise.
        assertFalse(FirstRun.shouldOfferRelaxed("کباب زعفرانی", 3))
        // Single token: relaxed() is null, so nothing to offer.
        assertFalse(FirstRun.shouldOfferRelaxed("کباب", 0))
    }

    @Test fun `relaxed preserves ascii query shapes`() {
        // A pasted English query must not crash the Farsi UI either.
        assertEquals("pasta", FirstRun.relaxed("pasta bake"))
        assertNull(FirstRun.relaxed("pasta"))
    }

    @Test fun `tip registry ids are unique`() {
        assertEquals(FirstRun.ALL_TIPS.size, FirstRun.ALL_TIPS.toSet().size)
    }

    // --- onboarding sample dish (deterministic, seeded ids) ---

    @Test fun `sample pick is stable for the same variation`() {
        // A random pick would re-roll on recomposition and flicker the row.
        assertEquals(FirstRun.samplePick(), FirstRun.samplePick())
        assertEquals(FirstRun.samplePick(2), FirstRun.samplePick(2))
    }

    @Test fun `sample pick cycles instead of throwing on out of range`() {
        val picks = (0..8).map { FirstRun.samplePick(it) }
        assertTrue(picks.all { it > 0 })
        // A negative or oversized variation must not crash onboarding.
        assertTrue(FirstRun.samplePick(-1) > 0)
        assertEquals(FirstRun.samplePick(0), FirstRun.samplePick(4))
    }

    @Test fun `sample pick ids are distinct`() {
        val all = (0..3).map { FirstRun.samplePick(it) }.toSet()
        assertEquals(4, all.size)
    }
}
