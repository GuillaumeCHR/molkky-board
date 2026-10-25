package com.yogistudio.molkkyboard

import android.app.Application
import com.yogistudio.molkkyboard.startup.StartupLoader
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MolkkyBoardApp : Application() {
    @Inject
    lateinit var startupLoader: StartupLoader

    override fun onCreate() {
        super.onCreate()
        // Aucun arbre en release : aucun log n'est émis.
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        // Le chargement démarre ici, en parallèle de l'intro, et ne dépend jamais de l'animation.
        startupLoader.start()
    }
}
