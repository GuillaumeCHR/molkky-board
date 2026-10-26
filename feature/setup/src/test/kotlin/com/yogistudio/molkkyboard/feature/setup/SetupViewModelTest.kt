package com.yogistudio.molkkyboard.feature.setup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yogistudio.molkkyboard.core.data.RoomGameRepository
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.database.MolkkyBoardDatabase
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.RulesMode
import com.yogistudio.molkkyboard.core.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SetupViewModelTest {
    private val ids = AtomicInteger()
    private lateinit var database: MolkkyBoardDatabase
    private lateinit var games: RoomGameRepository
    private lateinit var vm: SetupViewModel
    private val settings = FakeSettings(GameConfig.Playful.copy(targetScore = 40))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context: Context = ApplicationProvider.getApplicationContext()
        database =
            Room.inMemoryDatabaseBuilder(context, MolkkyBoardDatabase::class.java).allowMainThreadQueries().build()
        games = RoomGameRepository(database.gameDao(), { 1L }, { "id-${ids.incrementAndGet()}" })
        vm = SetupViewModel(games, settings) { "p-${ids.incrementAndGet()}" }
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun add(name: String) {
        vm.onNameChange(name)
        vm.addPlayer()
    }

    @Test
    fun `ajouter et retirer des joueurs avec des couleurs distinctes`() {
        add("  Alice ")
        add("")
        add("Bruno")
        val state = vm.state.value
        assertEquals(listOf("Alice", "Bruno"), state.players.map { it.name })
        assertEquals(listOf(0, 1), state.players.map { it.colorIndex })
        assertTrue(state.canStart)
        vm.removePlayer(state.players.first().id)
        assertFalse(vm.state.value.canStart)
        add("Chloé")
        assertEquals(
            listOf(1, 0),
            vm.state.value.players
                .map { it.colorIndex },
        )
    }

    @Test
    fun `nom limite a 20 caracteres`() {
        vm.onNameChange("x".repeat(40))
        assertEquals(SetupUiState.MAX_NAME, vm.state.value.draftName.length)
    }

    @Test
    fun `ordre au hasard conserve les joueurs`() {
        listOf("A", "B", "C", "D").forEach(::add)
        vm.shuffle()
        assertEquals(
            setOf("A", "B", "C", "D"),
            vm.state.value.players
                .map { it.name }
                .toSet(),
        )
    }

    @Test
    fun `lancer la partie cree la partie avec ses joueurs et ses regles`() =
        runTest {
            add("Alice")
            add("Bruno")
            vm.setImo(true)
            var created: String? = null
            vm.start { created = it }
            val id = games.observeOngoingGameId().first { it != null }
            assertEquals(id, created)
            val game = games.observe(id!!).first()!!
            assertEquals(GameConfig.ImoTournament, game.config)
            assertEquals(listOf("Alice", "Bruno"), game.teams.map { it.name })
            assertNotNull(game.currentSet.replay.state.currentPlayer)
            assertEquals("Alice", game.playerName(game.currentSet.replay.state.currentPlayer!!))
        }

    @Test
    fun `mode Officiel par defaut, Ludique avec les exceptions des reglages`() {
        assertEquals(RulesMode.OFFICIAL, vm.state.value.mode)
        assertEquals(GameConfig.Classic, vm.state.value.config)
        vm.setMode(RulesMode.PLAYFUL)
        assertEquals(GameConfig.Playful.copy(targetScore = 40), vm.state.value.config)
        vm.setMode(RulesMode.OFFICIAL)
        vm.setImo(true)
        assertEquals(GameConfig.ImoTournament, vm.state.value.config)
    }

    @Test
    fun `exceptions modifiees pour une seule partie`() =
        runTest {
            vm.setMode(RulesMode.PLAYFUL)
            vm.setPlayful(GameConfig.Playful.copy(catchUpReset = true))
            assertEquals(GameConfig.Playful.copy(catchUpReset = true), vm.state.value.config)
            assertEquals(GameConfig.Playful.copy(targetScore = 40), settings.settings.first().playfulRules)
        }

    @Test
    fun `impossible de lancer avec un seul joueur`() {
        add("Alice")
        var created = false
        vm.start { created = true }
        assertFalse(created)
    }

    private class FakeSettings(
        playful: GameConfig,
    ) : SettingsRepository {
        override val settings = MutableStateFlow(UserSettings(playfulRules = playful))

        override suspend fun setInputMode(mode: InputMode) = Unit

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
