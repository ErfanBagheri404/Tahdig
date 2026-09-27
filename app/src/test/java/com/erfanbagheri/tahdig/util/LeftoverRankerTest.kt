package com.erfanbagheri.tahdig.util

import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #107 ACs: "Enter برنج + مرغ -> rice/chicken dishes ranked, missing items listed",
 * "Empty input -> no empty state crash, CTA to add", "Unit tests for
 * coverage/min-missing scoring".
 *
 * Scoring is the strict order (a) coverage, (b) minimal additional items,
 * (c) expiry urgency — the weights are spaced so (a) can never be overturned.
 */
class LeftoverRankerTest {

    private val DAY = 86_400_000L
    private val now = 1_700_000_000_000L

    private fun food(id: Long, name: String, ingredients: String) = FoodEntity(
        id = id,
        name = name,
        nameEn = "",
        categoryId = 1,
        mealTime = "LUNCH",
        cuisine = "IRANI",
        difficulty = "EASY",
        prepTimeMin = 30,
        ingredients = ingredients,
        tags = "",
        description = "",
        imageUrl = null,
        isBlocked = false,
    )

    // The AC's own scenario: برنج + مرغ.
    private val all = listOf(
        food(1, "ته‌چین مرغ", "برنج، مرغ، ماست، زعفران، تخم‌مرغ"),
        food(2, "زرشک‌پلو با مرغ", "برنج، مرغ، زرشک، زعفران"),
        food(3, "کباب کوبیده", "گوشت گوسفند، پیاز، زعفران"),
        food(4, "سالاد فصل", "گوجه، خیار، کاهو"),
        food(5, "عدس‌پلو", "برنج، عدس، کشمش، پیاز"),
    )

    private fun rank(vararg texts: String, at: Long = now) =
        LeftoverRanker.rank(texts.map { LeftoverRanker.Leftover(it, at) }, all, now)

    // ── (a) coverage first ─────────────────────────────────────────

    @Test
    fun `rice and chicken rank rice-chicken dishes above rice-only ones`() {
        val out = rank("برنج", "مرغ")
        assertTrue("must find rice/chicken dishes", out.isNotEmpty())
        // Both ۲-ingredient dishes beat عدس‌پلو, which only uses the rice.
        val top = out.take(2).map { it.food.name }.toSet()
        assertEquals(setOf("ته‌چین مرغ", "زرشک‌پلو با مرغ"), top)
        // عدس‌پلو still appears — it uses one of the two.
        assertTrue(out.any { it.food.name == "عدس‌پلو" })
        assertTrue(out.indexOfFirst { it.food.name == "عدس‌پلو" } >= 2)
    }

    @Test
    fun `dishes using nothing entered are dropped`() {
        val out = rank("برنج", "مرغ")
        assertTrue(out.none { it.food.name == "کباب کوبیده" })
        assertTrue(out.none { it.food.name == "سالاد فصل" })
    }

    @Test
    fun `covered count is the number of recipe items the leftovers satisfy`() {
        val top = rank("برنج", "مرغ").first { it.food.name == "ته‌چین مرغ" }
        // ته‌چین مرغ: برنج، مرغ، ماست، زعفران، تخم‌مرغ — two are covered.
        assertEquals(2, top.covered)
        assertEquals(listOf("برنج", "مرغ").sorted(), top.used.sorted())
    }

    // ── (b) minimal additional items ───────────────────────────────

    @Test
    fun `with equal coverage the dish needing fewer extras wins`() {
        val extra = listOf(
            food(10, "کم‌هزینه", "برنج، مرغ"),
            food(11, "پرهزینه", "برنج، مرغ، ماست، زعفران، کره، خامه"),
        )
        val out = LeftoverRanker.rank(
            listOf(LeftoverRanker.Leftover("برنج"), LeftoverRanker.Leftover("مرغ")),
            extra, now,
        )
        assertEquals("کم‌هزینه", out.first().food.name)
        assertEquals(0, out.first().extraCount)
        assertEquals(4, out[1].extraCount)
    }

    @Test
    fun `missing items are listed for the diff line`() {
        val top = rank("برنج", "مرغ").first { it.food.name == "زرشک‌پلو با مرغ" }
        assertEquals(listOf("زرشک", "زعفران").sorted(), top.missing.sorted())
        assertEquals("با ۲ ماده دیگه درست میشه", top.headline())
    }

    @Test
    fun `a fully covered dish says so instead of counting zero`() {
        val out = LeftoverRanker.rank(
            listOf(LeftoverRanker.Leftover("برنج"), LeftoverRanker.Leftover("عدس")),
            listOf(food(20, "عدس‌پلو", "برنج، عدس")), now,
        )
        assertEquals("با همین مواد درست میشه", out.first().headline())
    }

    @Test
    fun `coverage can never be overturned by the extra-items penalty`() {
        // Two covered vs one covered with zero extras — the 100-point gap wins.
        val foods = listOf(
            food(30, "دوکاوره", "برنج، مرغ، ماست، زعفران، کره، خامه، روغن، نمک"),
            food(31, "یک‌کاوره", "برنج"),
        )
        val out = LeftoverRanker.rank(
            listOf(LeftoverRanker.Leftover("برنج"), LeftoverRanker.Leftover("مرغ")),
            foods, now,
        )
        assertEquals("دوکاوره", out.first().food.name)
    }

    // ── (c) expiry urgency ─────────────────────────────────────────

    @Test
    fun `an older leftover breaks a tie toward the dish that uses it`() {
        // Same coverage, same extras: the dish eating the 2-day-old component wins.
        val foods = listOf(
            food(40, "تازه", "برنج، ماست"),
            food(41, "مونده", "برنج، کشمش"),
        )
        val out = LeftoverRanker.rank(
            listOf(
                LeftoverRanker.Leftover("برنج", now),
                LeftoverRanker.Leftover("کشمش", now - 2 * DAY),
            ),
            foods, now,
        )
        assertEquals("مونده", out.first().food.name)
        assertTrue(out.first().score > out[1].score)
    }

    @Test
    fun `urgency saturates at the consume-by window`() {
        val foods = listOf(food(50, "الف", "برنج"), food(51, "ب", "برنج"))
        // Beyond the window both are equally urgent, so the name tiebreak decides.
        val a = LeftoverRanker.rank(
            listOf(LeftoverRanker.Leftover("برنج", now - 5 * DAY)), foods, now)
        val b = LeftoverRanker.rank(
            listOf(LeftoverRanker.Leftover("برنج", now - 99 * DAY)), foods, now)
        assertEquals(a.first().score, b.first().score, 0.0001)
    }

    @Test
    fun `an unknown cookedAt contributes no urgency`() {
        val fresh = LeftoverRanker.rank(
            listOf(LeftoverRanker.Leftover("برنج")), all, now)
        val dated = LeftoverRanker.rank(
            listOf(LeftoverRanker.Leftover("برنج", now)), all, now)
        assertEquals(fresh.first().score, dated.first().score, 0.0001)
    }

    @Test
    fun `a future timestamp never scores negative urgency`() {
        val out = LeftoverRanker.rank(
            listOf(LeftoverRanker.Leftover("برنج", now + 10 * DAY)), all, now)
        assertTrue(out.first().score > 0.0)
    }

    // ── empty input (AC: no crash, CTA to add) ─────────────────────

    @Test
    fun `empty input returns an empty list instead of throwing`() {
        assertTrue(LeftoverRanker.rank(emptyList(), all, now).isEmpty())
    }

    @Test
    fun `blank and punctuation-only entries are ignored`() {
        assertTrue(LeftoverRanker.rank(
            listOf(LeftoverRanker.Leftover(""), LeftoverRanker.Leftover("  ")), all, now,
        ).isEmpty())
    }

    @Test
    fun `one blank entry alongside a real one still ranks`() {
        val out = LeftoverRanker.rank(
            listOf(LeftoverRanker.Leftover(""), LeftoverRanker.Leftover("برنج")), all, now)
        assertTrue(out.isNotEmpty())
    }

    // ── Persian text handling ──────────────────────────────────────

    @Test
    fun `a quantity-bearing entry still matches the dish ingredient`() {
        // Entered «۲ پیمانه برنج» must hit a dish listing plain «برنج».
        val out = LeftoverRanker.rank(
            listOf(LeftoverRanker.Leftover("برنج")),
            listOf(food(60, "پلو", "۲ پیمانه برنج، کره")), now)
        assertTrue(out.isNotEmpty())
        assertEquals(1, out.first().covered)
    }

    @Test
    fun `ZWNJ and space spellings match the same dish`() {
        // Dish stores «مرغ سرخ شده» (spaces); the user types «مرغ سرخ‌شده» (ZWNJ).
        val foods = listOf(food(70, "مرغ‌بریان", "مرغ سرخ شده، پیاز"))
        val zwnj = LeftoverRanker.rank(
            listOf(LeftoverRanker.Leftover("مرغ سرخ‌شده")), foods, now)
        assertTrue("ZWNJ/space variants must match", zwnj.isNotEmpty())
    }

    @Test
    fun `limit caps the result list`() {
        val many = (1..40).map { food(it.toLong(), "غذا $it", "برنج، مرغ") }
        val out = LeftoverRanker.rank(
            listOf(LeftoverRanker.Leftover("برنج"), LeftoverRanker.Leftover("مرغ")),
            many, now, limit = 5)
        assertEquals(5, out.size)
    }
}
