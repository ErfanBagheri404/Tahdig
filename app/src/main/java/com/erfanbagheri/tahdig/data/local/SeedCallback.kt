package com.erfanbagheri.tahdig.data.local

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.erfanbagheri.tahdig.data.local.seed.SeedLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Seeds the database on first creation.
 *
 * Room's onCreate runs on the DB thread inside a transaction, so we cannot do suspend
 * work there. Instead we register a callback that launches the seed on an app-scoped
 * coroutine once the database file exists.
 */
class SeedCallback(private val context: Context) : RoomDatabase.Callback() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        scope.launch {
            val database = TahdigDatabase.getInstance(context)
            SeedLoader.loadInto(database, context.assets)
        }
    }
}
