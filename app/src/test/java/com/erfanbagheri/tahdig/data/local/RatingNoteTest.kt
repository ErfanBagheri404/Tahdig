package com.erfanbagheri.tahdig.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.erfanbagheri.tahdig.data.local.entity.RatingEntity
import kotlinx.coroutines.flow.first
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
 * #134 — rating rows now carry a private note, and the two must never be able
 * to clobber each other. Every test here exists because that is the failure.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RatingNoteTest {

    private lateinit var db: TahdigDatabase
    private val dao get() = db.ratingDao()

    /** Fixed clock: the decay input is asserted on, so it must not be 'now'. */
    private val NOW = 1_700_000_000_000L

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, TahdigDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    // -- the bug this feature must not have ----------------------------------

    @Test
    fun `changing a star does not erase the note`() = runBlocking {
        dao.insertIfAbsent(RatingEntity(foodId = 1, stars = 3))
        dao.updateNote(1, "خیلی کم‌زحمت بود، نصف مواد کم بود")

        dao.updateStars(1, 5, NOW)

        assertEquals("خیلی کم‌زحمت بود، نصف مواد کم بود", dao.observe(1).first()!!.note)
        assertEquals(5, dao.observe(1).first()!!.stars)
    }

    @Test
    fun `writing a note does not erase the stars`() = runBlocking {
        dao.insertIfAbsent(RatingEntity(foodId = 1, stars = 4))
        dao.updateNote(1, "برای مهمانی عالی بود")
        assertEquals(4, dao.observe(1).first()!!.stars)
    }

    @Test
    fun `stars can be cleared without touching the note`() = runBlocking {
        dao.insertIfAbsent(RatingEntity(foodId = 1, stars = 2))
        dao.updateNote(1, "دفعهٔ بعد کمتر نمک بزن")
        dao.updateStars(1, 0, NOW)
        assertEquals("دفعهٔ بعد کمتر نمک بزن", dao.observe(1).first()!!.note)
        assertEquals(0, dao.observe(1).first()!!.stars)
    }

    @Test
    fun `clearing the note keeps the stars`() = runBlocking {
        dao.insertIfAbsent(RatingEntity(foodId = 1, stars = 5))
        dao.updateNote(1, "فقط یک‌بار درست درمی‌آید")
        dao.updateNote(1, "")
        assertEquals(5, dao.observe(1).first()!!.stars)
        assertEquals("", dao.observe(1).first()!!.note)
    }

    // -- one row per dish ----------------------------------------------------

    @Test
    fun `repeated writes do not create a second row`() = runBlocking {
        dao.insertIfAbsent(RatingEntity(foodId = 7, stars = 1))
        dao.updateStars(7, 2, NOW)
        dao.updateNote(7, "خوب")
        dao.updateStars(7, 3, NOW)
        assertEquals(1, dao.observeNoted().first().size)
    }

    @Test
    fun `a note on a dish with no stars is allowed`() = runBlocking {
        // Writing a note before rating is the natural order in the UI.
        dao.insertIfAbsent(RatingEntity(foodId = 9, stars = 0))
        dao.updateNote(9, "فقط خواندمش، هنوز نپختم")
        val row = dao.observe(9).first()!!
        assertEquals(0, row.stars)
        assertTrue(row.note.isNotBlank())
    }

    // -- listing and search --------------------------------------------------

    @Test
    fun `only noted dishes are listed`() = runBlocking {
        dao.insertIfAbsent(RatingEntity(foodId = 1, stars = 5))
        dao.insertIfAbsent(RatingEntity(foodId = 2, stars = 4))
        dao.insertIfAbsent(RatingEntity(foodId = 3, stars = 3))
        dao.updateNote(2, "نمک زیاد بود")

        val noted = dao.observeNoted().first()
        assertEquals(1, noted.size)
        assertEquals(2, noted[0].foodId)
        assertEquals(1, dao.noteCount())
    }

    @Test
    fun `clearing the last note empties the list`() = runBlocking {
        dao.insertIfAbsent(RatingEntity(foodId = 1, stars = 5))
        dao.updateNote(1, "خوب بود")
        assertEquals(1, dao.noteCount())
        dao.updateNote(1, "")
        assertEquals(0, dao.noteCount())
        assertTrue(dao.observeNoted().first().isEmpty())
    }

    @Test
    fun `search finds a note by a word in it`() = runBlocking {
        dao.insertIfAbsent(RatingEntity(foodId = 1, stars = 4))
        dao.insertIfAbsent(RatingEntity(foodId = 2, stars = 5))
        dao.updateNote(1, "با لوبیای قرمز درست می‌شود")
        dao.updateNote(2, "برای صبحانهٔ سریع عالی است")

        val hits = dao.searchNotes("لوبیا")
        assertEquals(1, hits.size)
        assertEquals(1, hits[0].foodId)
    }

    @Test
    fun `search matches persian text and ignores empty notes`() = runBlocking {
        dao.insertIfAbsent(RatingEntity(foodId = 1, stars = 4))
        dao.insertIfAbsent(RatingEntity(foodId = 2, stars = 5))
        dao.updateNote(2, "قورمه‌سبزی خوب درآمد")
        assertEquals(1, dao.searchNotes("قورمه").size)
        // An empty query matches every note — but never a dish with none.
        // That filter is `note != ''` in the query, not the caller.
        val all = dao.searchNotes("")
        assertEquals(1, all.size)
        assertEquals(2, all[0].foodId)
    }

    @Test
    fun `a search that matches nothing returns empty`() = runBlocking {
        dao.insertIfAbsent(RatingEntity(foodId = 1, stars = 4))
        dao.updateNote(1, "خوب بود")
        assertTrue(dao.searchNotes("ژاپنی").isEmpty())
    }

    // -- storage rules -------------------------------------------------------

    @Test
    fun `a fresh row has an empty note, not null`() = runBlocking {
        dao.insertIfAbsent(RatingEntity(foodId = 1, stars = 3))
        assertEquals("", dao.observe(1).first()!!.note)
    }

    @Test
    fun `an unrated dish has no row at all`() = runBlocking {
        assertNull(dao.observe(42).first())
        assertNull(dao.getStars(42))
    }

    @Test
    fun `the note ceiling is documented and positive`() {
        assertTrue("a note ceiling must be positive", RatingEntity.MAX_NOTE_CHARS > 0)
        assertTrue("600 chars is the documented ceiling", RatingEntity.MAX_NOTE_CHARS == 600)
    }

    @Test
    fun `getStars still reads the rating the old way`() = runBlocking {
        dao.insertIfAbsent(RatingEntity(foodId = 5, stars = 4))
        assertEquals(4, dao.getStars(5))
    }
}
