package com.yogistudio.molkkyboard.startup

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import com.yogistudio.molkkyboard.feature.intro.IntroLoadState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import com.yogistudio.molkkyboard.core.designsystem.R as DesignR

/**
 * Chargement de démarrage, lancé dès `Application.onCreate` et indépendant de l'animation d'intro.
 * Aujourd'hui : préchargement des polices. Viendront Room et DataStore (J3), la partie en cours à
 * reprendre (J4), puis la session et la synchronisation (Phase 4). Un échec n'empêche jamais
 * d'ouvrir le menu : l'app passe alors en mode hors ligne.
 */
@Singleton
class StartupLoader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val started = AtomicBoolean(false)
        private val mutableState = MutableStateFlow(IntroLoadState.Loading)
        val state: StateFlow<IntroLoadState> = mutableState.asStateFlow()

        fun start() {
            if (!started.compareAndSet(false, true)) return
            scope.launch {
                mutableState.value =
                    runCatching { preloadFonts() }.fold(
                        onSuccess = { IntroLoadState.Ready },
                        onFailure = {
                            Timber.w(it, "Chargement de démarrage incomplet")
                            IntroLoadState.Failed
                        },
                    )
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
