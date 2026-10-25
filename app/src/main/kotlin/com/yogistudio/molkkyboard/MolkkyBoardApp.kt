package com.yogistudio.molkkyboard

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MolkkyBoardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Aucun arbre en release : aucun log n'est émis.
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
