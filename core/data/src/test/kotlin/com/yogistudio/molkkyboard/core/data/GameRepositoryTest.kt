package com.yogistudio.molkkyboard.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.yogistudio.molkkyboard.core.database.MolkkyBoardDatabase
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.Correct
import com.yogistudio.molkkyboard.core.scoring.Delete
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import com.yogistudio.molkkyboard.core.scoring.Outcome
import com.yogistudio.molkkyboard.core.scoring.ScoringError
import com.yogistudio.molkkyboard.core.scoring.SetStatus
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import com.yogistudio.molkkyboard.core.scoring.Undo
import com.yogistudio.molkkyboard.core.scoring.errorOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class GameRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = AtomicInteger(1_000)
    private val ids = AtomicInteger()
    private lateinit var database: MolkkyBoardDatabase
    private lateinit var repository: GameRepository

    private val alice = PlayerId("alice")
    private val bruno = PlayerId("bruno")
    private val newGame =
        NewGame(
            config = GameConfig.Classic,
            matchConfig = MatchConfig.Classic,
            lineup = listOf(TeamLineup(TeamId("A"), listOf(alice)), TeamLineup(TeamId("B"), listOf(bruno))),
            teams =
                listOf(
                    GameTeam(TeamId("A"), "Alice", 0, memberKey = memberKey(listOf(alice))),
                    GameTeam(TeamId("B"), "Bruno", 1, memberKey = memberKey(listOf(bruno))),
                ),
            players = listOf(GamePlayer(alice, "Alice"), GamePlayer(bruno, "Bruno")),
        )

    private fun open(name: String? = null): MolkkyBoardDatabase =
        (
            if (name ==
                null
            ) {
                Room.inMemoryDatabaseBuilder(context, MolkkyBoardDatabase::class.java)
            } else {
                Room.databaseBuilder(context, MolkkyBoardDatabase::class.java, name)
            }
        ).allowMainThreadQueries()
            .build()

    private fun repositoryOn(db: MolkkyBoardDatabase) =
        RoomGameRepository(db.gameDao(), { clock.incrementAndGet().toLong() }, { "id-${ids.incrementAndGet()}" })

    @Before
    fun setUp() {
        database = open()
        repository = repositoryOn(database)
    }

    @After
    fun tearDown() = database.close()

    private fun pins(vararg pins: Int) = Throw(EventId("e-${ids.incrementAndGet()}"), ThrowResult.Pins(pins.toList()))

    @Test
    fun `creer une partie puis la suivre en direct`() =
        runTest {
            val id = repository.create(newGame)
            repository.observe(id).test {
                val created = awaitItem()!!
                assertEquals(listOf("Alice", "Bruno"), created.teams.map { it.name })
                assertEquals("Alice", created.playerName(alice))
                assertEquals("inconnu", created.playerName(PlayerId("inconnu")))
                assertEquals(TeamId("A"), created.currentSet.replay.state.turn)
                repository.append(id, pins(7))
                // Plusieurs tables changent dans la transaction : on attend l'état final.
                var item = awaitItem()!!
                while (item.currentSet.log.isEmpty()) item = awaitItem()!!
                assertEquals(
                    7,
                    item.currentSet.replay.state
                        .team(TeamId("A"))!!
                        .score,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `un evenement refuse n'est pas ecrit`() =
        runTest {
            val id = repository.create(newGame)
            assertEquals(ScoringError.INVALID_PIN, repository.append(id, pins(13)).errorOrNull())
            assertEquals(ScoringError.NOT_YOUR_TURN, repository.append(id, pins(4).copy(by = bruno)).errorOrNull())
            assertEquals(
                ScoringError.NOTHING_TO_UNDO,
                repository.append(id, Undo(EventId("u"), EventId("x"))).errorOrNull(),
            )
            assertEquals(
                0,
                repository
                    .observe(id)
                    .first()!!
                    .currentSet.log.size,
            )
        }

    @Test
    fun `annuler corriger et supprimer passent par le journal`() =
        runTest {
            val id = repository.create(newGame)
            val first = pins(5)
            repository.append(id, first)
            val second = pins(4)
            repository.append(id, second)
            repository.append(id, Correct(EventId("c"), first.id, pins(9)))
            repository.append(id, Undo(EventId("u"), second.id))
            val snapshot = repository.observe(id).first()!!.currentSet
            assertEquals(4, snapshot.log.size)
            assertEquals(
                9,
                snapshot.replay.state
                    .team(TeamId("A"))!!
                    .score,
            )
            assertEquals(TeamId("B"), snapshot.replay.state.turn)
            repository.append(id, Delete(EventId("d"), first.id))
            assertEquals(
                0,
                repository
                    .observe(id)
                    .first()!!
                    .currentSet.replay.state
                    .team(TeamId("A"))!!
                    .score,
            )
        }

    @Test
    fun `partie en cours manche suivante cloture et suppression`() =
        runTest {
            val id = repository.create(newGame)
            assertEquals(id, repository.observeOngoingGameId().first())
            repository.startNextSet(id, newGame.lineup.reversed())
            val sets = repository.observe(id).first()!!.sets
            assertEquals(listOf(0, 1), sets.map { it.index })
            assertEquals(
                TeamId("B"),
                sets
                    .last()
                    .replay.state.turn,
            )
            repository.close(id)
            assertNull(repository.observeOngoingGameId().first())
            repository.reopen(id)
            assertEquals(id, repository.observeOngoingGameId().first())
            repository.delete(id)
            assertNull(repository.observe(id).first())
            assertEquals(ScoringError.UNKNOWN_EVENT, repository.append(id, pins(1)).errorOrNull())
        }

    @Test
    fun `reprise apres la mort du processus aucun lancer perdu`() =
        runTest {
            val name = "reprise.db"
            context.deleteDatabase(name)
            val firstRun = open(name)
            val id =
                repositoryOn(firstRun).run {
                    val gameId = create(newGame)
                    listOf(12, 12, 12).forEach { value ->
                        append(gameId, pins(value))
                        append(gameId, pins())
                    }
                    gameId
                }
            val before = repositoryOn(firstRun).observe(id).first()!!
            // Le processus meurt : la base est fermée, puis rouverte depuis son fichier par une nouvelle instance.
            firstRun.close()
            val secondRun = open(name)
            val restored = repositoryOn(secondRun)
            assertEquals(id, restored.observeOngoingGameId().first())
            val after = restored.observe(id).first()!!
            assertEquals(before.currentSet.log, after.currentSet.log)
            assertEquals(before.currentSet.replay.state, after.currentSet.replay.state)
            assertEquals(
                36,
                after.currentSet.replay.state
                    .team(TeamId("A"))!!
                    .score,
            )
            secondRun.close()
        }

    @Test
    fun `fin de manche visible dans l'instantane`() =
        runTest {
            val id = repository.create(newGame)
            listOf(12, 12, 12, 12, 2).forEach {
                repository.append(id, pins(it))
                if (repository
                        .observe(id)
                        .first()!!
                        .currentSet.replay.state.status == SetStatus.IN_PROGRESS
                ) {
                    repository.append(id, pins(1))
                }
            }
            val result =
                repository
                    .observe(id)
                    .first()!!
                    .currentSet.replay
            assertEquals(SetStatus.FINISHED, result.state.status)
            assertEquals(TeamId("A"), result.state.winner)
        }

    @Test
    fun `l'identite d'equipe ne depend pas de l'ordre des joueurs`() {
        assertEquals(memberKey(listOf(alice, bruno)), memberKey(listOf(bruno, alice)))
        assertNotEquals(memberKey(listOf(alice)), memberKey(listOf(alice, bruno)))
    }

    @Test
    fun `append renvoie l'etat recalcule`() =
        runTest {
            val id = repository.create(newGame)
            val outcome = repository.append(id, pins(3))
            assertEquals(
                3,
                (outcome as Outcome.Ok)
                    .value.state
                    .team(TeamId("A"))!!
                    .score,
            )
        }

    @Test
    fun `un retardataire rejoint la partie avec son nom`() =
        runTest {
            val id = repository.create(newGame)
            val chloe = PlayerId("chloe")
            val team = GameTeam(TeamId("C"), "Chloé", 2, memberKey = memberKey(listOf(chloe)))
            val event =
                com.yogistudio.molkkyboard.core.scoring
                    .AddPlayer(EventId("add"), chloe, TeamId("C"))
            assertEquals(true, repository.addParticipant(id, event, GamePlayer(chloe, "Chloé"), team) is Outcome.Ok)
            val game = repository.observe(id).first()!!
            assertEquals("Chloé", game.playerName(chloe))
            assertEquals("Chloé", game.team(TeamId("C"))!!.name)
            // Un ajout refusé (joueur déjà présent) n'enregistre rien.
            val again =
                com.yogistudio.molkkyboard.core.scoring
                    .AddPlayer(EventId("add2"), chloe, TeamId("D"))
            assertEquals(
                ScoringError.PLAYER_IN_MULTIPLE_TEAMS,
                repository.addParticipant(id, again, GamePlayer(chloe, "X"), team.copy(id = TeamId("D"))).errorOrNull(),
            )
            assertNull(repository.observe(id).first()!!.team(TeamId("D")))
        }
}
