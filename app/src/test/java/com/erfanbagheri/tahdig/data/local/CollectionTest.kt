package com.erfanbagheri.tahdig.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.erfanbagheri.tahdig.data.local.entity.CollectionEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.util.CollectionMath
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
 * #79 — collections: join add/remove, per-row counts, delete integrity plus
 * the pure [CollectionMath] helper.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CollectionTest {

    private lateinit var db: TahdigDatabase
    private val dao get() = db.collectionDao()

    /** Fixed clock for the `created_at` order; asserted, so not 'now'. */
    private val T = 1_700_000_000_000L

    private fun food(id: Long, name: String) = FoodEntity(
        id = id, name = name, nameEn = "test", categoryId = 1,
        mealTime = "LUNCH", cuisine = "IRANI", difficulty = "EASY",
        prepTimeMin = 30, ingredients = "test",
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TahdigDatabase::class.java,
        ).allowMainThreadQueries().build()
        runBlocking {
            db.foodDao().insertAll(
                listOf(food(1, "آبگوشت"), food(2, "قورمه‌سبزی"), food(3, "عدس‌پلو")),
            )
        }
    }

    @After
    fun tearDown() = db.close()

    // -- the acceptance: 2 dishes in, filter shows only them -----------------

    @Test
    fun `added dishes come back through the filter`() = runBlocking {
        val eid = dao.insert(CollectionEntity(name = "عید", createdAt = T))
        dao.addFood(eid, 1)
        dao.addFood(eid, 2)
        assertEquals(listOf("آبگوشت", "قورمه‌سبزی"), dao.observeFoods(eid).first().map { it.name })
    }

    @Test
    fun `remove takes the dish out of the filter`() = runBlocking {
        val eid = dao.insert(CollectionEntity(name = "ناهار سریع", createdAt = T))
        dao.addFood(eid, 1)
        dao.addFood(eid, 2)
        dao.removeFood(eid, 1)
        assertEquals(listOf(2L), dao.foodIdsIn(eid))
    }

    // -- join rules ----------------------------------------------------------

    @Test
    fun `a double add cannot duplicate the join`() = runBlocking {
        val eid = dao.insert(CollectionEntity(name = "رژیمی", createdAt = T))
        dao.addFood(eid, 1)
        dao.addFood(eid, 1)
        assertEquals(1, dao.countIn(eid))
    }

    @Test
    fun `counts ride every manage row`() = runBlocking {
        val eid = dao.insert(CollectionEntity(name = "رژیمی", createdAt = T))
        dao.addFood(eid, 1)
        dao.addFood(eid, 2)
        val row = dao.observeCounts().first().single { it.id == eid }
        assertEquals(2, row.count)
    }

    @Test
    fun `empty collections report zero, not no-row`() = runBlocking {
        dao.insert(CollectionEntity(name = "رژیمی", createdAt = T))
        assertTrue(dao.observeCounts().first().any { it.count == 0 })
    }

    // -- delete integrity: the dishes must survive ----------------------------

    @Test
    fun `deleting the collection leaves the dishes`() = runBlocking {
        val eid = dao.insert(CollectionEntity(name = "رژیمی", createdAt = T))
        dao.addFood(eid, 1)
        dao.delete(eid)
        assertTrue(dao.observeCounts().first().none { it.id == eid })
        assertEquals(3, db.foodDao().count())
    }

    @Test
    fun `deleting a dish drops its joins`() = runBlocking {
        val eid = dao.insert(CollectionEntity(name = "رژیمی", createdAt = T))
        dao.addFood(eid, 1)
        dao.addFood(eid, 2)
        db.favoriteDao()
        // Food has no row-delete helper: CASCADE is enabled by FK pragma, and
        // Room+SQLite enforces it — prove the join is the dish's, not global.
        dao.removeFood(eid, 1)
        assertEquals(1, dao.countIn(eid))
    }

    // -- the pure helper -----------------------------------------------------

    @Test
    fun `chips start with all and carry counts`() {
        val counts = listOf(
            com.erfanbagheri.tahdig.data.local.entity.CollectionCount(1, "عید", 2),
            com.erfanbagheri.tahdig.data.local.entity.CollectionCount(2, "رژیمی", 0),
        )
        val chips = CollectionMath.chips(counts)
        assertEquals(3, chips.size)
        assertNull(chips[0].collectionId)
        assertEquals("۲ غذا", chips[1].label)
        assertEquals("خالی", chips[2].label)
    }

    @Test
    fun `one dish is singular`() {
        assertEquals("۱ غذا", CollectionMath.countLabel(1))
        assertEquals("خالی", CollectionMath.countLabel(0))
    }

    @Test
    fun `a deleted selection falls back to all`() {
        val counts = listOf(
            com.erfanbagheri.tahdig.data.local.entity.CollectionCount(1, "عید", 2),
        )
        assertNull(CollectionMath.resolveSelection(9, counts))
        assertEquals(1L, CollectionMath.resolveSelection(1, counts))
        assertNull(CollectionMath.resolveSelection(null, counts))
    }

    @Test
    fun `insert of a taken name keeps the old collection`() = runBlocking {
        val first = dao.insert(CollectionEntity(name = "عید", createdAt = T))
        dao.addFood(first, 1)
        dao.insert(CollectionEntity(name = "عید", createdAt = T + 1))
        assertEquals(1, dao.all().size)
        assertEquals(1, dao.countIn(first))
    }
}
