package com.erfanbagheri.tahdig.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.erfanbagheri.tahdig.data.local.dao.CategoryDao
import com.erfanbagheri.tahdig.data.local.dao.FavoriteDao
import com.erfanbagheri.tahdig.data.local.dao.FoodDao
import com.erfanbagheri.tahdig.data.local.dao.HistoryDao
import com.erfanbagheri.tahdig.data.local.dao.ShoppingDao
import com.erfanbagheri.tahdig.data.local.entity.CategoryEntity
import com.erfanbagheri.tahdig.data.local.entity.FavoriteEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.entity.HistoryEntity
import com.erfanbagheri.tahdig.data.local.entity.ShoppingItemEntity
import com.erfanbagheri.tahdig.data.local.seed.SeedLoader

@Database(
    entities = [
        FoodEntity::class,
        CategoryEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        ShoppingItemEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class TahdigDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun categoryDao(): CategoryDao
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        private const val DB_NAME = "tahdig.db"

        @Volatile
        private var instance: TahdigDatabase? = null

        fun getInstance(context: Context): TahdigDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): TahdigDatabase =
            Room.databaseBuilder(context, TahdigDatabase::class.java, DB_NAME)
                .addCallback(SeedCallback(context))
                // ponytail: pre-release only, no users have data yet. Add a real
                // Migration when the app ships with a published schema.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        /**
         * Loads assets/seed JSON files on first creation only.
         * Room calls onCreate exactly once per database file lifetime.
         */
        suspend fun populateIfEmpty(context: Context) {
            val db = getInstance(context)
            if (db.foodDao().count() == 0) {
                SeedLoader.loadInto(db, context.assets)
            }
        }
    }
}
