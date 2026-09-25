package com.erfanbagheri.tahdig.widget

import com.erfanbagheri.tahdig.util.StreakMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WidgetLogicTest {

    @Test
    fun sizeClassificationBucketsCorrectly() {
        // 2x2 launcher cell ~ 110x40dp, 4x2 ~ 250x110, 4x4 ~ 250x250.
        assertEquals(WidgetLogic.Size.COMPACT, WidgetLogic.sizeFor(110, 40))
        assertEquals(WidgetLogic.Size.COMPACT, WidgetLogic.sizeFor(150, 90))
        assertEquals(WidgetLogic.Size.MEDIUM, WidgetLogic.sizeFor(250, 110))
        assertEquals(WidgetLogic.Size.MEDIUM, WidgetLogic.sizeFor(180, 240))
        assertEquals(WidgetLogic.Size.LARGE, WidgetLogic.sizeFor(250, 250))
        assertEquals(WidgetLogic.Size.LARGE, WidgetLogic.sizeFor(290, 150))
    }

    @Test
    fun twoByTwoNeverBecomesMedium() {
        // Regression: cell-based math put a 2x2 in MEDIUM, which renders the
        // shuffle button in a layout with no room for it.
        for (h in 40..120 step 10) {
            assertEquals(WidgetLogic.Size.COMPACT, WidgetLogic.sizeFor(110, h))
        }
    }

    @Test
    fun streakLineHidesAtZero() {
        assertNull(WidgetLogic.streakLine(0, false))
        assertNull(WidgetLogic.streakLine(-1, false))
    }

    @Test
    fun streakLineFormatsWithPersianDigits() {
        assertEquals("🔥 ۱۲ روز", WidgetLogic.streakLine(12, false))
        assertEquals("🔥 ۱ روز", WidgetLogic.streakLine(1, false))
    }

    @Test
    fun streakLineShowsFreezeTag() {
        assertEquals("🔥 ۵ روز (بیفریز)", WidgetLogic.streakLine(5, true))
    }

    @Test
    fun streakLineFromStateMatches() {
        val today = LocalDate.of(2026, 9, 23)
        val state = StreakMath.State(
            current = 7,
            longest = 10,
            freezesLeft = 1,
            thisWeek = 2,
            weeklyFloor = 3,
            frozenDay = today.minusDays(1),
        )
        assertEquals("🔥 ۷ روز (بیفریز)", WidgetLogic.streakLine(state))
    }

    @Test
    fun shuffleIndexCyclesWithinBounds() {
        assertEquals(0, WidgetLogic.shuffledIndex(0, 10))
        assertEquals(7, WidgetLogic.shuffledIndex(7, 10))
        assertEquals(3, WidgetLogic.shuffledIndex(13, 10))
        assertEquals(0, WidgetLogic.shuffledIndex(0, 0))
    }

    @Test
    fun dishTapAlwaysRoutesToDetail() {
        assertEquals(WidgetLogic.Action.OPEN_DISH, WidgetLogic.routeForDish(42L))
        assertEquals(WidgetLogic.Action.OPEN_DISH, WidgetLogic.routeForDish(-1L))
    }

    @Test
    fun shuffleNeverReturnsTheSameDishTwiceInARow() {
        // Consecutive spins must advance, otherwise the button looks broken.
        val count = 7
        val a = WidgetLogic.shuffledIndex(0, count)
        val b = WidgetLogic.shuffledIndex(1, count)
        assert(a != b) { "spin 0 and 1 both landed on $a" }
    }

    @Test
    fun listTitlesDropsNullsAndCapsAtThree() {
        val out = WidgetLogic.listTitles(listOf("آش", null, "پلو", "کباب", "خورش"))
        assertEquals(listOf("آش", "پلو", "کباب"), out)
    }

    @Test
    fun listTitlesAllowsFewerThanThree() {
        val out = WidgetLogic.listTitles(listOf("آش", null))
        assertEquals(listOf("آش"), out)
    }
}
