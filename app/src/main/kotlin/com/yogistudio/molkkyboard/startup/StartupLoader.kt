package com.yogistudio.molkkyboard.startup

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import com.yogistudio.molkkyboard.core.data.GameRepository
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.feature.intro.IntroLoadState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import com.yogistudio.molkkyboard.core.designsystem.R as DesignR

/** Ce que le démarrage a appris : état du chargement, intro courte ou non, partie en cours à reprendre. */
data class StartupState(
    val load: IntroLoadState = IntroLoadState.Loading,
    /** `null` tant que les réglages ne sont pas lus : l'écran de démarrage système reste affiché. */
    val shortIntro: Boolean? = null,
    val ongoingGameId: String? = null,
)

/**
 * Chargement de démarrage, lancé dès `Application.onCreate` et indépendant de l'animation d'intro : réglages
 * (et compteur de lancements), partie en cours à reprendre, polices. Viendront la session et la synchronisation
 * (Phase 4). Un échec n'empêche jamais d'ouvrir le menu : l'app passe alors en mode hors ligne.
 */
@Singleton
class StartupLoader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val settings: SettingsRepository,
        private val games: GameRepository,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val started = AtomicBoolean(false)
        private val mutableState = MutableStateFlow(StartupState())
        val state: StateFlow<StartupState> = mutableState.asStateFlow()

        fun start() {
            if (!started.compareAndSet(false, true)) return
            scope.launch {
                val result =
                    runCatching {
                        val current = settings.recordLaunch()
                        mutableState.update { it.copy(shortIntro = current.shortIntro && current.canChooseShortIntro) }
                        val ongoing = games.observeOngoingGameId().first()
                        preloadFonts()
                        ongoing
                    }
                mutableState.update { state ->
                    result.fold(
                        onSuccess = { state.copy(load = IntroLoadState.Ready, ongoingGameId = it) },
                        onFailure = {
                            Timber.w(it, "Chargement de démarrage incomplet")
                            state.copy(load = IntroLoadState.Failed, shortIntro = state.shortIntro ?: false)
                        },
                    )
                }
            }
        }

        private fun preloadFonts() {
            listOf(
                DesignR.font.bricolage_grotesque_bold,
                DesignR.font.bricolage_grotesque_extrabold,
                DesignR.font.inter_regular,
                DesignR.font.inter_medium,
                DesignR.font.inter_semibold,
                DesignR.font.inter_bold,
            ).forEach { ResourcesCompat.getFont(context, it) }
        }
    }
