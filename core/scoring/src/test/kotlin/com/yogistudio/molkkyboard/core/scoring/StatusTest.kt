package com.yogistudio.molkkyboard.core.scoring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** `SCORING_SPEC.md` § 13 : statut et éligibilité aux records, dérivés du journal. */
class StatusTest {
    private fun LogBuilder.aliceWins() {
        listOf(12, 12, 12, 12, 2).forEach {
            play(it)
            if (state.status == SetStatus.IN_PROGRESS) play(1)
        }
    }

    @Test
    fun `S1 victoire normale eligible`() {
        val result = LogBuilder().apply { aliceWins() }.result()
        assertEquals(SetStatus.FINISHED, result.state.status)
        assertFalse(result.modifiedAfterFinish)
        assertTrue(result.eligibleForRecords)
    }

    @Test
    fun `S2 abandon`() {
        val log = LogBuilder()
        log.play(3)
        log.add(Abandon(nextId()))
        log.add(throwOf(1))
        val result = log.result()
        assertEquals(SetStatus.ABANDONED, result.state.status)
        assertNull(result.state.turn)
        assertEquals(ScoringError.SET_FINISHED, result.rejected.single().error)
        assertFalse(result.eligibleForRecords)
    }

    @Test
    fun `S3 R3 une partie corrigee apres sa fin n'est pas eligible`() {
        val log = LogBuilder().apply { aliceWins() }
        val firstOfB = log.result().effectiveLog[1]
        log.add(Correct(nextId(), firstOfB.id, throwOf(2)))
        val result = log.result()
        assertEquals(SetStatus.FINISHED, result.state.status)
        assertTrue(result.hasCorrections)
        assertTrue(result.modifiedAfterFinish)
        assertFalse(result.eligibleForRecords)
    }

    @Test
    fun `S4 un Undo n'est pas une correction`() {
        val log = LogBuilder()
        log.play(3)
        log.undoLast()
        log.aliceWins()
        val result = log.result()
        assertFalse(result.hasCorrections)
        assertTrue(result.eligibleForRecords)
    }

    @Test
    fun `S5 abandon d'une manche terminee`() {
        val log = LogBuilder().apply { aliceWins() }
        log.add(Abandon(nextId()))
        assertEquals(
            ScoringError.SET_FINISHED,
            log
                .result()
                .rejected
                .single()
                .error,
        )
    }

    @Test
    fun `S6 annuler un abandon`() {
        val log = LogBuilder()
        log.play(3)
        log.add(Abandon(nextId()))
        log.undoLast()
        assertEquals(SetStatus.IN_PROGRESS, log.state.status)
        assertEquals(B, log.state.turn)
    }
}
