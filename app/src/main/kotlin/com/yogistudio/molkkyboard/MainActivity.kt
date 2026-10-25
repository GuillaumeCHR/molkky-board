package com.yogistudio.molkkyboard

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.navigation.MolkkyBoardNavHost
import com.yogistudio.molkkyboard.startup.StartupLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Activité unique. `AppCompatActivity` pour la langue par application sur les versions d'Android antérieures à 13. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var startupLoader: StartupLoader

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // L'écran de démarrage système ne reste que le temps de lire les réglages (intro courte ou complète).
        installSplashScreen().setKeepOnScreenCondition { startupLoader.state.value.shortIntro == null }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val startup by startupLoader.state.collectAsStateWithLifecycle()
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            MolkkyBoardTheme(sunlight = settings?.sunlight ?: false) {
                MolkkyBoardNavHost(startup = startup)
            }
        }
    }
}
