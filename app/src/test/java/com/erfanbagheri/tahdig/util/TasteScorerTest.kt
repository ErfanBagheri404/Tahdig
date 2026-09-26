package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #92 — the taste scorer is pure arithmetic precisely so this can assert the
 * weighting without a database, a clock, or a device.
 */
class TasteScorerTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun `a five star rating outranks a three star rating`() {
        val five = TasteScorer.Signals(stars = 5, ratedAt = now)
        val three = TasteScorer.Signals(stars = 3, ratedAt = now)
        assertTrue(TasteScorer.score(five, now) > TasteScorer.score(three, now))
    }

    @Test
    fun `a favorite outranks an unrated dish`() {
        val fav = TasteScorer.Signals(favorited = true)
        assertTrue(TasteScorer.score(fav, now) > TasteScorer.score(TasteScorer.Signals(), now))
    }

    @Test
    fun `having cooked a dish counts for it`() {
        val cooked = TasteScorer.Signals(cookedCount = 3)
        assertTrue(TasteScorer.score(cooked, now) > TasteScorer.score(TasteScorer.Signals(), now))
    }

    /** The issue's own wording: "they *and their category* rise". */
    @Test
    fun `a loved category lifts a dish the user never rated`() {
        val inLoved = TasteScorer.Signals(categoryStars = 4.7f)
        val inPlain = TasteScorer.Signals(categoryStars = 3.0f)
        assertTrue(TasteScorer.score(inLoved, now) > TasteScorer.score(inPlain, now))
    }

    /** First-run chip picker seed (#92): a ticked category lifts its dishes. */
    @Test
    fun `a ticked category lifts a dish the user has never touched`() {
        val ticked = TasteScorer.Signals(preferredCategory = true)
        val plain = TasteScorer.Signals()
        assertTrue(TasteScorer.score(ticked, now) > TasteScorer.score(plain, now))
    }

    /** A guess made before tasting anything must lose to a real rating. */
    @Test
    fun `a real rating outweighs the first-run guess`() {
        val guess = TasteScorer.Signals(preferredCategory = true)
        val realHate = TasteScorer.Signals(stars = 1, ratedAt = now)
        assertTrue(TasteScorer.score(realHate, now) < TasteScorer.score(guess, now))
    }

    @Test
    fun `one star is treated as a dislike, not a weak like`() {
        val hate = TasteScorer.Signals(stars = 1, ratedAt = now)
        assertTrue(TasteScorer.score(hate, now) < TasteScorer.score(TasteScorer.Signals(), now))
    }

    @Test
    fun `a hidden dish falls below a neutral dish`() {
        val hid = TasteScorer.Signals(disliked = true)
        assertTrue(TasteScorer.score(hid, now) < TasteScorer.score(TasteScorer.Signals(), now))
    }

    @Test
    fun `an old rating counts for less than a fresh one`() {
        val old = TasteScorer.Signals(stars = 5, ratedAt = now - 120 * day)
        val fresh = TasteScorer.Signals(stars = 5, ratedAt = now)
        assertTrue(TasteScorer.score(fresh, now) > TasteScorer.score(old, now))
    }

    @Test
    fun `a zero timestamp does not erase a real opinion`() {
        // null means "written before the column existed". Treating it as ancient
        // would decay the rating to nothing and silently discard real data.
        val s = TasteScorer.Signals(stars = 5, ratedAt = null)
        assertTrue(TasteScorer.score(s, now) > TasteScorer.score(TasteScorer.Signals(), now))
    }

    @Test
    fun `pool excludes a disliked dish`() {
        assertEquals(false, TasteScorer.pool(disliked = true))
        assertTrue(TasteScorer.pool(disliked = false))
    }

    @Test
    fun `weighted pick favours the better scoring dish`() {
        var first = 0
        repeat(400) {
            if (TasteScorer.weightedPick(listOf(1L, 2L)) { if (it == 2L) 100.0 else 0.0 } == 2L) first++
        }
        assertTrue("better dish won only $first/400", first > 320)
    }

    @Test
    fun `weighted pick is not deterministic`() {
        val seen = (1..40).map { TasteScorer.weightedPick(listOf(1L, 2L)) { 1.0 } }.toSet()
        assertTrue("always picked the same dish: $seen", seen.size > 1)
    }

    @Test
    fun `weighted pick returns null when nothing is eligible`() {
        assertNull(TasteScorer.weightedPick(emptyList<Long>()) { 1.0 })
    }

    @Test
    fun `scores are finite for a dish with no signals at all`() {
        assertTrue(TasteScorer.score(TasteScorer.Signals(), now).isFinite())
    }

    @Test
    fun `the best possible dish beats the worst`() {
        val best = TasteScorer.Signals(stars = 5, ratedAt = now, cookedCount = 9, favorited = true, categoryStars = 5f)
        val worst = TasteScorer.Signals(stars = 1, ratedAt = now, disliked = true)
        assertTrue(TasteScorer.score(best, now) > TasteScorer.score(worst, now))
    }

    @Test
    fun `decay costs over months, never crosses neutral`() {
        val old = TasteScorer.Signals(stars = 5, ratedAt = now - 120 * day)
        val fresh = TasteScorer.Signals(stars = 5, ratedAt = now)
        assertTrue(TasteScorer.score(fresh, now) > TasteScorer.score(old, now))
        assertTrue(TasteScorer.score(old, now) >= 1.0)
    }
}
