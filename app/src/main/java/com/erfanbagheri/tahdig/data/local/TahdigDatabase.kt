package com.erfanbagheri.tahdig.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.erfanbagheri.tahdig.data.local.dao.CategoryDao
import com.erfanbagheri.tahdig.data.local.dao.CookSessionDao
import com.erfanbagheri.tahdig.data.local.dao.FavoriteDao
import com.erfanbagheri.tahdig.data.local.dao.FoodDao
import com.erfanbagheri.tahdig.data.local.dao.HistoryDao
import com.erfanbagheri.tahdig.data.local.dao.MealPlanDao
import com.erfanbagheri.tahdig.data.local.dao.MilestoneCheckDao
import com.erfanbagheri.tahdig.data.local.dao.PantryDao
import com.erfanbagheri.tahdig.data.local.dao.RatingDao
import com.erfanbagheri.tahdig.data.local.dao.RecentViewDao
import com.erfanbagheri.tahdig.data.local.dao.ShoppingDao
import com.erfanbagheri.tahdig.data.local.dao.ShoppingTripDao
import com.erfanbagheri.tahdig.data.local.entity.CategoryEntity
import com.erfanbagheri.tahdig.data.local.entity.CookSessionEntity
import com.erfanbagheri.tahdig.data.local.entity.FavoriteEntity
import com.erfanbagheri.tahdig.data.local.entity.FoodEntity
import com.erfanbagheri.tahdig.data.local.dao.JournalDao
import com.erfanbagheri.tahdig.data.local.dao.BarcodeScanDao
import com.erfanbagheri.tahdig.data.local.dao.NutritionLogDao
import com.erfanbagheri.tahdig.data.local.entity.HistoryEntity
import com.erfanbagheri.tahdig.data.local.entity.JournalEntity
import com.erfanbagheri.tahdig.data.local.entity.BarcodeScanEntity
import com.erfanbagheri.tahdig.data.local.entity.NutritionLogEntity
import com.erfanbagheri.tahdig.data.local.entity.MealPlanEntity
import com.erfanbagheri.tahdig.data.local.entity.MilestoneCheckEntity
import com.erfanbagheri.tahdig.data.local.entity.PantryItemEntity
import com.erfanbagheri.tahdig.data.local.entity.RatingEntity
import com.erfanbagheri.tahdig.data.local.entity.RecentViewEntity
import com.erfanbagheri.tahdig.data.local.entity.ShoppingItemEntity
import com.erfanbagheri.tahdig.data.local.entity.ShoppingTripEntity
import com.erfanbagheri.tahdig.data.local.seed.SeedLoader

@Database(
    entities = [
        FoodEntity::class,
        CategoryEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        MealPlanEntity::class,
        RatingEntity::class,
        RecentViewEntity::class,
        ShoppingItemEntity::class,
        PantryItemEntity::class,
        MilestoneCheckEntity::class,
        CookSessionEntity::class,
        NutritionLogEntity::class,
        ShoppingTripEntity::class,
        JournalEntity::class,
        BarcodeScanEntity::class,
    ],
    version = 15, // + barcode_scans (#116); destructive fallback, pre-release
    exportSchema = true,
)
abstract class TahdigDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun categoryDao(): CategoryDao
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun ratingDao(): RatingDao
    abstract fun recentViewDao(): RecentViewDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun shoppingTripDao(): ShoppingTripDao
    abstract fun pantryDao(): PantryDao
    abstract fun milestoneCheckDao(): MilestoneCheckDao
    abstract fun cookSessionDao(): CookSessionDao

    /** Per-day nutrition log (#110). */
    abstract fun nutritionLogDao(): NutritionLogDao

    /** Cooking journal (#124). */
    abstract fun journalDao(): JournalDao
    abstract fun barcodeScanDao(): BarcodeScanDao

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
                // ponytail: pre-release only — no shipped users yet. Add real Migrations before first release.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        /** Close the live DB instance so backup/restore can safely overwrite the file. */
        fun closeInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }

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
