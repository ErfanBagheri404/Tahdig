package com.erfanbagheri.tahdig.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.erfanbagheri.tahdig.data.local.dao.FoodDao
import com.erfanbagheri.tahdig.data.local.dao.RatingDao
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.RatingEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #134 note search: the two queries the search pipeline depends on.
 *
 * The pipeline itself (SearchViewModel.withNotes) needs a live Activity, so it
 * is covered by the manual emulator check; what is pinned here is the SQL that
 * backs it — plain LIKE over the note column, no full-text table.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RatingNoteSearchTest {

    private lateinit var db: TahdigDatabase
    private lateinit var foodDao: FoodDao
    private lateinit var ratingDao: RatingDao

    /** Fixed clock for the #92 decay stamp; asserted, so not 'now'. */
    private val NOW = 1_700_000_000_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TahdigDatabase::class.java,
        ).allowMainThreadQueries().build()
        foodDao = db.foodDao()
        ratingDao = db.ratingDao()
    }

    @After
    fun tearDown() = db.close()

    private fun food(id: Long, name: String) = FoodEntity(
        id = id,
        name = name,
        nameEn = "test",
        categoryId = 1,
        mealTime = "LUNCH",
        cuisine = "IRANI",
        difficulty = "EASY",
        prepTimeMin = 30,
        ingredients = "test",
    )

    private suspend fun seedFood() {
        foodDao.insertAll(
            listOf(
                food(101, "خورش قورمه سبزی"),
                food(102, "آبگوشت"),
                food(103, "عدس‌پلو"),
            ),
        )
    }

    @Test
    fun noteSearchMatchesNoteTextButNeverStarOnlyRows() = runBlocking {
        seedFood()
        ratingDao.insertIfAbsent(RatingEntity(foodId = 101, stars = 5))
        ratingDao.updateNote(101, "با زعفران درست می‌شود")
        // 4 stars, empty note: must not appear in note search.
        ratingDao.insertIfAbsent(RatingEntity(foodId = 103, stars = 4))

        val hits = ratingDao.searchNotes("زعفران")
        assertEquals(listOf(101L), hits.map { it.foodId })

        val flowHits = ratingDao.searchNotesFlow("زعفران").first()
        assertEquals(listOf(101L), flowHits.map { it.foodId })
    }

    @Test
    fun noteSearchIgnoresDishesWithNoNoteAtAll() = runBlocking {
        seedFood()
        ratingDao.insertIfAbsent(RatingEntity(foodId = 101, stars = 3))

        // Without the `note != ''` guard a LIKE '%%' on an empty note would
        // still be a row — the guard lives in the query, not the caller.
        assertTrue(ratingDao.searchNotes("خورش").isEmpty())
        assertTrue(ratingDao.searchNotesFlow("خورش").first().isEmpty())
    }

    @Test
    fun byIdsFlowReturnsEveryRequestedDish() = runBlocking {
        seedFood()
        val got = foodDao.byIdsFlow(listOf(102, 103)).first().map { it.id }.toSet()
        assertEquals(setOf(102L, 103L), got)
    }

    @Test
    fun byIdsFlowWithNoIdsIsEmptyNotAnError() = runBlocking {
        seedFood()
        assertTrue(foodDao.byIdsFlow(emptyList()).first().isEmpty())
    }

    @Test
    fun clearingTheNoteRemovesItFromSearch() = runBlocking {
        seedFood()
        ratingDao.insertIfAbsent(RatingEntity(foodId = 102, stars = 4))
        ratingDao.updateNote(102, "فوت شد")
        assertEquals(1, ratingDao.searchNotes("فوت").size)

        // User clears the field: the note must leave the index, otherwise a
        // deleted sentence keeps coming back in search results forever.
        ratingDao.updateNote(102, "")
        assertTrue(ratingDao.searchNotes("فوت").isEmpty())
    }

    @Test
    fun changingStarsKeepsTheNoteIntact() = runBlocking {
        seedFood()
        ratingDao.insertIfAbsent(RatingEntity(foodId = 102, stars = 4))
        ratingDao.updateNote(102, "لوبیا را از قبل خیس کن")
        ratingDao.updateStars(102, 2, NOW)

        val row = ratingDao.observe(102).first()!!
        assertEquals(2, row.stars)
        assertEquals("لوبیا را از قبل خیس کن", row.note)
    }
}
