package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.GameConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** `SCORING_SPEC.md` § 8. */
class UndoCorrectTest {
    /** Amène A à 45 points : A 12, 12, 12, 9 ; B lance la 1 entre-temps. */
    private fun LogBuilder.aliceAt45() {
        listOf(12, 12, 12, 9).forEach {
            play(it)
            play(1)
        }
    }

    @Test
    fun `U1 annuler un depassement`() {
        val log = LogBuilder().apply { aliceAt45() }
        log.play(9)
        assertEquals(25, log.state.score(A))
        log.undoLast()
        assertEquals(45, log.state.score(A))
        assertEquals(A, log.state.turn)
        assertEquals(8, log.result().effectiveLog.size)
    }

    @Test
    fun `U2 annuler la victoire`() {
        val log = LogBuilder().apply { aliceAt45() }
        log.play(5)
        assertEquals(SetStatus.FINISHED, log.state.status)
        log.undoLast()
        val s = log.state
        assertEquals(SetStatus.IN_PROGRESS, s.status)
        assertEquals(45, s.score(A))
        assertEquals(A, s.turn)
        assertTrue(s.ranking.isEmpty())
    }

    @Test
    fun `U3 annuler une elimination`() {
        val log = LogBuilder()
        repeat(2) {
            log.play()
            log.play(1)
        }
        log.play()
        assertTrue(log.state.team(A)!!.eliminated)
        log.undoLast()
        val s = log.state
        assertFalse(s.team(A)!!.eliminated)
        assertEquals(2, s.misses(A))
        assertEquals(A, s.turn)
        assertEquals(SetStatus.IN_PROGRESS, s.status)
    }

    @Test
    fun `U4 rien a annuler`() {
        val log = LogBuilder()
        log.add(Undo(nextId(), EventId("inconnu")))
        val result = log.result()
        assertEquals(newSet(), result.state)
        assertEquals(ScoringError.NOTHING_TO_UNDO, result.rejected.single().error)
    }

    @Test
    fun `U5 deux annulations successives`() {
        val log = LogBuilder()
        val e1 = log.play(3)
        log.play(5)
        log.play(7)
        log.undoLast()
        log.undoLast()
        assertEquals(fold(newSet(), listOf(e1)).getOrThrow(), log.state)
    }

    @Test
    fun `un Undo ne peut viser que le dernier evenement effectif`() {
        val log = LogBuilder()
        val e1 = log.play(3)
        log.play(5)
        log.add(Undo(nextId(), e1.id))
        assertEquals(
            ScoringError.NOTHING_TO_UNDO,
            log
                .result()
                .rejected
                .single()
                .error,
        )
        assertEquals(2, log.result().effectiveLog.size)
    }

    @Test
    fun `U6 annuler retire aussi le lancer perdu derive`() {
        val teams = listOf(lineup(A, alice), lineup(B, bruno, bea))
        val log = LogBuilder(GameConfig.ImoTournament, teams)
        log.add(WrongTeam(nextId(), B, bruno, listOf(5)))
        val afterF10 = log.state
        log.play(3)
        assertEquals(1, log.state.misses(B))
        log.undoLast()
        assertEquals(afterF10, log.state)
    }

    @Test
    fun `U7 une correction recalcule les scores en cascade`() {
        val log = LogBuilder()
        log.play(3)
        val e2 = log.play(4)
        log.play(5)
        log.play(6)
        log.play(7)
        val corrected = Throw(nextId(), B, bruno, listOf(8))
        log.add(Correct(nextId(), e2.id, corrected))
        val result = log.result()
        assertEquals(15, result.state.score(A))
        assertEquals(14, result.state.score(B))
        assertTrue(result.corrected)
        assertEquals(e2.id, result.effectiveLog[1].id)
    }

    @Test
    fun `U8 une correction qui invaliderait la suite du journal est refusee`() {
        val log = LogBuilder()
        listOf(12, 12, 12, 2).forEach {
            log.play(it)
            log.play(1)
        }
        val target = log.play(1)
        log.play(1)
        log.play(1)
        val before = log.result()
        log.add(Correct(nextId(), target.id, Throw(nextId(), A, alice, listOf(12))))
        val after = log.result()
        assertEquals(ScoringError.CORRECTION_INVALIDATES_LOG, after.rejected.single().error)
        assertEquals(before.effectiveLog, after.effectiveLog)
        assertEquals(before.state, after.state)
        assertFalse(after.corrected)
    }

    @Test
    fun `correction d'un evenement inconnu`() {
        val log = LogBuilder()
        log.play(3)
        log.add(Correct(nextId(), EventId("inconnu"), Throw(nextId(), A, alice, listOf(1))))
        assertEquals(
            ScoringError.UNKNOWN_EVENT,
            log
                .result()
                .rejected
                .single()
                .error,
        )
    }

    @Test
    fun `un evenement corrige peut ensuite etre annule`() {
        val log = LogBuilder()
        log.play(3)
        val e2 = log.play(4)
        log.add(Correct(nextId(), e2.id, Throw(nextId(), B, bruno, listOf(9))))
        log.add(Undo(nextId(), e2.id))
        assertEquals(1, log.result().effectiveLog.size)
        assertEquals(0, log.state.score(B))
    }

    @Test
    fun `corrections de fautes et d'abandon`() {
        val log = LogBuilder(GameConfig.ImoTournament, listOf(lineup(A, alice), lineup(B, bruno, bea)))
        val fault = log.add(Fault(nextId(), A, alice, FaultKind.DROPPED))
        val wrongTeam = log.add(WrongTeam(nextId(), A, alice, listOf(2)))
        val abandon = log.add(Abandon(nextId()))
        log.add(Correct(nextId(), fault.id, Fault(nextId(), A, alice, FaultKind.TIMEOUT)))
        log.add(Correct(nextId(), wrongTeam.id, WrongTeam(nextId(), A, alice, listOf(3))))
        log.add(Correct(nextId(), abandon.id, Abandon(nextId())))
        val result = log.result()
        assertTrue(result.rejected.isEmpty())
        assertEquals(listOf(fault.id, wrongTeam.id, abandon.id), result.effectiveLog.map { it.id })
        assertEquals(SetStatus.ABANDONED, result.state.status)
    }

    @Test
    fun `un evenement invalide au rejeu est ignore et signale`() {
        val log = LogBuilder()
        log.add(Throw(nextId(), B, bruno, listOf(3)))
        log.play(4)
        val result = log.result()
        assertEquals(ScoringError.NOT_YOUR_TURN, result.rejected.single().error)
        assertEquals(4, result.state.score(A))
    }

    @Test
    fun `rejeu avec une configuration invalide`() =
        assertEquals(ScoringError.NOT_ENOUGH_TEAMS, replay(GameConfig.Classic, soloTeams(1), emptyList()).errorOrNull())
}
