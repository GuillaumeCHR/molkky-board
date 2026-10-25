package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.model.TeamOverride
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Cas limites hors spec, nécessaires à une couverture complète des branches du moteur. */
class EdgeCaseTest {
    @Test
    fun `une manche terminee n'a ni equipe ni joueur courant`() {
        val finished = newSet().given(A, score = 45).play(5)
        assertNull(finished.currentTeam)
        assertNull(finished.currentPlayer)
        assertNull(newSet().winner)
        assertEquals(A, finished.winner)
    }

    @Test
    fun `surcharge sans exemption l'equipe peut etre eliminee`() {
        val config = GameConfig(teamOverrides = mapOf(A to TeamOverride(startScore = 10)))
        assertTrue(
            newSet(config)
                .given(A, misses = 2)
                .play()
                .team(A)!!
                .eliminated,
        )
    }

    @Test
    fun `une equipe eliminee ne peut pas commettre de faute de mauvaise equipe`() {
        val s = newSet(teams = soloTeams(3)).given(C, eliminated = true)
        assertEquals(ScoringError.INVALID_EVENT, reduce(s, WrongTeam(nextId(), C, chloe, listOf(1))).errorOrNull())
        assertEquals(ScoringError.DUPLICATE_PIN, reduce(s, WrongTeam(nextId(), B, bruno, listOf(2, 2))).errorOrNull())
        assertEquals(ScoringError.INVALID_PIN, reduce(s, WrongTeam(nextId(), B, bruno, listOf(0))).errorOrNull())
    }

    @Test
    fun `rejoindre le score d'une equipe eliminee est sans effet`() {
        val s =
            newSet(GameConfig(catchUpReset = true), soloTeams(3))
                .given(B, score = 30, eliminated = true)
                .given(C, score = 40)
                .given(A, score = 20)
                .play(10)
        assertEquals(30, s.score(B))
    }

    @Test
    fun `configurations negatives invalides`() {
        listOf(
            GameConfig(teamOverrides = mapOf(A to TeamOverride(startScore = -1))),
            GameConfig(
                onMissLimit =
                    com.yogistudio.molkkyboard.core.model.MissPenalty
                        .ResetScore(-1),
            ),
        ).forEach { assertEquals(ScoringError.INVALID_CONFIG, start(it, soloTeams()).errorOrNull()) }
    }

    @Test
    fun `faute d'une equipe inconnue`() =
        assertEquals(
            ScoringError.UNKNOWN_TEAM,
            reduce(newSet(), Fault(nextId(), C, chloe, FaultKind.ABSENT)).errorOrNull(),
        )

    @Test
    fun `points de manche manquants comptes comme zero`() {
        val results = listOf(SetResult(listOf(A, B), mapOf(A to 50)), SetResult(listOf(B, A), mapOf(B to 50)))
        assertEquals(MatchStatus.MolkkoutRequired(3), matchStatus(MatchConfig.Classic, listOf(A, B), results))
    }

    @Test
    fun `Molkkout entre equipes de tailles differentes`() {
        val teams =
            listOf(
                TeamLineup(A, listOf(PlayerId("A1"), PlayerId("A2"))),
                TeamLineup(B, listOf(PlayerId("B1"), PlayerId("B2"), PlayerId("B3"))),
            )
        var s = Molkkout.start(teams).getOrThrow()
        assertEquals(4, s.throwsPerTeamInRound)
        repeat(8) { s = Molkkout.reduce(s, MolkkoutThrow(s.nextTeam!!.id, s.nextPlayer!!, emptyList())).getOrThrow() }
        assertEquals(1, s.round)
        assertEquals(3, s.throwsPerTeamInRound)
        var reversed = Molkkout.start(teams.reversed()).getOrThrow()
        assertEquals(4, reversed.throwsPerTeamInRound)
        repeat(8) {
            val team = reversed.nextTeam!!
            reversed =
                Molkkout.reduce(reversed, MolkkoutThrow(team.id, reversed.nextPlayer!!, emptyList())).getOrThrow()
        }
        assertEquals(3, reversed.throwsPerTeamInRound)
    }
}
