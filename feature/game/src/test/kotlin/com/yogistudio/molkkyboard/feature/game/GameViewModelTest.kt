package com.yogistudio.molkkyboard.feature.game

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.yogistudio.molkkyboard.core.data.GamePlayer
import com.yogistudio.molkkyboard.core.data.GameTeam
import com.yogistudio.molkkyboard.core.data.NewGame
import com.yogistudio.molkkyboard.core.data.RoomGameRepository
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.data.memberKey
import com.yogistudio.molkkyboard.core.database.MolkkyBoardDatabase
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.model.UserSettings
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GameViewModelTest {
    private val ids = AtomicInteger()
    private lateinit var database: MolkkyBoardDatabase
    private lateinit var games: RoomGameRepository
    private val settings = FakeSettings()
    private lateinit var gameId: String

    private val alice = PlayerId("alice")
    private val bruno = PlayerId("bruno")

    @Before
    fun setUp() =
        runBlocking {
            Dispatchers.setMain(UnconfinedTestDispatcher())
            val context: Context = ApplicationProvider.getApplicationContext()
            database =
                Room.inMemoryDatabaseBuilder(context, MolkkyBoardDatabase::class.java).allowMainThreadQueries().build()
            games = RoomGameRepository(database.gameDao(), { 1L }, { "id-${ids.incrementAndGet()}" })
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
        database.close()
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        GameViewModel(SavedStateHandle(mapOf(GameViewModel.GAME_ID to gameId)), games, settings, {
            "ev-${ids.incrementAndGet()}"
        })

    private suspend fun play(vararg pins: Int) =
        games.append(
            gameId,
            Throw(EventId("p-${ids.incrementAndGet()}"), ThrowResult.Pins(pins.toList())),
        )

    /** Dernier état qui vérifie [predicate] (les mises à jour de Room arrivent en plusieurs émissions). */
    private suspend fun GameViewModel.awaitState(predicate: (GameUiState) -> Boolean): GameUiState =
        state.first {
            !it.loading &&
                predicate(it)
        }

    @Test
    fun `apercu puis validation d'un lancer`() =
        runTest {
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.togglePin(7, true)
            val preview = vm.awaitState { it.selection != Selection.None }
            assertEquals(Preview(PreviewKind.POINTS, 7, 7), preview.preview)
            assertTrue(preview.canValidate)
            vm.effectFlow.test {
                vm.validate()
                assertEquals(GameEffect.Validated("Alice", 7), awaitItem())
            }
            val after = vm.awaitState { it.currentTeam?.name == "Bruno" }
            assertEquals(7, after.teams.first { it.name == "Alice" }.score)
            assertEquals(Selection.None, after.selection)
        }

    @Test
    fun `rien n'est enregistre sans selection`() =
        runTest {
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.validate()
            assertEquals(
                0,
                games
                    .observe(gameId)
                    .first()!!
                    .currentSet.log.size,
            )
        }

    @Test
    fun `rate selectionne puis valide`() =
        runTest {
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.toggleMiss()
            assertEquals(PreviewKind.MISS, vm.awaitState { it.selection == Selection.Miss }.preview.kind)
            vm.toggleMiss()
            assertEquals(Selection.None, vm.awaitState { it.selection == Selection.None }.selection)
            vm.toggleMiss()
            vm.validate()
            assertEquals(
                1,
                vm
                    .awaitState { it.currentTeam?.name == "Bruno" }
                    .teams
                    .first { it.name == "Alice" }
                    .misses,
            )
        }

    @Test
    fun `depassement retour a 25`() =
        runTest {
            listOf(12, 12, 12, 9).forEach {
                play(it)
                play(1)
            }
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.togglePin(9, true)
            assertEquals(PreviewKind.OVERSHOOT, vm.awaitState { it.selection != Selection.None }.preview.kind)
            vm.effectFlow.test {
                vm.validate()
                assertEquals(GameEffect.Validated("Alice", 9), awaitItem())
                assertEquals(GameEffect.Overshoot(25), awaitItem())
            }
        }

    @Test
    fun `victoire la partie est close puis l'annulation la rouvre`() =
        runTest {
            listOf(12, 12, 12, 9).forEach {
                play(it)
                play(1)
            }
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.togglePin(5, true)
            assertEquals(PreviewKind.WIN, vm.awaitState { it.selection != Selection.None }.preview.kind)
            vm.effectFlow.test {
                vm.validate()
                assertEquals(GameEffect.Validated("Alice", 5), awaitItem())
                assertEquals(GameEffect.Won("Alice"), awaitItem())
            }
            val finished = vm.awaitState { it.finished }
            assertEquals("Alice", finished.winnerName)
            assertNull(games.observeOngoingGameId().first())
            vm.undo()
            vm.awaitState { !it.finished }
            assertEquals(gameId, games.observeOngoingGameId().first())
        }

    @Test
    fun `saisie rapide et changement de mode`() =
        runTest {
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.setInputMode(InputMode.QUICK)
            assertEquals(InputMode.QUICK, vm.awaitState { it.inputMode == InputMode.QUICK }.inputMode)
            vm.setQuickMode(com.yogistudio.molkkyboard.core.designsystem.component.QuickPadMode.PinCount)
            vm.selectQuick(3)
            assertEquals(
                Preview(PreviewKind.POINTS, 3, 3),
                vm.awaitState { it.selection == Selection.Quick(3) }.preview,
            )
            vm.validate()
            assertEquals(
                3,
                vm
                    .awaitState { it.currentTeam?.name == "Bruno" }
                    .teams
                    .first { it.name == "Alice" }
                    .score,
            )
        }

    @Test
    fun `abandon`() =
        runTest {
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.abandon()
            assertTrue(vm.awaitState { it.abandoned }.abandoned)
            assertNull(games.observeOngoingGameId().first())
        }

    private fun CoroutineScope.launchCollect(vm: GameViewModel) =
        launch(
            UnconfinedTestDispatcher(),
        ) { vm.state.collect {} }

    private class FakeSettings : SettingsRepository {
        override val settings = MutableStateFlow(UserSettings())

        override suspend fun setInputMode(mode: InputMode) = settings.emit(settings.value.copy(inputMode = mode))

        override suspend fun setSunlight(enabled: Boolean) = Unit

        override suspend fun setVoiceAnnouncements(enabled: Boolean) = Unit

        override suspend fun setSounds(enabled: Boolean) = Unit

        override suspend fun setCoach(enabled: Boolean) = Unit

        override suspend fun setShortIntro(enabled: Boolean) = Unit

        override suspend fun setOnboardingDone(done: Boolean) = Unit

        override suspend fun recordLaunch() = settings.value
    }
}
