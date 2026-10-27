package com.yogistudio.molkkyboard.feature.game

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.yogistudio.molkkyboard.core.data.GamePlayer
import com.yogistudio.molkkyboard.core.data.GameTeam
import com.yogistudio.molkkyboard.core.data.JournalStatsRepository
import com.yogistudio.molkkyboard.core.data.NewGame
import com.yogistudio.molkkyboard.core.data.RoomGameRepository
import com.yogistudio.molkkyboard.core.data.RoomPlayerRepository
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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

// Fin de partie (J5) : victoire, record, série, match, revanche, annonces vocales.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GameEndViewModelTest {
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
            settings,
            { "ev-${ids.incrementAndGet()}" },
            JournalStatsRepository(games),
            RoomPlayerRepository(database.playerDao()) { 1L },
        ).also(viewModels::add)

    /** Effets visuels seulement : les annonces vocales ont leurs propres tests. */
    private fun GameViewModel.gameEffects() = effectFlow.filterNot { it is GameEffect.Announce }

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

    /** Alice gagne en 5 lancers (12, 12, 12, 12 puis 2) ; Bruno fait 1 à chaque fois. */
    private suspend fun perfectUntilLastThrow() =
        repeat(4) {
            play(12)
            play(1)
        }

    @Test
    fun `fin de manche - partie parfaite, statistiques, revanche ouverte avec le vainqueur en dernier`() =
        runTest {
            perfectUntilLastThrow()
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.togglePin(2, true)
            vm.awaitState { it.selection != Selection.None }
            vm.gameEffects().test {
                vm.validate()
                awaitItem()
                val won = awaitItem() as GameEffect.Won
                assertEquals(50, won.target)
                assertTrue(won.perfect)
                // Toute première victoire : pas de record annoncé.
                assertFalse(won.newRecord)
            }
            val end = vm.awaitState { it.end != null }.end!!
            assertEquals("Alice", end.winner?.name)
            assertEquals(5, end.winnerThrows)
            assertTrue(end.perfect)
            assertEquals(listOf(5, 4), end.teamStats.map { it.throws })
            assertNull(end.match)
            vm.gameEffects().test {
                vm.rematch(NextStarter.WinnerLast)
                val open = awaitItem() as GameEffect.OpenGame
                val rematch = games.observe(open.gameId).first()!!
                assertEquals(listOf("B", "A"), rematch.currentSet.lineup.map { it.id.value })
            }
        }

    @Test
    fun `un record battu est annonce et rappele sur l'ecran de fin, la serie apparait`() =
        runTest {
            // Une première partie gagnée en 9 lancers par Alice, close.
            val slow =
                games.create(
                    games.observe(gameId).first()!!.let {
                        NewGame(it.config, it.matchConfig, it.currentSet.lineup, it.teams, it.players)
                    },
                )
            repeat(8) {
                games.append(slow, Throw(EventId("s-$it"), ThrowResult.Pins(listOf(6))))
                games.append(slow, Throw(EventId("t-$it"), ThrowResult.Pins(listOf(1))))
            }
            games.append(slow, Throw(EventId("s-last"), ThrowResult.Pins(listOf(2))))
            games.close(slow)
            // La partie en cours est créée après : la série les enchaîne dans l'ordre.
            gameId =
                games.create(
                    games.observe(slow).first()!!.let {
                        NewGame(it.config, it.matchConfig, it.currentSet.lineup, it.teams, it.players)
                    },
                )
            perfectUntilLastThrow()
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.togglePin(2, true)
            vm.awaitState { it.selection != Selection.None }
            vm.gameEffects().test {
                vm.validate()
                awaitItem()
                assertTrue((awaitItem() as GameEffect.Won).newRecord)
            }
            val end = vm.awaitState { it.end?.newRecord == true && it.end?.series != null }.end!!
            assertEquals(2, end.series!!.sets)
            assertEquals(
                listOf(2),
                end.series.rows
                    .map { it.wins }
                    .filter { it > 0 },
            )
        }

    @Test
    fun `match en 2 manches - la partie reste ouverte, la manche suivante commence par le perdant`() =
        runTest {
            val match =
                games.create(
                    games.observe(gameId).first()!!.let {
                        NewGame(it.config, MatchConfig(setsToWin = 2), it.currentSet.lineup, it.teams, it.players)
                    },
                )
            gameId = match
            perfectUntilLastThrow()
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.togglePin(2, true)
            vm.awaitState { it.selection != Selection.None }
            vm.validate()
            val end = vm.awaitState { it.end?.match != null }.end!!
            assertEquals(2, end.match!!.nextSet)
            assertEquals(listOf(1, 0), end.match.wins.map { it.second })
            // Le match continue : la partie reste « en cours » à l'accueil.
            assertEquals(match, games.observeOngoingGameId().first())
            vm.nextSet()
            val second = vm.awaitState { !it.finished && it.round == 1 && it.end == null }
            assertEquals("Bruno", second.currentTeam?.name)
        }

    @Test
    fun `annonces vocales - douze, puis tour suivant avec ce qu'il faut`() =
        runTest {
            listOf(12, 12, 12).forEach {
                play(it)
                play(1)
            }
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            vm.togglePin(12, true)
            vm.awaitState { it.selection != Selection.None }
            vm.effectFlow.filterIsInstance<GameEffect.Announce>().test {
                vm.validate()
                assertEquals(Announcement.Twelve, awaitItem().text)
                assertEquals(Announcement.Turn("Bruno", null), awaitItem().text)
            }
        }

    @Test
    fun `coach - a 38 points il faut 12, la quille 12 est surlignee, desactivable`() =
        runTest {
            listOf(12, 12, 12).forEach {
                play(it)
                play(1)
            }
            play(2)
            play(1)
            val vm = viewModel()
            backgroundScope.launchCollect(vm)
            val coach = vm.awaitState { it.coach != null }.coach!!
            assertEquals(12, coach.needed)
            assertFalse(coach.danger)
            settings.setCoach(false)
            vm.awaitState { it.coach == null }
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

        override suspend fun setVoiceRate(rate: Float) = Unit

        override suspend fun setVoiceVolume(volume: Float) = Unit

        override suspend fun setOngoingNotification(enabled: Boolean) = Unit

        override suspend fun setNotificationPrompted() = Unit

        override suspend fun setCoach(enabled: Boolean) = settings.emit(settings.value.copy(coach = enabled))

        override suspend fun setShortIntro(enabled: Boolean) = Unit

        override suspend fun setOnboardingDone(done: Boolean) = Unit

        override suspend fun setPlayfulRules(config: GameConfig) =
            settings.emit(
                settings.value.copy(playfulRules = config),
            )

        override suspend fun setReadyCard(enabled: Boolean) = settings.emit(settings.value.copy(readyCard = enabled))

        override suspend fun recordGameStarted() =
            settings.emit(settings.value.copy(gamesStarted = settings.value.gamesStarted + 1))

        override suspend fun setBeginnerMode(enabled: Boolean) =
            settings.emit(
                settings.value.copy(beginnerMode = enabled),
            )

        override suspend fun setBeginnerTipSeen(tip: com.yogistudio.molkkyboard.core.model.BeginnerTip) =
            settings.emit(settings.value.copy(beginnerTipsSeen = settings.value.beginnerTipsSeen + tip))

        override suspend fun recordLaunch() = settings.value
    }
}
