package com.yogistudio.molkkyboard.feature.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.yogistudio.molkkyboard.core.data.CommandGateway
import com.yogistudio.molkkyboard.core.data.GamePlayer
import com.yogistudio.molkkyboard.core.data.GameTeam
import com.yogistudio.molkkyboard.core.data.JournalStatsRepository
import com.yogistudio.molkkyboard.core.data.NewGame
import com.yogistudio.molkkyboard.core.data.RoomGameRepository
import com.yogistudio.molkkyboard.core.data.RoomPlayerRepository
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.data.memberKey
import com.yogistudio.molkkyboard.core.database.MolkkyBoardDatabase
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.model.UserSettings
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import com.yogistudio.molkkyboard.core.scoring.NextStarter
import com.yogistudio.molkkyboard.core.scoring.ScoringError
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

// Première partie guidée (prise en main en 4 bulles, dont 3 sur l'écran de jeu).
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
class GuidedFirstGameTest {
    @get:Rule
    val compose = createComposeRule()

    private val ids = AtomicInteger()
    private lateinit var database: MolkkyBoardDatabase
    private lateinit var games: RoomGameRepository
    private val settings = FakeSettings()
    private lateinit var gameId: String
    private val clock = AtomicLong()

    private val alice = PlayerId("alice")
    private val bruno = PlayerId("bruno")

    @Before
    fun setUp() =
        runBlocking {
            Dispatchers.setMain(UnconfinedTestDispatcher())
            val context: Context = ApplicationProvider.getApplicationContext()
            database =
                Room.inMemoryDatabaseBuilder(context, MolkkyBoardDatabase::class.java).allowMainThreadQueries().build()
            games =
                RoomGameRepository(database.gameDao(), { clock.incrementAndGet() }, { "id-${ids.incrementAndGet()}" })
            gameId =
                games.create(
                    NewGame(
                        config = GameConfig.Classic,
                        matchConfig = MatchConfig(setsToWin = 1),
                        lineup = listOf(TeamLineup(TeamId("A"), listOf(alice)), TeamLineup(TeamId("B"), listOf(bruno))),
                        teams =
                            listOf(
                                GameTeam(TeamId("A"), "Alice", 0, memberKey = memberKey(listOf(alice))),
                                GameTeam(TeamId("B"), "Bruno", 1, memberKey = memberKey(listOf(bruno))),
                            ),
                        players = listOf(GamePlayer(alice, "Alice"), GamePlayer(bruno, "Bruno")),
                    ),
                )
        }

    @After
    fun tearDown() {
        // Les coroutines des ViewModels (annonces, record) se terminent avant la fermeture de la base.
        runBlocking { viewModels.forEach { it.viewModelScope.coroutineContext[Job]?.cancelAndJoin() } }
        database.close()
        Dispatchers.resetMain()
    }

    private val viewModels = mutableListOf<GameViewModel>()

    private fun viewModel() =
        GameViewModel(
            SavedStateHandle(mapOf(GameViewModel.GAME_ID to gameId)),
            games,
            CommandGateway(games, Optional.empty()),
            settings,
            { "ev-${ids.incrementAndGet()}" },
            JournalStatsRepository(games),
            RoomPlayerRepository(database.playerDao()) { 1L },
        ).also(viewModels::add)

    /**
     * Première partie guidée, côté écran de jeu : la bulle des quilles ne bloque pas la grille, celle de
     * « Valider » non plus ; deux taps suffisent pour le premier lancer validé, puis « Compris ! » clôt le guide.
     * (Avec l'étape de création — deux pseudos et « Lancer la partie » — on reste loin des 30 secondes.)
     */
    @Test
    fun `premier lancer valide en trois taps (C'est parti, quille, Valider), sans que bulles ni carte ne bloquent`() =
        runTest {
            val vm = viewModel()
            compose.setContent {
                val state by vm.state.collectAsStateWithLifecycle()
                MolkkyBoardTheme(reducedMotion = true) {
                    GameContent(
                        state,
                        GameActions(
                            onTogglePin = vm::togglePin,
                            onValidate = vm::validate,
                            onGuideDone = vm::finishGuide,
                            onStartPlaying = vm::startPlaying,
                        ),
                    )
                }
            }
            // Première partie : la carte « Prêts à jouer ? » s'affiche, puis les bulles.
            compose.waitUntil(TIMEOUT_MS) { vm.state.value.readyCard }
            compose.onNodeWithText("C'est parti").performClick()
            compose.waitUntil(TIMEOUT_MS) { !vm.state.value.readyCard }
            compose.waitUntil(TIMEOUT_MS) { vm.state.value.guide == GuideStep.PINS }
            compose.onNodeWithText("un second tap relève une quille", substring = true).assertIsDisplayed()
            compose.onNodeWithContentDescription("Quille 7, debout").performClick()
            compose.waitUntil(TIMEOUT_MS) { vm.state.value.guide == GuideStep.VALIDATE }
            compose.onNodeWithText("« Valider » enregistre", substring = true).assertIsDisplayed()
            compose.onNodeWithText("Valider").performClick()
            compose.waitUntil(TIMEOUT_MS) { vm.state.value.guide == GuideStep.UNDO }
            assertEquals(
                7,
                games
                    .observe(gameId)
                    .first()!!
                    .currentSet.replay.state.teams
                    .first()
                    .score,
            )
            compose.onNodeWithText("Compris !").performClick()
            compose.waitUntil(TIMEOUT_MS) { settings.settings.value.onboardingDone }
            compose.waitUntil(TIMEOUT_MS) { vm.state.value.guide == null }
        }

    private class FakeSettings : SettingsRepository {
        override val settings = MutableStateFlow(UserSettings())

        override suspend fun setInputMode(mode: InputMode) = settings.emit(settings.value.copy(inputMode = mode))

        override suspend fun setSunlight(enabled: Boolean) = Unit

        override suspend fun setVoiceAnnouncements(enabled: Boolean) = Unit

        override suspend fun setSounds(enabled: Boolean) = Unit

        override suspend fun setVoiceRate(rate: Float) = Unit

        override suspend fun setVoiceVolume(volume: Float) = Unit

        override suspend fun setOngoingNotification(enabled: Boolean) = Unit

        override suspend fun setNotificationPrompted() = Unit

        override suspend fun setOnboardingDone(done: Boolean) =
            settings.emit(
                settings.value.copy(onboardingDone = done),
            )

        override suspend fun setPlayfulRules(config: GameConfig) =
            settings.emit(
                settings.value.copy(playfulRules = config),
            )

        override suspend fun setReadyCard(enabled: Boolean) = settings.emit(settings.value.copy(readyCard = enabled))

        override suspend fun recordGameStarted() =
            settings.emit(settings.value.copy(gamesStarted = settings.value.gamesStarted + 1))

        override suspend fun recordLaunch() = settings.value
    }
}

private const val TIMEOUT_MS = 5_000L
