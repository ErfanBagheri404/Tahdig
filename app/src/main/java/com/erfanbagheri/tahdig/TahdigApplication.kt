package com.erfanbagheri.tahdig

import android.app.Application
import com.erfanbagheri.tahdig.data.prefs.SettingsStore

class TahdigApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStore.init(this)
    }
}
