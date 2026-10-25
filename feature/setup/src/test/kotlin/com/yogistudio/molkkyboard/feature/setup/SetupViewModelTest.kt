package com.yogistudio.molkkyboard.feature.setup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yogistudio.molkkyboard.core.data.RoomGameRepository
import com.yogistudio.molkkyboard.core.database.MolkkyBoardDatabase
import com.yogistudio.molkkyboard.core.model.GameConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context: Context = ApplicationProvider.getApplicationContext()
        database =
            Room.inMemoryDatabaseBuilder(context, MolkkyBoardDatabase::class.java).allowMainThreadQueries().build()
        games = RoomGameRepository(database.gameDao(), { 1L }, { "id-${ids.incrementAndGet()}" })
        vm = SetupViewModel(games) { "p-${ids.incrementAndGet()}" }
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
            vm.setPreset(RulesPreset.Tournament)
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
    fun `impossible de lancer avec un seul joueur`() {
        add("Alice")
        var created = false
        vm.start { created = true }
        assertFalse(created)
    }
}
