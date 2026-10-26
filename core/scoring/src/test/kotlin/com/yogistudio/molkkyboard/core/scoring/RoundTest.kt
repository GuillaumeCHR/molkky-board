package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.EndCondition
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.TeamId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `SCORING_SPEC.md` § 16 : tour courant. W22 (Mölkkout) est couvert par `MolkkoutTest`, qui a son propre
 * compteur ; W23 (rejeu ≡ suite de `reduce`) par `PropertyTest`.
 */
class RoundTest {
    private val dana = TeamId("D")

    private fun three() = newSet(teams = soloTeams(3))

    /** Joue [count] lancers d'un point. */
    private fun SetState.ones(count: Int): SetState = (1..count).fold(this) { s, _ -> s.play(1) }

    private fun SetState.apply(event: PlayEvent): SetState = reduce(this, event).getOrThrow()

    @Test
    fun `W1 une manche commence au tour 1`() {
        val s = three()
        assertEquals(1, s.round)
        assertEquals(A, s.turn)
    }

    @Test
    fun `W2 le tour change quand chaque equipe a lance`() {
        val s = three().ones(2)
        assertEquals(1, s.round)
        assertEquals(2, s.play(1).round)
        assertEquals(A, s.play(1).turn)
    }

    @Test
    fun `W3 une equipe eliminee en cours de tour ne compte plus`() {
        // B rate à chaque tour ; il est éliminé au tour 3.
        var s =
            three()
                .play(1)
                .play()
                .play(1)
                .play(1)
                .play()
                .play(1)
                .play(1)
        s = s.play()
        assertTrue(s.team(B)!!.eliminated)
        assertEquals(3, s.round)
        assertEquals(C, s.turn)
        s = s.play(1)
        assertEquals(4, s.round)
        s = s.play(1)
        assertEquals(C, s.turn)
        assertEquals(4, s.round)
    }

    @Test
    fun `W4 la derniere equipe de l'ordre eliminee termine le tour`() {
        val s =
            three()
                .ones(2)
                .play()
                .ones(2)
                .play()
                .ones(2)
                .play()
        assertTrue(s.team(C)!!.eliminated)
        assertEquals(4, s.round)
        assertEquals(A, s.turn)
    }

    @Test
    fun `W5 une faute consomme le lancer dans le tour`() {
        val s = three().ones(4).fault(FaultKind.TRESPASS)
        assertEquals(C, s.turn)
        assertEquals(2, s.round)
    }

    @Test
    fun `W6 un lancer mauvaise equipe est hors tour`() {
        var s = three().ones(4).apply(wrongTeam(C, chloe, 4))
        assertEquals(2, s.round)
        assertEquals(B, s.turn)
        // Le lancer perdu de C est consommé à son passage, dans le tour 2.
        s = s.play(1)
        assertEquals(A, s.turn)
        assertEquals(3, s.round)
    }

    @Test
    fun `W7 un retardataire rejoint le tour en cours`() {
        var s = three().ones(7).apply(AddPlayer(nextId(), PlayerIds.anna, dana))
        assertEquals(3, s.round)
        s = s.ones(2)
        assertEquals(dana, s.turn)
        assertEquals(3, s.round)
        s = s.play(1)
        assertEquals(4, s.round)
        assertEquals(A, s.turn)
    }

    @Test
    fun `W8 un retardataire entre deux tours lance en dernier dans le nouveau tour`() {
        var s = three().ones(9).apply(AddPlayer(nextId(), PlayerIds.anna, dana))
        assertEquals(4, s.round)
        s = s.ones(3)
        assertEquals(dana, s.turn)
        assertEquals(5, s.play(1).round)
    }

    @Test
    fun `W9 un joueur ajoute a une equipe ne change pas le tour`() {
        val s =
            newSet(teams = listOf(lineup(A, alice, arthur), lineup(B, bruno)))
                .ones(3)
                .apply(AddPlayer(nextId(), PlayerIds.anna, A))
        assertEquals(2, s.round)
    }

    @Test
    fun `W10 le depart de la derniere equipe du tour le termine`() {
        val s = three().ones(5).apply(RemovePlayer(nextId(), chloe))
        assertEquals(3, s.round)
        assertEquals(A, s.turn)
    }

    @Test
    fun `W11 une equipe qui part apres avoir lance ne retient pas le tour`() {
        var s = three().ones(4).apply(RemovePlayer(nextId(), alice))
        assertEquals(2, s.round)
        assertEquals(B, s.turn)
        s = s.ones(2)
        assertEquals(3, s.round)
        assertEquals(B, s.turn)
    }

    @Test
    fun `W12 une suppression peut ramener au tour precedent`() {
        val log = LogBuilder(teams = soloTeams(3))
        log.play(1)
        log.play(1)
        val t3 = log.play(1)
        assertEquals(2, log.state.round)
        log.add(Delete(nextId(), t3.id))
        assertEquals(1, log.state.round)
        assertEquals(C, log.state.turn)
    }

    @Test
    fun `W13 une correction qui annule une elimination garde le tour`() {
        val log = LogBuilder(teams = soloTeams(3))
        repeat(2) {
            log.play(1)
            log.play()
            log.play(1)
        }
        log.play(1)
        val thirdMiss = log.play()
        log.play(1)
        log.play(1)
        assertTrue(log.state.team(B)!!.eliminated)
        assertEquals(C, log.state.turn)
        log.add(Correct(nextId(), thirdMiss.id, Throw(nextId(), pins(5))))
        assertFalse(log.state.team(B)!!.eliminated)
        assertEquals(4, log.state.round)
        assertEquals(B, log.state.turn)
    }

    @Test
    fun `W14 une correction peut faire revenir une equipe au tour precedent`() {
        val log = LogBuilder(teams = soloTeams(3))
        repeat(2) {
            log.play(1)
            log.play(1)
            log.play()
        }
        log.play(1)
        log.play(1)
        val thirdMiss = log.play()
        log.play(1)
        log.play(1)
        assertEquals(5, log.state.round)
        log.add(Correct(nextId(), thirdMiss.id, Throw(nextId(), pins(5))))
        assertEquals(4, log.state.round)
        assertEquals(C, log.state.turn)
    }

    @Test
    fun `W15 victoire au tour de l'evenement decisif`() {
        val s = newSet().ones(10).given(A, score = 45).play(5)
        assertEquals(SetStatus.FINISHED, s.status)
        assertEquals(6, s.round)
    }

    @Test
    fun `W16 fin par elimination au tour de l'elimination`() {
        val s =
            newSet(teams = listOf(lineup(B, bruno), lineup(A, alice)))
                .play()
                .play(1)
                .play()
                .play(1)
                .play()
        assertEquals(SetStatus.FINISHED, s.status)
        assertEquals(A, s.winner)
        assertEquals(3, s.round)
    }

    @Test
    fun `W17 fin par depart entre deux tours`() {
        val s = newSet().ones(4).apply(RemovePlayer(nextId(), bruno))
        assertEquals(SetStatus.FINISHED, s.status)
        assertEquals(3, s.round)
    }

    @Test
    fun `W18 une equipe classee sort du decompte des tours`() {
        val config = GameConfig(endCondition = EndCondition.PlayForPlaces)
        var s = newSet(config, soloTeams(3)).ones(12).given(A, score = 45).play(5)
        assertEquals(1, s.team(A)!!.finishedRank)
        assertEquals(5, s.round)
        s = s.ones(2)
        assertEquals(6, s.round)
        assertEquals(B, s.turn)
    }

    @Test
    fun `W19 l'abandon fige le tour`() {
        val s = three().ones(9).apply(Abandon(nextId()))
        assertEquals(SetStatus.ABANDONED, s.status)
        assertEquals(4, s.round)
    }

    @Test
    fun `W20 reordonner entre deux tours ne change pas le tour`() {
        val s = three().ones(3).apply(Reorder(nextId(), listOf(C, A, B)))
        assertEquals(2, s.round)
        assertEquals(C, s.turn)
    }

    @Test
    fun `W21 le deroule porte le tour de chaque evenement`() {
        val log = LogBuilder(teams = soloTeams(3))
        repeat(4) { log.play(1) }
        val entries = describe(three(), log.result().effectiveLog)
        assertEquals(listOf(1, 1, 1, 2), entries.map { it.round })
    }

    @Test
    fun `une equipe partie qui revient apres sa place attend le tour suivant`() {
        // Ordre A, B, C : B part au tour 1, puis revient au tour 2 alors que C est au tour (sa place est passée).
        var s = three().play(1).apply(RemovePlayer(nextId(), bruno)).ones(2)
        assertEquals(2, s.round)
        assertEquals(C, s.turn)
        s = s.apply(AddPlayer(nextId(), PlayerIds.anna, B))
        assertEquals(2, s.round)
        s = s.play(1)
        assertEquals(3, s.round)
        assertEquals(A, s.turn)
        assertEquals(B, s.play(1).turn)
    }

    @Test
    fun `une equipe partie qui revient avant sa place lance dans le tour en cours`() {
        // B part au tour 1 ; elle revient au tour 2 alors que A est au tour : elle lance après A.
        var s = three().play(1).apply(RemovePlayer(nextId(), bruno)).play(1)
        assertEquals(2, s.round)
        assertEquals(A, s.turn)
        s = s.apply(AddPlayer(nextId(), PlayerIds.anna, B)).play(1)
        assertEquals(B, s.turn)
        assertEquals(2, s.round)
    }
}
