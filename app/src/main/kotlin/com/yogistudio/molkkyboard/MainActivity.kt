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
import com.yogistudio.molkkyboard.crash.CrashReports
import com.yogistudio.molkkyboard.navigation.LaunchTarget
import com.yogistudio.molkkyboard.navigation.MolkkyBoardNavHost
import com.yogistudio.molkkyboard.startup.StartupLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/** Activité unique. `AppCompatActivity` pour la langue par application sur les versions d'Android antérieures à 13. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var startupLoader: StartupLoader

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var crashReports: CrashReports

    /** Rapport du dernier plantage, proposé (jamais envoyé) au retour sur l'accueil. */
    private val crashReport = MutableStateFlow<String?>(null)

    /** Écran demandé par la notification « Saisir le lancer » ou par un raccourci de l'icône. */
    private val target = MutableStateFlow<LaunchTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // L'écran de démarrage système ne reste que le temps de lire les réglages (sons de l'intro).
        installSplashScreen().setKeepOnScreenCondition { !startupLoader.state.value.settingsRead }
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            readTarget(intent)
            lifecycleScope.launch { crashReport.value = withContext(Dispatchers.IO) { crashReports.pending() } }
        }
        AppShortcuts.publish(this)
        enableEdgeToEdge()
        setContent {
            val startup by startupLoader.state.collectAsStateWithLifecycle()
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            MolkkyBoardTheme(sunlight = settings?.sunlight ?: false) {
                val open by target.collectAsStateWithLifecycle()
                val crash by crashReport.collectAsStateWithLifecycle()
                MolkkyBoardNavHost(
                    startup = startup,
                    target = open,
                    onTargetOpened = { target.value = null },
                    crashReport = crash,
                    onCrashReportHandled = {
                        crashReport.value = null
                        lifecycleScope.launch(Dispatchers.IO) { crashReports.clear() }
                    },
                    settings = settings,
                    onNotificationPrompted = { lifecycleScope.launch { settingsRepository.setNotificationPrompted() } },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readTarget(intent)
    }

    /**
     * Seuls un identifiant de partie bien formé (UUID) et les trois raccourcis connus sont acceptés ; tout le reste
     * est ignoré.
     */
    private fun readTarget(intent: Intent?) {
        intent ?: return
        intent.getStringExtra(EXTRA_GAME_ID)?.let { id ->
            runCatching { UUID.fromString(id).toString() }.getOrNull()?.takeIf { it == id }?.let {
                target.value = LaunchTarget.Game(it)
            }
            return
        }
        target.value =
            when (intent.getStringExtra(AppShortcuts.EXTRA_SHORTCUT)) {
                AppShortcuts.NEW_GAME -> LaunchTarget.NewGame
                AppShortcuts.PLACEMENT -> LaunchTarget.Placement
                AppShortcuts.RULES -> LaunchTarget.Rules
                else -> return
            }
    }

    companion object {
        const val EXTRA_GAME_ID = "com.yogistudio.molkkyboard.GAME_ID"
    }
}
