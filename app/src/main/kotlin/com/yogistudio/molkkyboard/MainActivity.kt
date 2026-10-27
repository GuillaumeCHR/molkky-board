package com.yogistudio.molkkyboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.navigation.MolkkyBoardNavHost
import com.yogistudio.molkkyboard.startup.StartupLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Activité unique. `AppCompatActivity` pour la langue par application sur les versions d'Android antérieures à 13. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var startupLoader: StartupLoader

    @Inject
    lateinit var settingsRepository: SettingsRepository

    /** Partie à ouvrir, demandée par la notification « Saisir le lancer ». */
    private val gameToOpen = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // L'écran de démarrage système ne reste que le temps de lire les réglages (intro courte ou complète).
        installSplashScreen().setKeepOnScreenCondition { startupLoader.state.value.shortIntro == null }
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) readGameToOpen(intent)
        enableEdgeToEdge()
        setContent {
            val startup by startupLoader.state.collectAsStateWithLifecycle()
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            MolkkyBoardTheme(sunlight = settings?.sunlight ?: false) {
                val open by gameToOpen.collectAsStateWithLifecycle()
                MolkkyBoardNavHost(
                    startup = startup,
                    gameToOpen = open,
                    onGameOpened = { gameToOpen.value = null },
                    settings = settings,
                    onNotificationPrompted = { lifecycleScope.launch { settingsRepository.setNotificationPrompted() } },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readGameToOpen(intent)
    }

    /** Seul un identifiant de partie bien formé (UUID) est accepté ; tout le reste est ignoré. */
    private fun readGameToOpen(intent: Intent?) {
        val id = intent?.getStringExtra(EXTRA_GAME_ID) ?: return
        gameToOpen.value = runCatching { UUID.fromString(id).toString() }.getOrNull()?.takeIf { it == id }
    }

    companion object {
        const val EXTRA_GAME_ID = "com.yogistudio.molkkyboard.GAME_ID"
    }
}
