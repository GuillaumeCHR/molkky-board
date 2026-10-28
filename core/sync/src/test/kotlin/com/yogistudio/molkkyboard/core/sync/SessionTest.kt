package com.yogistudio.molkkyboard.core.sync

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.MatchConfig
import com.yogistudio.molkkyboard.core.scoring.SetEvent
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Protocole de la partie à côté : un hôte et deux invités reliés en mémoire (`ARCHITECTURE.md` § 6 bis). */
class SessionTest {
    private val alice = PlayerId("alice")
    private val bruno = PlayerId("bruno")
    private val lineup = listOf(TeamLineup(TeamId("A"), listOf(alice)), TeamLineup(TeamId("B"), listOf(bruno)))
    private var now = 1_000L
    private var ids = 0

    /** Autorité de test : une manche, validée par la vraie fonction partagée. */
    private inner class FakeAuthority : Authority {
        val log = mutableListOf<SetEvent>()

        override suspend fun apply(command: GameCommand, author: Author): CommandOutcome {
            if (!command.allowedFor(author)) return CommandOutcome.Rejected(RejectCode.FORBIDDEN)
            val play = command as? GameCommand.Play ?: return CommandOutcome.Applied
            return when (
                val validation =
                    validate(
                        SetLog(GameConfig.Classic, lineup, log),
                        play.event,
                        author,
                        play.baseSeq,
                    )
            ) {
                is Validation.Accepted -> CommandOutcome.Applied.also { log += play.event }
                is Validation.Rejected -> CommandOutcome.Rejected(validation.code, validation.error)
            }
        }

        override suspend fun snapshot() =
            SharedGame(
                id = "partie",
                createdAt = 1L,
                config = GameConfig.Classic,
                matchConfig = MatchConfig(setsToWin = 1),
                teams =
                    listOf(
                        SharedTeam(TeamId("A"), "Alice", 0, memberKey = "a"),
                        SharedTeam(TeamId("B"), "Bruno", 1, memberKey = "b"),
                    ),
                players = listOf(SharedPlayer(alice, "Alice"), SharedPlayer(bruno, "Bruno")),
                sets = listOf(SharedSet(lineup, log.toList())),
            )
    }

    private class FakeStore : GuestStore {
        var game: SharedGame? = null
        val applied = mutableListOf<GameCommand>()

        override suspend fun importGame(game: SharedGame) {
            this.game = game
            applied.clear()
        }

        override suspend fun applyRemote(command: GameCommand, author: String) {
            applied += command
        }
    }

    private val authority = FakeAuthority()
    private val host = HostSession(authority, clock = { now })
    private val aliceStore = FakeStore()
    private val brunoStore = FakeStore()
    private val aliceGuest = GuestSession(aliceStore) { "a-${ids++}" }
    private val brunoGuest = GuestSession(brunoStore) { "b-${ids++}" }
    private val aliceConn = ConnectionId(1)
    private val brunoConn = ConnectionId(2)
    private val guests = mapOf(aliceConn to aliceGuest, brunoConn to brunoGuest)
    private val closed = mutableSetOf<ConnectionId>()

    /** Livre les messages de l'hôte (et les réponses des invités), comme le ferait le transport. */
    private suspend fun deliver(outgoing: List<Outgoing>) {
        outgoing.forEach { out ->
            when (out) {
                is Outgoing.To -> {
                    reply(out.connection, guests.getValue(out.connection).receive(out.message))
                }

                is Outgoing.Broadcast -> {
                    host.connectedGuests.forEach {
                        reply(
                            it,
                            guests.getValue(it).receive(out.message),
                        )
                    }
                }

                is Outgoing.Close -> {
                    closed += out.connection
                }
            }
        }
    }

    private suspend fun reply(connection: ConnectionId, messages: List<Message>) =
        messages.forEach { deliver(host.receive(connection, it)) }

    private suspend fun send(connection: ConnectionId, message: Message) = deliver(host.receive(connection, message))

    private fun pins(vararg pins: Int, by: PlayerId? = null) =
        Throw(EventId("e${ids++}"), ThrowResult.Pins(pins.toList()), by)

    private suspend fun joinBoth() {
        send(aliceConn, aliceGuest.joinRequest(host.newInvite(), "Alice", "pin"))
        assertEquals(GuestStatus.WAITING_APPROVAL, aliceGuest.status)
        deliver(host.accept(aliceConn, alice))
        send(brunoConn, brunoGuest.joinRequest(host.newInvite(), "Bruno", null))
        deliver(host.accept(brunoConn, bruno))
    }

    @Test
    fun `arrivee - jeton, attente de l'organisateur, accueil avec la partie complete`() =
        runTest {
            val token = host.newInvite()
            send(aliceConn, aliceGuest.joinRequest(token, " Alice‮ ", "pin"))
            assertEquals(listOf("Alice"), host.candidates.map { it.nickname })
            assertNull(aliceStore.game)
            deliver(host.accept(aliceConn, alice))
            assertEquals(GuestStatus.PLAYING, aliceGuest.status)
            assertEquals("partie", aliceStore.game?.id)
            assertEquals(alice, aliceGuest.player)
            assertEquals(64, aliceGuest.sessionToken?.length)
        }

    @Test
    fun `jeton reutilise, expire, ou mauvaise version - refuses`() =
        runTest {
            val token = host.newInvite()
            send(aliceConn, aliceGuest.joinRequest(token, "Alice", null))
            send(brunoConn, brunoGuest.joinRequest(token, "Bruno", null))
            assertEquals(JoinRejection.TOKEN_INVALID, brunoGuest.rejection)
            assertTrue(brunoConn in closed)
            val other = GuestSession(FakeStore()) { "x" }
            val late = host.newInvite()
            now += HostSession.INVITE_TTL_MS + 1
            assertEquals(
                listOf(Message.JoinRejected(JoinRejection.TOKEN_INVALID)),
                host
                    .receive(
                        ConnectionId(3),
                        other.joinRequest(late, "Chloé", null),
                    ).filterIsInstance<Outgoing.To>()
                    .map {
                        it.message
                    },
            )
            val fresh = host.newInvite()
            val old = Message.JoinRequest(PROTOCOL_VERSION + 1, fresh, "Denis", null)
            assertEquals(
                Message.JoinRejected(JoinRejection.VERSION),
                (host.receive(ConnectionId(4), old).first() as Outgoing.To).message,
            )
        }

    @Test
    fun `refus de l'organisateur`() =
        runTest {
            send(aliceConn, aliceGuest.joinRequest(host.newInvite(), "Alice", null))
            deliver(host.refuse(aliceConn))
            assertEquals(JoinRejection.REFUSED, aliceGuest.rejection)
            assertTrue(host.candidates.isEmpty())
        }

    @Test
    fun `une proposition validee est numerotee et diffusee a tous, auteur compris`() =
        runTest {
            joinBoth()
            val proposal = aliceGuest.propose(GameCommand.Play(pins(7, by = alice), baseSeq = 0))
            send(aliceConn, proposal)
            assertEquals(1, host.lastSeq)
            assertEquals(1, aliceStore.applied.size)
            assertEquals(1, brunoStore.applied.size)
            assertEquals(CommandOutcome.Applied, aliceGuest.outcomes[proposal.id])
            assertEquals(1, brunoGuest.lastSeq)
        }

    @Test
    fun `une proposition renvoyee n'est pas appliquee deux fois`() =
        runTest {
            joinBoth()
            val proposal = aliceGuest.propose(GameCommand.Play(pins(7, by = alice), baseSeq = 0))
            send(aliceConn, proposal)
            send(aliceConn, proposal)
            assertEquals(1, authority.log.size)
            assertEquals(1, host.lastSeq)
            assertEquals(1, aliceStore.applied.size)
        }

    @Test
    fun `hors tour ou au nom d'un autre - refuse avec le motif, rien n'est diffuse`() =
        runTest {
            joinBoth()
            val notMine = brunoGuest.propose(GameCommand.Play(pins(7, by = alice), baseSeq = 0))
            send(brunoConn, notMine)
            assertEquals(CommandOutcome.Rejected(RejectCode.FORBIDDEN), brunoGuest.outcomes[notMine.id])
            val early = brunoGuest.propose(GameCommand.Play(pins(7, by = bruno), baseSeq = 0))
            send(brunoConn, early)
            assertEquals(CommandOutcome.Rejected(RejectCode.NOT_YOUR_TURN), brunoGuest.outcomes[early.id])
            assertEquals(0, host.lastSeq)
            assertTrue(aliceStore.applied.isEmpty())
            // Un joueur ne gère pas le match.
            val nextSet = brunoGuest.propose(GameCommand.NextSet(1))
            send(brunoConn, nextSet)
            assertEquals(CommandOutcome.Rejected(RejectCode.FORBIDDEN), brunoGuest.outcomes[nextSet.id])
        }

    @Test
    fun `la saisie de l'organisateur passe par la meme validation et est diffusee`() =
        runTest {
            joinBoth()
            val (outcome, outgoing) = host.submit(GameCommand.Play(pins(5), baseSeq = 0))
            assertEquals(CommandOutcome.Applied, outcome)
            deliver(outgoing)
            assertEquals(1, aliceStore.applied.size)
            assertEquals(1, brunoStore.applied.size)
        }

    @Test
    fun `coupure puis reconnexion - l'hote renvoie seulement ce qui manque`() =
        runTest {
            joinBoth()
            host.disconnected(brunoConn)
            send(aliceConn, aliceGuest.propose(GameCommand.Play(pins(7, by = alice), baseSeq = 0)))
            assertEquals(0, brunoGuest.lastSeq)
            send(brunoConn, brunoGuest.resumeRequest()!!)
            assertEquals(1, brunoGuest.lastSeq)
            assertEquals(1, brunoStore.applied.size)
        }

    @Test
    fun `un trou dans les numeros declenche un rattrapage`() =
        runTest {
            joinBoth()
            val event = Message.Event(2, "x", "hote", GameCommand.NextSet(1), now)
            assertEquals(listOf(Message.Resume(brunoGuest.sessionToken!!, 0)), brunoGuest.receive(event))
            assertEquals(0, brunoGuest.lastSeq)
        }

    @Test
    fun `retard trop grand ou numero incoherent - etat complet`() =
        runTest {
            joinBoth()
            val outgoing = host.receive(brunoConn, Message.Resume(brunoGuest.sessionToken!!, lastSeq = 99))
            assertTrue((outgoing.single() as Outgoing.To).message is Message.Snapshot)
        }

    @Test
    fun `un telephone exclu ne revient pas`() =
        runTest {
            joinBoth()
            val device = host.guests.first { it.nickname == "Bruno" }.device
            deliver(host.exclude(device))
            assertEquals(GuestStatus.REMOVED, brunoGuest.status)
            val again = host.receive(ConnectionId(9), brunoGuest.resumeRequest()!!)
            assertEquals(Message.JoinRejected(JoinRejection.SESSION_INVALID), (again.first() as Outgoing.To).message)
            // Une proposition sur une connexion non acceptée ferme la connexion.
            assertEquals(
                listOf(Outgoing.Close(ConnectionId(9))),
                host.receive(ConnectionId(9), brunoGuest.propose(GameCommand.NextSet(1))),
            )
        }

    @Test
    fun `fin du partage, ping et message inattendu`() =
        runTest {
            joinBoth()
            assertEquals(listOf(Outgoing.To(aliceConn, Message.Pong)), host.receive(aliceConn, Message.Ping))
            assertEquals(listOf(Outgoing.Close(aliceConn)), host.receive(aliceConn, Message.Pong))
            deliver(host.end())
            assertEquals(GuestStatus.ENDED, aliceGuest.status)
            assertTrue(host.guests.isEmpty())
        }

    @Test
    fun `messages - aller-retour JSON, taille bornee`() {
        val event = Message.Event(1, "id", "hote", GameCommand.Play(pins(7, by = alice), 0), 5)
        assertEquals(event, decodeMessage(event.encode()!!))
        assertNull(decodeMessage("{pas du json"))
        assertNull(decodeMessage("x".repeat(MAX_MESSAGE_BYTES + 1)))
        val huge = Message.JoinRequest(PROTOCOL_VERSION, "t", "x".repeat(MAX_MESSAGE_BYTES), null)
        assertNull(huge.encode())
    }

    @Test
    fun `cas limites - saisie refusee, partie pleine, refus renvoye, etat complet et ping cote invite`() =
        runTest {
            joinBoth()
            // Saisie de l'organisateur refusée : rien n'est diffusé.
            val (refused, nothing) = host.submit(GameCommand.Play(pins(7, by = bruno), baseSeq = 0))
            assertTrue(refused is CommandOutcome.Rejected)
            assertTrue(nothing.isEmpty())
            // Une proposition refusée, renvoyée telle quelle, reçoit le même refus.
            val bad = brunoGuest.propose(GameCommand.Play(pins(7, by = alice), baseSeq = 0))
            assertEquals(1, brunoGuest.unanswered().size)
            send(brunoConn, bad)
            assertEquals(
                listOf(Outgoing.To(brunoConn, Message.EventRejected(bad.id, RejectCode.FORBIDDEN))),
                host.receive(brunoConn, bad),
            )
            assertTrue(brunoGuest.unanswered().isEmpty())
            // État complet reçu par l'invité, ping, message inattendu.
            assertEquals(emptyList<Message>(), aliceGuest.receive(Message.Snapshot(authority.snapshot(), 7)))
            assertEquals(7, aliceGuest.lastSeq)
            assertEquals(listOf(Message.Pong), aliceGuest.receive(Message.Ping))
            assertEquals(emptyList<Message>(), aliceGuest.receive(Message.Pong))
            assertTrue(aliceGuest.device!!.startsWith("tel-"))
            // Partie pleine.
            repeat(MAX_GUESTS - 2) { i ->
                val conn = ConnectionId(100L + i)
                host.receive(conn, Message.JoinRequest(PROTOCOL_VERSION, host.newInvite(), "J$i", null))
                host.accept(conn, null)
            }
            val full =
                host.receive(
                    ConnectionId(99),
                    Message.JoinRequest(PROTOCOL_VERSION, host.newInvite(), "Trop", null),
                )
            assertEquals(Message.JoinRejected(JoinRejection.FULL), (full.first() as Outgoing.To).message)
            assertTrue(ProtocolJson.configuration.ignoreUnknownKeys)
        }
}
