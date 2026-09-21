package com.erfanbagheri.tahdig

import android.app.Application
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.util.NutritionDB

class TahdigApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStore.init(this)
        NutritionDB.load(assets)
    }
}
