package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** `SCORING_SPEC.md` § 11. */
class MolkkoutTest {
    private fun teams(size: Int) =
        listOf(
            TeamLineup(A, (1..size).map { PlayerId("A$it") }),
            TeamLineup(B, (1..size).map { PlayerId("B$it") }),
        )

    private fun MolkkoutState.next(vararg pins: Int): MolkkoutState =
        Molkkout.reduce(this, MolkkoutThrow(nextTeam!!.id, nextPlayer!!, pins.toList())).getOrThrow()

    @ParameterizedTest(name = "équipes de {0} : {1}")
    @CsvSource("1, ABBAAB", "2, ABBAABBA", "3, ABBAAB", "4, ABBAABBA")
    fun `K1 a K4 alternance des equipes`(size: Int, expected: String) {
        var s = Molkkout.start(teams(size)).getOrThrow()
        val order =
            expected.indices.joinToString("") {
                s.nextTeam!!
                    .id.value
                    .also { s = s.next(4) }
            }
        assertEquals(expected, order)
        // Série complète à égalité : on passe aux tours supplémentaires.
        assertEquals(1, s.round)
        assertNull(s.winner)
    }

    @Test
    fun `K2 les joueurs alternent dans leur equipe`() {
        var s = Molkkout.start(teams(2)).getOrThrow()
        val players = (1..8).map { s.nextPlayer!!.value.also { s = s.next() } }
        assertEquals(listOf("A1", "B1", "B2", "A2", "A1", "B1", "B2", "A2"), players)
    }

    @Test
    fun `K5 decompte`() {
        assertEquals(12, points(listOf(12)))
        assertEquals(2, points(listOf(6, 4)))
        assertEquals(0, points(emptyList()))
    }

    @Test
    fun `K6 la quille 7 n'est pas sur le terrain`() {
        val s = Molkkout.start(teams(1)).getOrThrow()
        assertEquals(
            ScoringError.INVALID_PIN,
            Molkkout.reduce(s, MolkkoutThrow(A, PlayerId("A1"), listOf(7))).errorOrNull(),
        )
    }

    @Test
    fun `K7 le plus haut total gagne`() {
        val throws =
            listOf(A to 12, B to 6, B to 4, A to 4, A to 4, B to 4)
                .map { (team, pin) -> MolkkoutThrow(team, PlayerId("${team.value}1"), listOf(pin)) }
        val s = Molkkout.replay(teams(1), throws).getOrThrow()
        assertEquals(mapOf(A to 20, B to 14), s.totals)
        assertEquals(A, s.winner)
        assertNull(s.nextTeam)
        assertNull(s.nextPlayer)
    }

    @Test
    fun `K8 egalite chaque joueur relance une fois jusqu'au departage`() {
        var s = Molkkout.start(teams(1)).getOrThrow()
        repeat(6) { s = s.next(6) }
        assertEquals(1, s.round)
        assertEquals(1, s.throwsPerTeamInRound)
        s = s.next(4).next(4)
        assertEquals(2, s.round)
        assertEquals(A, s.nextTeam!!.id)
        s = s.next(4).next(12)
        assertEquals(B, s.winner)
    }

    @Test
    fun `erreurs du Molkkout`() {
        assertEquals(ScoringError.INVALID_CONFIG, Molkkout.start(teams(1) + TeamLineup(C, listOf(chloe))).errorOrNull())
        assertEquals(
            ScoringError.EMPTY_TEAM,
            Molkkout.start(listOf(TeamLineup(A, emptyList()), teams(1)[1])).errorOrNull(),
        )
        val s = Molkkout.start(teams(1)).getOrThrow()
        assertEquals(
            ScoringError.NOT_YOUR_TURN,
            Molkkout.reduce(s, MolkkoutThrow(B, PlayerId("B1"), emptyList())).errorOrNull(),
        )
        assertEquals(
            ScoringError.NOT_YOUR_TURN,
            Molkkout.reduce(s, MolkkoutThrow(A, PlayerId("B1"), emptyList())).errorOrNull(),
        )
        assertEquals(
            ScoringError.DUPLICATE_PIN,
            Molkkout.reduce(s, MolkkoutThrow(A, PlayerId("A1"), listOf(4, 4))).errorOrNull(),
        )
        var won = s
        listOf(12, 0, 0, 0, 0, 0).forEach { won = won.next(*if (it == 0) intArrayOf() else intArrayOf(it)) }
        assertEquals(A, won.winner)
        assertEquals(
            ScoringError.SET_FINISHED,
            Molkkout.reduce(won, MolkkoutThrow(A, PlayerId("A1"), emptyList())).errorOrNull(),
        )
    }
}
