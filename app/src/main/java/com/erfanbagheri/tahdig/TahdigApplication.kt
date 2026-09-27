package com.erfanbagheri.tahdig

import android.app.Application
import com.erfanbagheri.tahdig.data.prefs.SettingsStore
import com.erfanbagheri.tahdig.util.IngredientRegistry
import com.erfanbagheri.tahdig.util.NutritionDB
import com.erfanbagheri.tahdig.util.SubstitutionRegistry
import com.erfanbagheri.tahdig.util.TechniqueRegistry

class TahdigApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStore.init(this)
        NutritionDB.load(assets)
        IngredientRegistry.load(this)
        SubstitutionRegistry.load(this)
        TechniqueRegistry.load(this)
    }
}
