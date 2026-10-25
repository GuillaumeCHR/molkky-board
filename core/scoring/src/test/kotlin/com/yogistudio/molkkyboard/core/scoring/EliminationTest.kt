package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.GameConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** `SCORING_SPEC.md` § 5. */
class EliminationTest {
    @Test
    fun `E1 trois rates eliminent et le dernier en lice gagne quel que soit son score`() {
        val s = newSet().given(A, score = 30, misses = 2).given(B, score = 12).play()
        assertTrue(s.team(A)!!.eliminated)
        assertEquals(SetStatus.FINISHED, s.status)
        assertEquals(listOf(B, A), s.ranking)
        assertEquals(12, s.score(B))
    }

    @Test
    fun `E2 a trois equipes la manche continue sans l'equipe eliminee`() {
        var s = newSet(teams = soloTeams(3)).given(A, score = 30, misses = 2).play()
        assertTrue(s.team(A)!!.eliminated)
        assertEquals(SetStatus.IN_PROGRESS, s.status)
        val order = mutableListOf<Any?>()
        repeat(4) {
            order += s.turn
            s = s.play(1)
        }
        assertEquals(listOf(B, C, B, C), order)
    }

    @Test
    fun `E3 les eliminees sont classees de la derniere a la premiere eliminee`() {
        val s =
            newSet(teams = soloTeams(3))
                .given(A, eliminated = true)
                .given(B, score = 40, misses = 2)
                .given(C, score = 10)
                .turnOf(B)
                .play()
        assertEquals(SetStatus.FINISHED, s.status)
        assertEquals(listOf(C, B, A), s.ranking)
    }

    @Test
    fun `E4 les rates s'accumulent malgre les lancers adverses`() {
        val s =
            newSet()
                .given(A, score = 30, misses = 1)
                .play()
                .play(4)
                .play()
        assertTrue(s.team(A)!!.eliminated)
    }

    @Test
    fun `E5 les rates doivent etre consecutifs`() {
        val s =
            newSet()
                .given(A, score = 30, misses = 1)
                .play()
                .play(4)
                .play(1)
                .play(4)
                .play()
        assertFalse(s.team(A)!!.eliminated)
        assertEquals(31, s.score(A))
        assertEquals(1, s.misses(A))
    }

    @Test
    fun `E6 le compteur de rates est celui de l'equipe`() {
        val teams = listOf(lineup(A, alice, arthur), lineup(B, bruno))
        val s =
            newSet(teams = teams)
                .play()
                .play(3)
                .play()
                .play(3)
                .play()
        assertTrue(s.team(A)!!.eliminated)
        assertEquals(listOf(B, A), s.ranking)
    }

    @Test
    fun `E7 en tournoi IMO le vainqueur par elimination recoit 50 et l'elimine 0`() {
        val s = newSet(GameConfig.ImoTournament).given(A, score = 30, misses = 2).given(B, score = 12).play()
        assertEquals(mapOf(A to 0, B to 50), s.setPoints)
        assertEquals(30, s.score(A))
        assertEquals(12, s.score(B))
    }

    @Test
    fun `points de manche reels en mode classique et pendant la manche`() {
        val running = newSet(GameConfig.ImoTournament).given(A, score = 30)
        assertEquals(mapOf(A to 30, B to 0), running.setPoints)
        val classic = newSet().given(A, score = 30, misses = 2).given(B, score = 12).play()
        assertEquals(mapOf(A to 30, B to 12), classic.setPoints)
    }

    @Test
    fun `en mode IMO une equipe non eliminee et non gagnante garde son score`() {
        val s = newSet(GameConfig.ImoTournament, soloTeams(3)).given(A, score = 45).given(B, score = 20).play(5)
        assertEquals(mapOf(A to 50, B to 20, C to 0), s.setPoints)
    }
}
