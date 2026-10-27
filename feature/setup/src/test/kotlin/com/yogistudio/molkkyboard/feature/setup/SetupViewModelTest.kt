package com.yogistudio.molkkyboard.feature.setup

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yogistudio.molkkyboard.core.data.KnownTeam
import com.yogistudio.molkkyboard.core.data.PlayerProfile
import com.yogistudio.molkkyboard.core.data.PlayerRepository
import com.yogistudio.molkkyboard.core.data.RoomGameRepository
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.data.memberKey
import com.yogistudio.molkkyboard.core.database.MolkkyBoardDatabase
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.RulesMode
import com.yogistudio.molkkyboard.core.model.TeamOverride
import com.yogistudio.molkkyboard.core.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
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
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SetupViewModelTest {
    private val ids = AtomicInteger()
    private lateinit var database: MolkkyBoardDatabase
    private lateinit var games: RoomGameRepository
    private lateinit var vm: SetupViewModel
    private lateinit var players: FakePlayers
    private val names = listOf("Les Bouleaux", "Les Élans", "Les Hiboux")
    private val settings = FakeSettings(GameConfig.Playful.copy(targetScore = 40))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context: Context = ApplicationProvider.getApplicationContext()
        database =
            Room.inMemoryDatabaseBuilder(context, MolkkyBoardDatabase::class.java).allowMainThreadQueries().build()
        games = RoomGameRepository(database.gameDao(), { 1L }, { "id-${ids.incrementAndGet()}" })
        players = FakePlayers()
        vm = SetupViewModel(games, settings, players, FakePhotos) { "p-${ids.incrementAndGet()}" }
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
    fun `chaque joueur ajoute devient un profil reutilisable avec un avatar distinct`() =
        runTest {
            add("Alice")
            add("Bruno")
            val state = vm.state.value
            assertEquals(
                2,
                state.players
                    .map { it.avatar }
                    .toSet()
                    .size,
            )
            assertEquals(listOf("Alice", "Bruno"), savedProfiles(2).map { it.name })
            vm.removePlayer(state.players.first().id)
            assertEquals(
                listOf("Alice"),
                awaitState { it.profiles.isNotEmpty() }
                    .profiles
                    .map { it.name },
            )
            vm.addProfile(
                vm.state.value.profiles
                    .first(),
            )
            assertEquals(
                setOf("Alice", "Bruno"),
                vm.state.value.players
                    .map { it.name }
                    .toSet(),
            )
            assertTrue(
                vm.state.value.profiles
                    .isEmpty(),
            )
        }

    @Test
    fun `glisser-deposer de l'ordre de passage`() {
        add("A")
        add("B")
        add("C")
        vm.movePlayer(0, 2)
        assertEquals(
            listOf("B", "C", "A"),
            vm.state.value.units
                .map { it.name },
        )
    }

    @Test
    fun `en equipes, repartition en alternance puis partie creee avec les equipes`() =
        runTest {
            listOf("Alice", "Bruno", "Chloé", "Denis").forEach(::add)
            vm.setPlayMode(PlayMode.TEAMS, names)
            val teams = vm.state.value.teams
            assertEquals(2, teams.size)
            assertEquals(
                listOf(listOf("Alice", "Chloé"), listOf("Bruno", "Denis")),
                teams.map { t ->
                    t.members.map {
                        vm.state.value
                            .player(it)!!
                            .name
                    }
                },
            )
            assertEquals(2, teams.map { it.colorIndex }.toSet().size)
            vm.renameTeam(teams.first().id, "Les Bouleaux")
            vm.moveMember(teams.first().id, 0, 1)
            var created: String? = null
            vm.start { created = it }
            val id = games.observeOngoingGameId().first { it != null }!!
            assertEquals(id, created)
            val game = games.observe(id).first()!!
            assertEquals("Les Bouleaux", game.teams.first().name)
            assertEquals(
                listOf("Chloé", "Alice"),
                game.currentSet.lineup
                    .first()
                    .players
                    .map(game::playerName),
            )
            assertEquals("Les Bouleaux", players.team(game.teams.first().memberKey)?.name)
        }

    @Test
    fun `les memes joueurs regroupes retrouvent leur equipe`() =
        runTest {
            add("Alice")
            add("Bruno")
            val (alice, bruno) =
                vm.state.value.players
                    .map { it.id }
            players.saveTeam(KnownTeam(memberKey(listOf(alice, bruno)), "Les Pives", 5, "leaf"))
            vm.setPlayMode(PlayMode.TEAMS, names)
            vm.switchTeam(bruno)
            val team =
                awaitState { s -> s.teams.any { it.known != null } }
                    .teams
                    .first { it.known != null }
            vm.acceptKnownTeam(team.id)
            val accepted =
                vm.state.value.teams
                    .first { it.id == team.id }
            assertEquals("Les Pives", accepted.name)
            assertEquals(5, accepted.colorIndex)
            assertEquals("leaf", accepted.emblem)
        }

    @Test
    fun `ajouter un joueur directement dans une equipe`() {
        vm.setPlayMode(PlayMode.TEAMS, names)
        val (first, second) = vm.state.value.teams
        vm.addPlayerToTeam(second.id, "  Emma ")
        vm.addPlayerToTeam(second.id, "Félix")
        vm.addPlayerToTeam(first.id, "")
        val teams = vm.state.value.teams
        assertTrue(teams.first().members.isEmpty())
        assertEquals(
            listOf("Emma", "Félix"),
            teams.last().members.map {
                vm.state.value
                    .player(it)!!
                    .name
            },
        )
        vm.addProfileToTeam(first.id, PlayerProfile("z", "Zoé", 3, "🐧"))
        assertEquals(
            listOf(PlayerId("z")),
            vm.state.value.teams
                .first()
                .members,
        )
    }

    @Test
    fun `le mode famille ne s'applique qu'en Ludique`() {
        add("Alice")
        add("Bruno")
        val alice =
            vm.state.value.units
                .first()
                .id
        vm.setFamily(alice, TeamOverride(startScore = 20, exemptFromMissLimit = true))
        assertEquals(GameConfig.Classic, vm.state.value.config)
        vm.setMode(RulesMode.PLAYFUL)
        assertEquals(
            mapOf(alice to TeamOverride(startScore = 20, exemptFromMissLimit = true)),
            vm.state.value.config.teamOverrides,
        )
        assertEquals(TeamOverride(startScore = 34), TeamOverride().withStartScore(60, 35))
        assertEquals(TeamOverride(targetScore = 30, startScore = 29), TeamOverride(startScore = 40).withTarget(30, 50))
        assertEquals(TeamOverride(), TeamOverride(targetScore = 30).withTarget(50, 50))
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

        override suspend fun setVoiceRate(rate: Float) = Unit

        override suspend fun setVoiceVolume(volume: Float) = Unit

        override suspend fun setOngoingNotification(enabled: Boolean) = Unit

        override suspend fun setNotificationPrompted() = Unit

        override suspend fun setCoach(enabled: Boolean) = Unit

        override suspend fun setShortIntro(enabled: Boolean) = Unit

        override suspend fun setOnboardingDone(done: Boolean) = Unit

        override suspend fun setPlayfulRules(config: GameConfig) =
            settings.emit(
                settings.value.copy(playfulRules = config),
            )

        override suspend fun recordLaunch() = settings.value
    }

    /** Les lectures de Room se terminent sur un autre fil : on relit l'état jusqu'à la condition (au plus 2 s). */
    private fun TestScope.awaitState(predicate: (SetupUiState) -> Boolean): SetupUiState {
        repeat(POLLS) {
            // Les flux Room réémettent via la boucle principale (Robolectric) puis l'ordonnanceur des tests.
            shadowOf(Looper.getMainLooper()).idle()
            testScheduler.runCurrent()
            if (predicate(vm.state.value)) return vm.state.value
            Thread.sleep(POLL_MS)
        }
        return vm.state.value
    }

    /** Les profils sont écrits en arrière-plan : on relit la base jusqu'à les voir (au plus 2 s). */
    private suspend fun savedProfiles(count: Int): List<PlayerProfile> {
        repeat(POLLS) {
            val list = players.profiles.first()
            if (list.size >= count) return list
            Thread.sleep(POLL_MS)
        }
        return players.profiles.first()
    }

    /** Dépôt de profils en mémoire : les flux Room ne réémettent pas de façon fiable sous Robolectric. */
    private class FakePlayers : PlayerRepository {
        private val saved = MutableStateFlow<Map<String, PlayerProfile>>(emptyMap())
        private val teams = mutableMapOf<String, KnownTeam>()

        override val profiles = saved.map { map -> map.values.sortedBy { it.name.lowercase() } }

        override suspend fun saveProfile(profile: PlayerProfile) = saved.update { it + (profile.id to profile) }

        override suspend fun archiveProfile(id: String) = saved.update { it - id }

        override suspend fun team(memberKey: String): KnownTeam? = teams[memberKey]

        override suspend fun saveTeam(team: KnownTeam) {
            teams[team.memberKey] = team
        }
    }

    private object FakePhotos : PhotoStore {
        override fun captureUri(): Uri = Uri.EMPTY

        override suspend fun import(source: Uri, playerId: String): String = "file:///$playerId.jpg"

        override suspend fun delete(playerId: String) = Unit
    }

    private companion object {
        const val POLLS = 40
        const val POLL_MS = 50L
    }
}
