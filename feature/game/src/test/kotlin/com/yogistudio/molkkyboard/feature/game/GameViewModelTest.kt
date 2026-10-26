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
import com.yogistudio.molkkyboard.core.scoring.ScoringError
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

    @Test
    fun `tour courant et avertissement au deuxieme rate`() =
        runTest {
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            assertEquals(1, vm.awaitState { true }.round)
            play()
            play(1)
            play()
            val second = vm.awaitState { it.round == 2 && it.currentTeam?.name == "Bruno" }
            assertEquals(false, second.missWarning)
            play(1)
            val warned = vm.awaitState { it.round == 3 }
            assertEquals("Alice", warned.currentTeam?.name)
            assertTrue(warned.missWarning)
        }

    @Test
    fun `douze avec la quille 12 seule puis strike des 12 quilles`() =
        runTest {
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.effectFlow.test {
                vm.togglePin(12, true)
                vm.awaitState { it.selection != Selection.None }
                vm.validate()
                assertEquals(GameEffect.Validated("Alice", 12), awaitItem())
                assertEquals(GameEffect.Twelve(strike = false), awaitItem())
                vm.awaitState { it.currentTeam?.name == "Bruno" }
                (1..12).forEach { vm.togglePin(it, true) }
                vm.awaitState { (it.selection as? Selection.Pins)?.pins?.size == 12 }
                vm.validate()
                assertEquals(GameEffect.Validated("Bruno", 12), awaitItem())
                assertEquals(GameEffect.Twelve(strike = true), awaitItem())
            }
        }

    @Test
    fun `un douze gagnant claque avant la victoire, un douze qui depasse ne se celebre pas`() =
        runTest {
            listOf(12, 12, 12).forEach {
                play(it)
                play(1)
            }
            play(1, 2)
            play(1)
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.effectFlow.test {
                vm.togglePin(12, true)
                vm.awaitState { it.selection != Selection.None }
                vm.validate()
                assertEquals(GameEffect.Validated("Alice", 12), awaitItem())
                assertEquals(GameEffect.Twelve(strike = false), awaitItem())
                assertEquals(GameEffect.Won("Alice"), awaitItem())
                vm.undo()
                vm.awaitState { !it.finished }
                play(1, 2, 3, 4, 5, 6, 7)
                play(1)
                vm.awaitState { it.currentTeam?.name == "Alice" && it.teams.first().score == 45 }
                vm.togglePin(12, true)
                vm.awaitState { it.selection != Selection.None }
                vm.validate()
                assertEquals(GameEffect.Validated("Alice", 12), awaitItem())
                assertEquals(GameEffect.Overshoot(25), awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `elimination au troisieme rate puis annulation qui la defait`() =
        runTest {
            play()
            play(1)
            play()
            play(1)
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.effectFlow.test {
                vm.toggleMiss()
                assertEquals(PreviewKind.ELIMINATION, vm.awaitState { it.selection == Selection.Miss }.preview.kind)
                vm.validate()
                assertEquals(GameEffect.Validated("Alice", 0), awaitItem())
                val eliminated = awaitItem() as GameEffect.Eliminated
                assertEquals("Alice", eliminated.team.name)
                assertEquals(3, eliminated.missLimit)
                // Il ne reste que Bruno : l'élimination s'enchaîne avec sa victoire.
                assertEquals(GameEffect.Won("Bruno"), awaitItem())
                vm.awaitState { it.finished }
                vm.undo()
                val cancelled = awaitItem() as GameEffect.EliminationCancelled
                assertEquals("Alice", cancelled.team.name)
            }
            val back = vm.awaitState { !it.finished }
            assertEquals(false, back.teams.first { it.name == "Alice" }.out)
        }

    @Test
    fun `en mode Ludique sans elimination l'apercu du troisieme rate n'elimine pas`() =
        runTest {
            gameId =
                games.create(
                    NewGame(
                        config = GameConfig.Playful,
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
            play()
            play(1)
            play()
            play(1)
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            assertEquals(false, vm.awaitState { it.currentTeam?.name == "Alice" }.missWarning)
            vm.toggleMiss()
            val preview = vm.awaitState { it.selection == Selection.Miss }.preview
            assertEquals(Preview(PreviewKind.MISS, misses = 3), preview)
        }

    @Test
    fun `tableau des scores classe par score, hors jeu en bas`() =
        runTest {
            play(5)
            play(9)
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            val state = vm.awaitState { it.round == 2 }
            assertEquals(listOf("Bruno", "Alice"), state.scoreboard.map { it.name })
            assertEquals(41, state.scoreboard.first().remaining)
            assertEquals(listOf("Alice"), state.teams.first().members)
        }

    /** Joue un lancer et renvoie son identifiant (pour le corriger ou le supprimer ensuite). */
    private suspend fun throwId(vararg pins: Int): EventId {
        val id = EventId("p-${ids.incrementAndGet()}")
        games.append(gameId, Throw(id, ThrowResult.Pins(pins.toList())))
        return id
    }

    @Test
    fun `deroule groupe par tour, avec attribution et depassement`() =
        runTest {
            listOf(12, 12, 12, 9).forEach {
                play(it)
                play(1)
            }
            play(9)
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            val log = vm.awaitState { it.round == 5 }.log
            assertEquals(listOf(1, 2, 3, 4, 5), log.map { it.round })
            assertEquals(listOf("Alice", "Bruno"), log.first().items.map { it.teamName })
            val overshoot = log.last().items.single()
            assertEquals(LogKind.THROW, overshoot.kind)
            assertEquals(45, overshoot.scoreBefore)
            assertEquals(25, overshoot.scoreAfter)
            assertTrue(overshoot.editable)
        }

    @Test
    fun `corriger un ancien lancer recalcule tout, une correction qui finirait la partie plus tot est refusee`() =
        runTest {
            val first = throwId(12)
            play(1)
            listOf(12, 12, 9).forEach {
                play(it)
                play(1)
            }
            val overshoot = throwId(9)
            play(1)
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            assertEquals(
                25,
                vm
                    .awaitState { it.round == 6 }
                    .teams
                    .first()
                    .score,
            )
            vm.effectFlow.test {
                // 45 + 5 = 50 : Alice aurait gagné, et le lancer de Bruno qui suit n'aurait pas eu lieu.
                vm.correct(overshoot, ThrowResult.Pins(listOf(5)))
                assertEquals(GameEffect.EditRefused(ScoringError.CORRECTION_INVALIDATES_LOG), awaitItem())
                vm.correct(first, ThrowResult.Pins(listOf(2)))
                assertEquals(GameEffect.Edited, awaitItem())
            }
            // 2 + 12 + 12 + 9 = 35, puis 9 : 44, le retour à 25 a disparu.
            assertEquals(
                44,
                vm
                    .awaitState { it.teams.first().score == 44 }
                    .teams
                    .first()
                    .score,
            )
        }

    @Test
    fun `supprimer le lancer gagnant rouvre la partie`() =
        runTest {
            listOf(12, 12, 12, 9).forEach {
                play(it)
                play(1)
            }
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.togglePin(5, true)
            vm.awaitState { it.selection != Selection.None }
            vm.effectFlow.test {
                vm.validate()
                assertEquals(GameEffect.Validated("Alice", 5), awaitItem())
                assertEquals(GameEffect.Won("Alice"), awaitItem())
                val finished = vm.awaitState { it.finished }
                // La partie est close juste après la victoire.
                assertNull(games.observeOngoingGameId().first { it == null })
                vm.delete(
                    finished.log
                        .last()
                        .items
                        .last()
                        .id,
                )
                assertEquals(GameEffect.Edited, awaitItem())
            }
            assertEquals("Alice", vm.awaitState { !it.finished }.currentTeam?.name)
            assertEquals(gameId, games.observeOngoingGameId().first())
        }

    @Test
    fun `une correction qui annule une elimination la rejoue a l'envers et rouvre la partie`() =
        runTest {
            val firstMiss = throwId()
            play(1)
            play()
            play(1)
            play()
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.awaitState { it.finished }
            games.close(gameId)
            vm.effectFlow.test {
                vm.correct(firstMiss, ThrowResult.Pins(listOf(3)))
                assertEquals("Alice", (awaitItem() as GameEffect.EliminationCancelled).team.name)
                assertEquals(GameEffect.Edited, awaitItem())
            }
            val state = vm.awaitState { !it.finished }
            assertEquals(3, state.teams.first { it.name == "Alice" }.score)
            assertEquals(gameId, games.observeOngoingGameId().first())
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

        override suspend fun setPlayfulRules(config: GameConfig) =
            settings.emit(
                settings.value.copy(playfulRules = config),
            )

        override suspend fun recordLaunch() = settings.value
    }
}
