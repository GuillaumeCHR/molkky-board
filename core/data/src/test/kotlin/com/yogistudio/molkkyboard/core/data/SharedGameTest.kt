package com.yogistudio.molkkyboard.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yogistudio.molkkyboard.core.database.MolkkyBoardDatabase
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.AddPlayer
import com.yogistudio.molkkyboard.core.scoring.Correct
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import com.yogistudio.molkkyboard.core.scoring.NextStarter
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import com.yogistudio.molkkyboard.core.sync.CommandOutcome
import com.yogistudio.molkkyboard.core.sync.ConnectionId
import com.yogistudio.molkkyboard.core.sync.GameCommand
import com.yogistudio.molkkyboard.core.sync.GuestSession
import com.yogistudio.molkkyboard.core.sync.HostSession
import com.yogistudio.molkkyboard.core.sync.Outgoing
import com.yogistudio.molkkyboard.core.sync.RejectCode
import com.yogistudio.molkkyboard.core.sync.SharedPlayer
import com.yogistudio.molkkyboard.core.sync.SharedTeam
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

/**
 * Partie à côté de bout en bout, sans réseau : deux téléphones (deux bases Room), l'hôte comme autorité, l'invité qui
 * n'écrit que ce qui est diffusé. Les deux parties doivent rester identiques.
 */
@RunWith(RobolectricTestRunner::class)
class SharedGameTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = AtomicInteger(1_000)
    private val ids = AtomicInteger()
    private val databases = mutableListOf<MolkkyBoardDatabase>()
    private val alice = PlayerId("alice")
    private val bruno = PlayerId("bruno")

    private fun repository(): GameRepository {
        val db =
            Room
                .inMemoryDatabaseBuilder(context, MolkkyBoardDatabase::class.java)
                .allowMainThreadQueries()
                .build()
                .also(databases::add)
        return RoomGameRepository(db.gameDao(), { clock.incrementAndGet().toLong() }) { "id-${ids.incrementAndGet()}" }
    }

    @After
    fun tearDown() = databases.forEach { it.close() }

    private val hostGames = repository()
    private val guestGames = repository()
    private lateinit var authority: GameAuthority
    private lateinit var host: HostSession
    private val store = GuestGameStore(guestGames)
    private val guest = GuestSession(store) { "g-${ids.incrementAndGet()}" }
    private val connection = ConnectionId(1)

    private suspend fun deliver(outgoing: List<Outgoing>) {
        outgoing.forEach { out ->
            if (out is Outgoing.To || out is Outgoing.Broadcast) {
                guest.receive(out.message).forEach { deliver(host.receive(connection, it)) }
            }
        }
    }

    private fun pins(vararg pins: Int, by: PlayerId? = null) =
        Throw(EventId("e-${ids.incrementAndGet()}"), ThrowResult.Pins(pins.toList()), by)

    private suspend fun start(): String {
        val id =
            hostGames.create(
                NewGame(
                    config = GameConfig.Classic,
                    matchConfig = MatchConfig(setsToWin = 2),
                    lineup = listOf(TeamLineup(TeamId("A"), listOf(alice)), TeamLineup(TeamId("B"), listOf(bruno))),
                    teams =
                        listOf(
                            GameTeam(TeamId("A"), "Alice", 0, memberKey = memberKey(listOf(alice))),
                            GameTeam(TeamId("B"), "Bruno", 1, memberKey = memberKey(listOf(bruno))),
                        ),
                    players =
                        listOf(
                            GamePlayer(alice, "Alice", profileId = "profil-alice"),
                            GamePlayer(bruno, "Bruno"),
                        ),
                ),
            )
        // Partie commencée sur un seul téléphone, puis partagée.
        hostGames.append(id, pins(12))
        hostGames.setOrigin(id, GameOrigin.HOSTED)
        authority = GameAuthority(hostGames, id) { mapOf("profil-alice" to "leaf") }
        host = HostSession(authority, clock = { clock.get().toLong() })
        deliver(host.receive(connection, guest.joinRequest(host.newInvite(), "Bruno", null)))
        deliver(host.accept(connection, bruno))
        return id
    }

    private suspend fun assertSame(id: String) {
        val a = hostGames.observe(id).first()!!
        val b = guestGames.observe(id).first()!!
        assertEquals(a.sets.map { it.log }, b.sets.map { it.log })
        assertEquals(a.currentSet.replay.state, b.currentSet.replay.state)
        assertEquals(a.teams, b.teams)
        assertEquals(a.players.map { it.id to it.name }, b.players.map { it.id to it.name })
        assertEquals(a.molkkout, b.molkkout)
    }

    private fun baseSeq(id: String) =
        kotlinx.coroutines.runBlocking {
            guestGames
                .observe(id)
                .first()!!
                .currentSet.log.size
        }

    private suspend fun guestPlays(id: String, command: GameCommand): CommandOutcome? {
        val proposal = guest.propose(command)
        deliver(host.receive(connection, proposal))
        return guest.outcomes[proposal.id]
    }

    @Test
    fun `l'invite recoit la partie commencee, sans photo ni profil, et la voit comme rejointe`() =
        runTest {
            val id = start()
            val joined = guestGames.observe(id).first()!!
            assertEquals(GameOrigin.JOINED, joined.origin)
            assertEquals(GameOrigin.HOSTED, hostGames.observe(id).first()!!.origin)
            assertTrue(joined.players.all { it.profileId == null })
            assertEquals(
                "leaf",
                hostGames
                    .share(id, mapOf("profil-alice" to "leaf"))!!
                    .players
                    .first()
                    .avatar,
            )
            assertSame(id)
        }

    @Test
    fun `lancers, refus et correction se propagent, les deux parties restent identiques`() =
        runTest {
            val id = start()
            assertEquals(CommandOutcome.Applied, guestPlays(id, GameCommand.Play(pins(5, by = bruno), baseSeq(id))))
            // Bruno ne lance pas pour Alice.
            val refused = guestPlays(id, GameCommand.Play(pins(5, by = alice), baseSeq(id)))
            assertEquals(RejectCode.FORBIDDEN, (refused as CommandOutcome.Rejected).code)
            // L'organisateur saisit pour Alice, puis corrige son premier lancer.
            val first =
                hostGames
                    .observe(id)
                    .first()!!
                    .currentSet.log
                    .first()
                    .id
            deliver(host.submit(GameCommand.Play(pins(3), baseSeq(id))).second)
            deliver(host.submit(GameCommand.Play(Correct(EventId("c1"), first, pins(7)), baseSeq(id))).second)
            assertSame(id)
            assertEquals(
                7 + 3,
                guestGames
                    .observe(id)
                    .first()!!
                    .currentSet.replay.state
                    .team(TeamId("A"))!!
                    .score,
            )
        }

    @Test
    fun `retardataire, manche suivante et revanche passent aussi`() =
        runTest {
            val id = start()
            val chloe = PlayerId("chloe")
            val add =
                GameCommand.AddParticipant(
                    AddPlayer(EventId("add"), chloe, TeamId("C")),
                    SharedPlayer(chloe, "Chloé"),
                    SharedTeam(TeamId("C"), "Chloé", 2, memberKey = memberKey(listOf(chloe))),
                    baseSeq(id),
                )
            deliver(host.submit(add).second)
            assertEquals("Chloé", guestGames.observe(id).first()!!.playerName(chloe))
            assertSame(id)
            // Fin de la 1re manche : Alice (12) termine à 50.
            listOf(1, 1, 12, 1, 1, 12, 1, 1, 12, 1, 1, 2).forEachIndexed { i, p ->
                deliver(host.submit(GameCommand.Play(pins(p), baseSeq(id))).second)
                assertTrue("lancer $i", host.lastSeq > 0)
            }
            assertSame(id)
            deliver(host.submit(GameCommand.NextSet(1)).second)
            assertEquals(
                2,
                guestGames
                    .observe(id)
                    .first()!!
                    .sets.size,
            )
            assertSame(id)
            deliver(host.submit(GameCommand.Rematch("revanche", NextStarter.Rotate)).second)
            assertEquals("revanche", authority.gameId)
            assertEquals("revanche", store.gameId)
            assertEquals(GameOrigin.JOINED, guestGames.observe("revanche").first()!!.origin)
            assertSame("revanche")
        }

    @Test
    fun `rattrapage apres coupure`() =
        runTest {
            val id = start()
            host.disconnected(connection)
            deliver(
                host
                    .submit(
                        GameCommand.Play(pins(4, by = bruno), baseSeq(id)),
                    ).second
                    .filterNot { it is Outgoing.Broadcast },
            )
            host.submit(
                GameCommand.Play(
                    pins(6),
                    hostGames
                        .observe(id)
                        .first()!!
                        .currentSet.log.size,
                ),
            )
            deliver(host.receive(connection, guest.resumeRequest()!!))
            assertSame(id)
            assertEquals(2, guest.lastSeq)
        }
}
