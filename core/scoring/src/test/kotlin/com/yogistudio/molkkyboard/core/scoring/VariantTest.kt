package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.EndCondition
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.MissPenalty
import com.yogistudio.molkkyboard.core.model.Overshoot
import com.yogistudio.molkkyboard.core.model.TeamOverride
import com.yogistudio.molkkyboard.core.model.handicap
import com.yogistudio.molkkyboard.core.model.isStandard
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** `SCORING_SPEC.md` § 9, plus les surcharges par équipe du mode handicap. */
class VariantTest {
    @Test
    fun `V1 depassement retour a 0`() =
        assertEquals(0, newSet(GameConfig(overshoot = Overshoot.ResetTo(0))).given(A, score = 45).play(9).score(A))

    @Test
    fun `V2 depassement lancer annule`() {
        val s = newSet(GameConfig(overshoot = Overshoot.Stay)).given(A, score = 45, misses = 1).play(9)
        assertEquals(45, s.score(A))
        assertEquals(0, s.misses(A))
    }

    @Test
    fun `V3 depassement rebond`() =
        assertEquals(46, newSet(GameConfig(overshoot = Overshoot.Bounce)).given(A, score = 45).play(9).score(A))

    @Test
    fun `rebond borne a zero`() {
        val config = GameConfig(targetScore = 10, overshoot = Overshoot.Bounce)
        assertEquals(0, newSet(config).given(A, score = 9).play(12).score(A))
    }

    @Test
    fun `V4 aucune sanction le compteur continue`() {
        val s = newSet(GameConfig(onMissLimit = MissPenalty.Nothing)).given(A, score = 30, misses = 2).play()
        assertEquals(3, s.misses(A))
        assertFalse(s.team(A)!!.eliminated)
        assertEquals(4, s.play(1).play().misses(A))
    }

    @Test
    fun `V5 retour a 0 au lieu de l'elimination`() {
        val s = newSet(GameConfig(onMissLimit = MissPenalty.ResetScore(0))).given(A, score = 30, misses = 2).play()
        assertEquals(0, s.score(A))
        assertEquals(0, s.misses(A))
        assertTrue(s.team(A)!!.active)
    }

    @Test
    fun `V6 retour a 25 au lieu de l'elimination`() {
        val s = newSet(GameConfig(onMissLimit = MissPenalty.ResetScore(25))).given(A, score = 40, misses = 2).play()
        assertEquals(25, s.score(A))
        assertEquals(0, s.misses(A))
    }

    @Test
    fun `V7 cible 30 retour a 15`() {
        val config = GameConfig(targetScore = 30, overshoot = Overshoot.ResetTo(15))
        assertEquals(15, newSet(config).given(A, score = 25).play(6).score(A))
    }

    @Test
    fun `V8 quatre rates avant elimination`() {
        val config = GameConfig(missLimit = 4)
        assertFalse(
            newSet(config)
                .given(A, misses = 2)
                .play()
                .team(A)!!
                .eliminated,
        )
        assertTrue(
            newSet(config)
                .given(A, score = 30, misses = 3)
                .play()
                .team(A)!!
                .eliminated,
        )
    }

    private val places = GameConfig(endCondition = EndCondition.PlayForPlaces)

    @Test
    fun `V9 V10 jouer les places`() {
        var s = newSet(places, soloTeams(3)).given(A, score = 45).given(C, score = 48).play(5)
        assertEquals(1, s.team(A)!!.finishedRank)
        assertEquals(SetStatus.IN_PROGRESS, s.status)
        assertEquals(B, s.turn)
        s = s.play(1)
        assertEquals(C, s.turn)
        s = s.play(1, 2)
        assertEquals(SetStatus.FINISHED, s.status)
        assertEquals(listOf(A, C, B), s.ranking)
    }

    @Test
    fun `apres une place l'equipe est sautee`() {
        val s =
            newSet(places, soloTeams(3))
                .given(A, score = 45)
                .play(5)
                .play(1)
                .play(1)
        assertEquals(B, s.turn)
    }

    @Test
    fun `V11 jouer les places puis elimination`() {
        val s =
            newSet(places, soloTeams(3))
                .given(A, score = 50, finishedRank = 1)
                .given(B, misses = 2)
                .turnOf(B)
                .play()
        assertEquals(SetStatus.FINISHED, s.status)
        assertEquals(listOf(A, C, B), s.ranking)
    }

    private val catchUp = GameConfig(catchUpReset = true)

    @Test
    fun `V12 rejoindre un score fait redescendre l'autre`() {
        val s = newSet(catchUp).given(A, score = 20).given(B, score = 30).play(10)
        assertEquals(30, s.score(A))
        assertEquals(25, s.score(B))
    }

    @Test
    fun `V13 pas d'effet sans egalite exacte`() {
        val s = newSet(catchUp).given(A, score = 20).given(B, score = 30).play(12)
        assertEquals(32, s.score(A))
        assertEquals(30, s.score(B))
    }

    @Test
    fun `rejoindre un score sous la moitie de la cible est sans effet`() {
        val s = newSet(catchUp).given(B, score = 10).play(10)
        assertEquals(10, s.score(B))
    }

    @Test
    fun `handicap score de depart cible et exemption propres a une equipe`() {
        val config =
            GameConfig(
                teamOverrides =
                    mapOf(
                        A to TeamOverride(startScore = 20, targetScore = 30, exemptFromMissLimit = true),
                    ),
            )
        assertTrue(config.handicap)
        assertFalse(config.isStandard)
        val s = newSet(config)
        assertEquals(20, s.score(A))
        assertEquals(0, s.score(B))
        assertFalse(
            s
                .given(A, misses = 2)
                .play()
                .team(A)!!
                .eliminated,
        )
        val won = s.play(10)
        assertEquals(SetStatus.FINISHED, won.status)
        assertEquals(mapOf(A to 30, B to 0), won.setPoints)
    }

    @Test
    fun `regles standard`() {
        assertTrue(GameConfig.Classic.isStandard)
        assertTrue(GameConfig.ImoTournament.isStandard)
        assertFalse(GameConfig(targetScore = 40).isStandard)
        assertFalse(GameConfig(overshoot = Overshoot.ResetTo(0)).isStandard)
    }
}
