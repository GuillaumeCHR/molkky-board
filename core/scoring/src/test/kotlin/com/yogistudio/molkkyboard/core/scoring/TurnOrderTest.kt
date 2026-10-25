package com.yogistudio.molkkyboard.core.scoring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** `SCORING_SPEC.md` § 6. */
class TurnOrderTest {
    @Test
    fun `T1 chaque equipe fait tourner ses propres joueurs`() {
        var s = newSet(teams = listOf(lineup(A, alice, arthur), lineup(B, bruno, bea, benoit)))
        val throwers = (1..8).map { s.currentPlayer.also { s = s.play(1) } }
        assertEquals(listOf(alice, bruno, arthur, bea, alice, benoit, arthur, bruno), throwers)
    }

    @Test
    fun `T2 une equipe eliminee est sautee`() {
        var s = newSet(teams = soloTeams(3)).given(B, misses = 1)
        val turns =
            (1..10).map { i ->
                s.turn.also { s = if (s.turn == B) s.play() else s.play(1) }
            }
        assertEquals(listOf(A, B, C, A, B, C, A, C, A, C), turns)
    }

    @Test
    fun `T3 premier a 50 fin immediate sans finir le tour`() {
        val s =
            newSet(teams = soloTeams(3))
                .given(B, score = 10)
                .given(C, score = 20)
                .given(A, score = 45)
                .play(5)
        assertEquals(SetStatus.FINISHED, s.status)
        assertNull(s.turn)
        assertEquals(listOf(A, C, B), s.ranking)
    }

    @Test
    fun `T4 lancer saisi pour une equipe dont ce n'est pas le tour`() =
        assertEquals(ScoringError.NOT_YOUR_TURN, newSet().tryThrow(B, 3))

    @Test
    fun `T5 joueur d'une autre equipe`() =
        assertEquals(
            ScoringError.NOT_YOUR_TURN,
            newSet().tryThrow(A, 3, player = bruno),
        )

    @Test
    fun `T6 coequipier qui n'est pas le lanceur attendu`() {
        val s = newSet(teams = listOf(lineup(A, alice, arthur), lineup(B, bruno)))
        assertEquals(ScoringError.NOT_YOUR_TURN, s.tryThrow(A, 3, player = arthur))
    }

    @Test
    fun `joueur absent de la partie`() =
        assertEquals(
            ScoringError.UNKNOWN_PLAYER,
            newSet().tryThrow(A, 3, player = chloe),
        )
}
