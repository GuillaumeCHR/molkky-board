package com.yogistudio.molkkyboard.core.sync

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.scoring.Abandon
import com.yogistudio.molkkyboard.core.scoring.AddPlayer
import com.yogistudio.molkkyboard.core.scoring.Correct
import com.yogistudio.molkkyboard.core.scoring.Delete
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.Fault
import com.yogistudio.molkkyboard.core.scoring.FaultKind
import com.yogistudio.molkkyboard.core.scoring.RemovePlayer
import com.yogistudio.molkkyboard.core.scoring.Reorder
import com.yogistudio.molkkyboard.core.scoring.ScoringError
import com.yogistudio.molkkyboard.core.scoring.SetEvent
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import com.yogistudio.molkkyboard.core.scoring.Undo
import com.yogistudio.molkkyboard.core.scoring.WrongTeam
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Validation partagée par l'autorité (téléphone hôte, futur serveur) : `ARCHITECTURE.md` § 6 bis. */
class ValidationTest {
    private val alice = PlayerId("alice")
    private val bruno = PlayerId("bruno")
    private val a = TeamId("A")
    private val b = TeamId("B")
    private val lineup = listOf(TeamLineup(a, listOf(alice)), TeamLineup(b, listOf(bruno)))
    private var next = 0

    private fun id() = EventId("e${next++}")

    private fun pins(vararg pins: Int, by: PlayerId? = null) = Throw(id(), ThrowResult.Pins(pins.toList()), by)

    private fun set(vararg events: SetEvent) = SetLog(GameConfig.Classic, lineup, events.toList())

    private val aliceDevice = Author(DeviceId("tel-alice"), Role.PLAYER, alice)
    private val brunoDevice = Author(DeviceId("tel-bruno"), Role.PLAYER, bruno)
    private val organizer = Author(DeviceId("hote"), Role.ORGANIZER)

    private fun assertRejected(code: RejectCode, validation: Validation) =
        assertEquals(code, (validation as? Validation.Rejected)?.code, "$validation")

    private fun assertAccepted(validation: Validation) =
        assertTrue(validation is Validation.Accepted, "$validation")

    @Test
    fun `un joueur saisit son propre lancer quand c'est son tour`() {
        val result = validate(set(), pins(7, by = alice), aliceDevice)
        assertAccepted(result)
        assertEquals(
            7,
            (result as Validation.Accepted)
                .result.state.teams
                .first { it.id == a }
                .score,
        )
    }

    @Test
    fun `un joueur ne saisit pas pour un autre, l'organisateur oui`() {
        assertRejected(RejectCode.FORBIDDEN, validate(set(), pins(7, by = bruno), aliceDevice))
        assertRejected(RejectCode.FORBIDDEN, validate(set(), pins(7), aliceDevice))
        assertAccepted(validate(set(), pins(7), organizer))
        assertAccepted(validate(set(), pins(7), Author.Local))
    }

    @Test
    fun `hors de son tour - NOT_YOUR_TURN, meme pour l'organisateur qui nomme le lanceur`() {
        val result = validate(set(), pins(7, by = bruno), brunoDevice)
        assertRejected(RejectCode.NOT_YOUR_TURN, result)
        assertEquals(ScoringError.NOT_YOUR_TURN, (result as Validation.Rejected).error)
        assertRejected(RejectCode.NOT_YOUR_TURN, validate(set(), pins(7, by = bruno), organizer))
    }

    @Test
    fun `un lancer incoherent est INVALID, avec l'erreur du moteur`() {
        val result = validate(set(), pins(13, by = alice), aliceDevice)
        assertRejected(RejectCode.INVALID, result)
        assertEquals(ScoringError.INVALID_PIN, (result as Validation.Rejected).error)
    }

    @Test
    fun `un identifiant deja present dans le journal est refuse`() {
        val first = pins(7, by = alice)
        assertRejected(
            RejectCode.INVALID,
            validate(set(first), Throw(first.id, ThrowResult.Pins(listOf(3)), bruno), brunoDevice),
        )
    }

    @Test
    fun `fautes et lancer de la mauvaise equipe - seulement au nom du joueur du telephone`() {
        assertAccepted(validate(set(), Fault(id(), FaultKind.TRESPASS, alice), aliceDevice))
        assertRejected(RejectCode.FORBIDDEN, validate(set(), Fault(id(), FaultKind.WRONG_PLAYER, alice), aliceDevice))
        assertRejected(RejectCode.FORBIDDEN, validate(set(), Fault(id(), FaultKind.TRESPASS, bruno), aliceDevice))
        assertRejected(
            RejectCode.FORBIDDEN,
            validate(set(), WrongTeam(id(), b, alice, ThrowResult.Pins(listOf(3))), brunoDevice),
        )
        assertAccepted(validate(set(), WrongTeam(id(), b, bruno, ThrowResult.Pins(listOf(3))), brunoDevice))
    }

    @Test
    fun `gestion des joueurs et abandon - organisateur seulement`() {
        listOf(
            Abandon(id()),
            AddPlayer(id(), PlayerId("chloe"), TeamId("C")),
            RemovePlayer(id(), bruno),
            Reorder(id(), listOf(b, a)),
        ).forEach { event ->
            assertRejected(RejectCode.FORBIDDEN, validate(set(), event, aliceDevice))
            assertAccepted(validate(set(), event, organizer))
        }
    }

    @Test
    fun `un joueur annule ou corrige son dernier lancer, pas celui d'un autre`() {
        val aliceThrow = pins(7, by = alice)
        val brunoThrow = pins(3, by = bruno)
        val log = set(aliceThrow, brunoThrow)
        assertRejected(RejectCode.FORBIDDEN, validate(log, Undo(id(), brunoThrow.id), aliceDevice))
        assertAccepted(validate(log, Undo(id(), brunoThrow.id), brunoDevice))
        assertAccepted(validate(log, Correct(id(), aliceThrow.id, pins(8, by = alice)), aliceDevice))
        assertAccepted(validate(log, Correct(id(), aliceThrow.id, pins(8)), aliceDevice))
        assertRejected(
            RejectCode.FORBIDDEN,
            validate(log, Correct(id(), aliceThrow.id, pins(8, by = bruno)), aliceDevice),
        )
        assertRejected(RejectCode.FORBIDDEN, validate(log, Correct(id(), aliceThrow.id, Abandon(id())), aliceDevice))
        assertRejected(
            RejectCode.FORBIDDEN,
            validate(log, Correct(id(), brunoThrow.id, pins(4, by = bruno)), aliceDevice),
        )
        assertAccepted(validate(log, Delete(id(), aliceThrow.id), aliceDevice))
        assertRejected(RejectCode.FORBIDDEN, validate(log, Delete(id(), brunoThrow.id), aliceDevice))
        // Correction d'une faute : le remplaçant peut être une faute à son nom.
        val fault = Fault(id(), FaultKind.TRESPASS, alice)
        assertAccepted(validate(set(fault), Correct(id(), fault.id, Fault(id(), FaultKind.DROPPED)), aliceDevice))
    }

    @Test
    fun `annulation sur un journal depasse - STALE`() {
        val aliceThrow = pins(7, by = alice)
        val brunoThrow = pins(3, by = bruno)
        // Alice voulait annuler son lancer, mais Bruno a lancé entre-temps.
        assertRejected(
            RejectCode.STALE,
            validate(set(aliceThrow, brunoThrow), Undo(id(), aliceThrow.id), aliceDevice, baseSeq = 1),
        )
        assertAccepted(validate(set(aliceThrow), Undo(id(), aliceThrow.id), aliceDevice, baseSeq = 1))
    }

    @Test
    fun `correction sur un journal depasse - STALE seulement si la cible a ete touchee depuis`() {
        val aliceThrow = pins(7, by = alice)
        val brunoThrow = pins(3, by = bruno)
        val correctedByHost = Correct(id(), aliceThrow.id, pins(9))
        // Un lancer de Bruno depuis : la correction d'Alice reste valable.
        assertAccepted(
            validate(set(aliceThrow, brunoThrow), Correct(id(), aliceThrow.id, pins(8)), organizer, baseSeq = 1),
        )
        // L'organisateur a déjà corrigé ce lancer : refus.
        assertRejected(
            RejectCode.STALE,
            validate(set(aliceThrow, correctedByHost), Correct(id(), aliceThrow.id, pins(8)), organizer, baseSeq = 1),
        )
        assertRejected(
            RejectCode.STALE,
            validate(set(aliceThrow, brunoThrow), Delete(id(), brunoThrow.id), organizer, baseSeq = 1),
        )
        assertAccepted(validate(set(aliceThrow, brunoThrow), Delete(id(), aliceThrow.id), organizer, baseSeq = 1))
        // Un lancer ordinaire n'est jamais périmé : seul le tour compte.
        assertAccepted(validate(set(aliceThrow), pins(4, by = bruno), brunoDevice, baseSeq = 0))
    }

    @Test
    fun `correction refusee par le moteur - INVALID avec la raison`() {
        val aliceThrow = pins(7, by = alice)
        val result = validate(set(aliceThrow), Undo(id(), EventId("inconnu")), organizer)
        assertRejected(RejectCode.INVALID, result)
        assertEquals(ScoringError.NOTHING_TO_UNDO, (result as Validation.Rejected).error)
    }

    @Test
    fun `un telephone joueur sans joueur associe ne peut rien saisir`() {
        assertRejected(RejectCode.FORBIDDEN, validate(set(), pins(7, by = alice), Author(DeviceId("x"), Role.PLAYER)))
    }
}
