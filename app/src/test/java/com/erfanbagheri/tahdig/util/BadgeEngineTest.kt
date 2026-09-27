package com.erfanbagheri.tahdig.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Badge engine fixtures (#121): boundary counts, idempotence, coverage. */
class BadgeEngineTest {

    private val jan1 = LocalDate.of(2026, 1, 1)

    private fun cook(
        i: Long,
        day: LocalDate = jan1,
        hour: Int = 12,
        hasNote: Boolean = false,
    ) = BadgeEngine.Cook(
        foodId = i,
        at = day,
        hour = hour,
        hasNote = hasNote,
    )

    private fun dish(i: Long, cat: Int = 1, cuisine: String = "", prep: Int = 30) =
        BadgeEngine.DistinctDish(id = i, categoryId = cat, cuisine = cuisine, prepTimeMin = prep)

    @Test fun emptyHistoryUnlocksNothing() {
        assertTrue(BadgeEngine.evaluate(BadgeEngine.History()).isEmpty())
    }

    @Test fun firstCookUnlocksFirstBadge() {
        val h = BadgeEngine.History(cooks = listOf(cook(1)))
        val out = BadgeEngine.evaluate(h)
        assertTrue("first_cook" in out)
        assertFalse("cook_10" in out)
    }

    @Test fun reEvaluationIsIdempotent() {
        val h = BadgeEngine.History(
            cooks = (1..12).map { cook(it.toLong()) },
            distinctDishes = (1..12).map { dish(it.toLong()) },
        )
        val a = BadgeEngine.evaluate(h)
        val b = BadgeEngine.evaluate(h)
        assertEquals(a, b)
        assertTrue("cook_10" in a)
    }

    /** AC: a count of 9 must NOT unlock «cook_10» — the gate is >=, not >. */
    @Test fun boundaryCountsStayLockedBelowGoal() {
        val h = BadgeEngine.History(
            cooks = (1..9).map { cook(it.toLong()) },
            distinctDishes = (1..9).map { dish(it.toLong()) },
        )
        val out = BadgeEngine.evaluate(h)
        assertFalse("cook_10" in out)
        assertFalse("distinct_10" in out)
        assertTrue("first_cook" in out)
    }

    @Test fun repeatCooksDoNotCountAsDistinct() {
        // 12 cooks but only 2 dishes: cook_10 fires, distinct_10 must not.
        val h = BadgeEngine.History(
            cooks = (1..12).map { cook((it % 2).toLong()) },
            distinctDishes = listOf(dish(0), dish(1)),
            totalCategories = 27,
        )
        val out = BadgeEngine.evaluate(h)
        assertTrue("cook_10" in out)
        assertFalse("distinct_10" in out)
    }

    /** AC: cuisine coverage must read history, not metadata. */
    @Test fun cuisineCoverageIgnoresEmpty() {
        // All three dishes have no cuisine recorded: the region badge must
        // NOT fire — "unknown" is not a region visited.
        val h = BadgeEngine.History(
            cooks = (1..3).map { cook(it.toLong()) },
            distinctDishes = (1..3).map { dish(it.toLong(), cuisine = "") },
            totalCuisines = 0, // seed has no cuisine column → denominator 0
        )
        val out = BadgeEngine.evaluate(h)
        assertFalse("all_cuisine_1" in out)
    }

    @Test fun cuisineCoverageUnlocksWhenCovered() {
        val h = BadgeEngine.History(
            cooks = (1..4).map { cook(it.toLong()) },
            distinctDishes = listOf(
                dish(1, cuisine = "شمال"),
                dish(2, cuisine = "جنوب"),
                dish(3, cuisine = "مرکز"),
                dish(4, cuisine = "شمال"), // repeat must not help
            ),
            totalCuisines = 3,
        )
        assertTrue("all_cuisine_1" in BadgeEngine.evaluate(h))
    }

    /** AC: one count at the boundary — 3 of 3 unlocks, 2 of 3 stays locked. */
    @Test fun categoryBoundaries() {
        val three = BadgeEngine.History(
            cooks = listOf(cook(1)),
            distinctDishes = listOf(
                dish(1, cat = 1), dish(2, cat = 2), dish(3, cat = 3),
            ),
        )
        assertTrue("cat_3" in BadgeEngine.evaluate(three))

        val two = BadgeEngine.History(
            cooks = listOf(cook(1)),
            distinctDishes = listOf(dish(1, cat = 1), dish(2, cat = 2)),
        )
        assertFalse("cat_3" in BadgeEngine.evaluate(two))
    }

    @Test fun streakBoundaries() {
        val h = BadgeEngine.History(longestStreak = 30)
        val out = BadgeEngine.evaluate(h)
        assertTrue("streak_3" in out)
        assertTrue("streak_30" in out)
        assertFalse("streak_100" in out)
    }

    /**
     * AC: prep-time record — one 8-minute dish unlocks the 10- and 15-minute
     * tiers but NOT the 5-minute one.
     */
    @Test fun speedRecordTiers() {
        val h = BadgeEngine.History(
            cooks = listOf(cook(1)),
            distinctDishes = listOf(dish(1, prep = 8)),
        )
        val out = BadgeEngine.evaluate(h)
        assertTrue("speed_15" in out)
        assertTrue("speed_10" in out)
        assertFalse("speed_5" in out)
    }

    @Test fun zeroPrepTimeIsNotARecord() {
        // prep_time_min = 0 means "unknown", not "instant": no speed badge.
        val h = BadgeEngine.History(
            cooks = listOf(cook(1)),
            distinctDishes = listOf(dish(1, prep = 0)),
        )
        val out = BadgeEngine.evaluate(h)
        assertFalse("speed_15" in out)
        assertFalse("speed_10" in out)
        assertFalse("speed_5" in out)
    }

    @Test fun yaldaWindow() {
        val yalda = BadgeEngine.History(cooks = listOf(
            cook(1, LocalDate.of(2026, 12, 21)),
        ))
        assertTrue("yalda" in BadgeEngine.evaluate(yalda))

        val ordinary = BadgeEngine.History(cooks = listOf(
            cook(1, LocalDate.of(2026, 12, 19)),
        ))
        assertFalse("yalda" in BadgeEngine.evaluate(ordinary))
    }

    @Test fun nightShiftCountsOnly_1_to_4() {
        val h = BadgeEngine.History(
            cooks = (1..20).map { cook(it.toLong(), jan1, hour = 3) } +
                listOf(cook(99, jan1, hour = 0), cook(100, jan1, hour = 5)),
        )
        val out = BadgeEngine.evaluate(h)
        assertTrue("night_20" in out)
        assertFalse("night_50" in out)
    }

    @Test fun freezeMonthNeedsContiguity() {
        val contiguous = (0..30).map { cook(it.toLong(), jan1.plusDays(it.toLong())) }
        assertTrue(
            "no_freeze_month" in BadgeEngine.evaluate(
                BadgeEngine.History(cooks = contiguous, freezesGranted = 0),
            ),
        )
        // A 31-cook history with a hole on day 15 must not unlock.
        val holed = (0..30).filter { it != 15 }
            .map { cook(it.toLong(), jan1.plusDays(it.toLong())) }
        assertFalse(
            "no_freeze_month" in BadgeEngine.evaluate(
                BadgeEngine.History(cooks = holed, freezesGranted = 0),
            ),
        )
    }

    @Test fun notesCountNotRows() {
        // 12 journal ROWS but only 9 with notes: writer stays locked.
        val h = BadgeEngine.History(
            cooks = (1..12).map { cook(it.toLong(), hasNote = it <= 9) },
        )
        assertFalse("journal_10" in BadgeEngine.evaluate(h))
    }

    @Test fun waterStreakBadge() {
        assertTrue(
            "water_7" in BadgeEngine.evaluate(
                BadgeEngine.History(waterGoalDayStreak = 7),
            ),
        )
        assertFalse(
            "water_7" in BadgeEngine.evaluate(
                BadgeEngine.History(waterGoalDayStreak = 6),
            ),
        )
    }

    @Test fun statesMirrorUnlocks() {
        val h = BadgeEngine.History(
            cooks = (1..5).map { cook(it.toLong()) },
            distinctDishes = (1..3).map { dish(it.toLong(), cat = it.toInt()) },
            totalCategories = 27,
        )
        val map = BadgeEngine.states(h).associateBy { it.def.id }
        assertTrue(map.getValue("first_cook").unlocked)
        // Progress is the raw counter (5 cooks), goal 1 — the «۵ از ۱» line
        // reads as over-achieved, which is exactly right for this badge.
        assertEquals(5, map.getValue("first_cook").progress)
        assertEquals(1, map.getValue("first_cook").goal)
        assertFalse(map.getValue("cook_10").unlocked)
        assertEquals(5, map.getValue("cook_10").progress)
        assertTrue(map.getValue("cat_3").unlocked)
        // cat_all shows the real denominator, not a placeholder.
        assertEquals(27, map.getValue("cat_all").goal)
        assertFalse("همه «{n}»" in map.getValue("cat_all").def.requirement)
    }

    @Test fun newlyUnlockedDiffsSets() {
        val before = setOf("first_cook")
        val after = setOf("first_cook", "cook_10")
        val fresh = BadgeEngine.newlyUnlocked(before, after)
        assertEquals(1, fresh.size)
        assertEquals("cook_10", fresh[0].id)
    }
}
