package com.erfanbagheri.tahdig.util

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.erfanbagheri.tahdig.data.local.TahdigDatabase
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.RatingEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #92 — the taste profile is assembled from tables that already exist, so these
 * tests are about the *join*: a rating on food 1 must not leak into food 2, and
 * a reset must suppress the score without deleting the row.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TasteProfileTest {

    private lateinit var db: TahdigDatabase
    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, TahdigDatabase::class.java)
            .allowMainThreadQueries().build()
        runBlocking {
            db.foodDao().insertAll(
                listOf(
                    food(1, 10, "خورش ۱"),
                    food(2, 10, "خورش ۲"),
                    food(3, 20, "پلو ۱"),
                ),
            )
        }
    }

    @After
    fun tearDown() = db.close()

    private fun food(id: Long, categoryId: Long, name: String) = FoodEntity(
        id = id,
        name = name,
        nameEn = "dish $id",
        categoryId = categoryId,
        mealTime = "ناهار",
        cuisine = "ایرانی",
        difficulty = "آسان",
        prepTimeMin = 30,
        ingredients = "[]",
    )

    @Test
    fun `a rating on one dish does not touch another`() = runBlocking {
        db.ratingDao().insertIfAbsent(RatingEntity(foodId = 1, stars = 5))
        db.ratingDao().updateStars(1, 5, now)

        val snap = TasteProfile.snapshot(db)
        assertEquals(5, snap.byFoodId[1L]?.stars)
        assertNull(snap.byFoodId[2L])
    }

    @Test
    fun `rating three dishes lifts their category`() = runBlocking {
        for (id in listOf(1L, 2L)) {
            db.ratingDao().insertIfAbsent(RatingEntity(foodId = id, stars = 5))
            db.ratingDao().updateStars(id, 5, now)
        }
        val snap = TasteProfile.snapshot(db)
        // Category 10 has a mean of 5; category 20 has no ratings at all.
        assertEquals(5f, snap.categoryMean[10L]?.toFloat())
        assertNull(snap.categoryMean[20L])
    }

    @Test
    fun `a reset suppresses the score but keeps the rating`() = runBlocking {
        db.ratingDao().insertIfAbsent(RatingEntity(foodId = 1, stars = 5))
        db.ratingDao().updateStars(1, 5, now)

        val before = TasteProfile.snapshot(db)
        val after = TasteProfile.snapshot(db, resetAt = now + 1)

        assertEquals(5, before.byFoodId[1L]?.stars)
        assertNull("pre-reset rating must not steer the feed", after.byFoodId[1L]?.stars)
        // …and the row itself is still there, so the user keeps their stars.
        assertEquals(5, db.ratingDao().getStars(1))
    }

    @Test
    fun `a rating written after the reset still counts`() = runBlocking {
        db.ratingDao().insertIfAbsent(RatingEntity(foodId = 1, stars = 5))
        db.ratingDao().updateStars(1, 5, now)
        val snap = TasteProfile.snapshot(db, resetAt = now - 1)
        assertEquals(5, snap.byFoodId[1L]?.stars)
    }

    @Test
    fun `a reset does not rewrite category history`() = runBlocking {
        for (id in listOf(1L, 2L)) {
            db.ratingDao().insertIfAbsent(RatingEntity(foodId = id, stars = 5))
            db.ratingDao().updateStars(id, 5, now)
        }
        // Dish signals gone, but «I love stews» is still a fact about the user.
        val snap = TasteProfile.snapshot(db, resetAt = now + 1)
        assertEquals(5f, snap.categoryMean[10L]?.toFloat())
    }

    @Test
    fun `an unticked picker category does not lift anything`() = runBlocking {
        val snap = TasteProfile.snapshot(db, preferredCategories = setOf(99L))
        val dish = db.foodDao().getById(1)!!
        assertEquals(1.0, snap.scoreOf(dish, now), 0.0001)
    }

    @Test
    fun `a ticked picker category lifts an unrated dish`() = runBlocking {
        val snap = TasteProfile.snapshot(db, preferredCategories = setOf(10L))
        val dish = db.foodDao().getById(1)!!
        assertTrue(snap.scoreOf(dish, now) > 1.0)
    }

    @Test
    fun `a hidden dish is never eligible`() = runBlocking {
        db.favoriteDao().upsert(
            com.erfanbagheri.tahdig.data.local.entity.FavoriteEntity(foodId = 1, isBlocked = true),
        )
        val snap = TasteProfile.snapshot(db)
        assertTrue(snap.isDisliked(1L))
        assertTrue(!TasteScorer.pool(disliked = snap.isDisliked(1L)))
    }
}
