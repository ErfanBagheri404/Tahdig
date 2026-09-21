package com.erfanbagheri.tahdig.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DurationParserTest {

    @Test
    fun `parses persian minutes`() {
        val f = DurationParser.first("۴۵ دقیقه بپزید")!!
        assertEquals(45 * 60L, f.seconds)
        assertEquals(45L, f.minutes)
    }

    @Test
    fun `parses ascii minutes`() {
        assertEquals(20 * 60L, DurationParser.first("20 دقیقه")!!.seconds)
    }

    @Test
    fun `parses hours`() {
        assertEquals(2 * 60 * 60L, DurationParser.first("۲ ساعت بجوشد")!!.seconds)
    }

    @Test
    fun `parses hours and minutes`() {
        val f = DurationParser.first("۱ ساعت و ۳۰ دقیقه")!!
        assertEquals(90 * 60L, f.seconds)
        assertEquals(90L, f.minutes)
    }

    @Test
    fun `parses hour and a half`() {
        assertEquals(150 * 60L, DurationParser.first("۲ ساعت و نیم")!!.seconds)
    }

    @Test
    fun `no duration returns null`() {
        assertNull(DurationParser.first("پیاز را خرد کنید"))
    }

    @Test
    fun `zero duration is not offered as a timer`() {
        assertNull(DurationParser.first("۰ دقیقه"))
    }

    @Test
    fun `finds all durations in order`() {
        val all = DurationParser.all("۵ دقیقه تفت دهید سپس ۲ ساعت بپزید")
        assertEquals(2, all.size)
        assertEquals(5 * 60L, all[0].seconds)
        assertEquals(120 * 60L, all[1].seconds)
    }

    @Test
    fun `range yields the number attached to the unit`() {
        // "۱۰ تا ۱۵ دقیقه" → 15, the upper bound — the safe direction for a timer.
        assertEquals(15 * 60L, DurationParser.first("۱۰ تا ۱۵ دقیقه")!!.seconds)
    }

    @Test
    fun `mmss pads`() {
        assertEquals("05:00", DurationParser.mmss(300))
        assertEquals("02:05", DurationParser.mmss(125))
        assertEquals("00:09", DurationParser.mmss(9))
    }
}
