package fr.molkky.core.scoring

import fr.molkky.core.model.GameConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** `SCORING_SPEC.md` § 10. */
class MatchTest {
    private fun result(vararg points: Pair<fr.molkky.core.model.TeamId, Int>) =
        SetResult(points.sortedByDescending { it.second }.map { it.first }, points.toMap())

    private val ab = listOf(A, B)

    @Test
    fun `M1 deux manches gagnees terminent le match`() {
        val results = listOf(result(A to 50, B to 20), result(A to 50, B to 10))
        assertEquals(MatchStatus.Finished(A), matchStatus(MatchConfig.Classic, ab, results))
        assertEquals(MatchStatus.NextSet(2, listOf(B, A)), matchStatus(MatchConfig.Classic, ab, results.take(1)))
    }

    @Test
    fun `M2 la belle est commencee par l'equipe qui a le plus de points`() {
        val results = listOf(result(A to 50, B to 32), result(A to 41, B to 50))
        assertEquals(MatchStatus.NextSet(3, listOf(A, B)), matchStatus(MatchConfig.Classic, ab, results))
        val reversed = listOf(result(A to 30, B to 50), result(A to 50, B to 41))
        assertEquals(MatchStatus.NextSet(3, listOf(B, A)), matchStatus(MatchConfig.Classic, ab, reversed))
    }

    @Test
    fun `M3 egalite de points Molkkout puis le vainqueur commence`() {
        val results = listOf(result(A to 50, B to 40), result(A to 40, B to 50))
        assertEquals(MatchStatus.MolkkoutRequired(3), matchStatus(MatchConfig.Classic, ab, results))
        assertEquals(
            MatchStatus.NextSet(3, listOf(B, A)),
            matchStatus(MatchConfig.Classic, ab, results, molkkoutWinner = B),
        )
    }

    @Test
    fun `egalite sans departage on alterne`() {
        val results = listOf(result(A to 50, B to 40), result(A to 40, B to 50))
        val config = MatchConfig(tieBreak = TieBreak.None)
        assertEquals(MatchStatus.NextSet(3, listOf(A, B)), matchStatus(config, ab, results))
    }

    @Test
    fun `M4 alternance`() {
        val config = MatchConfig(setsToWin = 3, nextStarter = NextStarter.Alternate)
        val results = listOf(result(A to 50, B to 0), result(A to 0, B to 50))
        assertEquals(MatchStatus.NextSet(1, listOf(A, B)), matchStatus(config, ab, emptyList()))
        assertEquals(MatchStatus.NextSet(2, listOf(B, A)), matchStatus(config, ab, results.take(1)))
        assertEquals(MatchStatus.NextSet(3, listOf(A, B)), matchStatus(config, ab, results))
    }

    @Test
    fun `M5 le perdant commence`() {
        val config = MatchConfig(nextStarter = NextStarter.LoserStarts)
        assertEquals(MatchStatus.NextSet(2, listOf(B, A)), matchStatus(config, ab, listOf(result(A to 50, B to 10))))
    }

    @Test
    fun `M6 a trois equipes l'ordre relatif est conserve`() {
        val abc = listOf(A, B, C)
        val config = MatchConfig(setsToWin = 3, nextStarter = NextStarter.Alternate)
        val results = listOf(result(A to 50, B to 0, C to 0), result(A to 0, B to 50, C to 0))
        assertEquals(MatchStatus.NextSet(2, listOf(B, C, A)), matchStatus(config, abc, results.take(1)))
        assertEquals(MatchStatus.NextSet(3, listOf(C, A, B)), matchStatus(config, abc, results))
    }

    @Test
    fun `belle a trois equipes avec DecisiveByPoints on alterne`() {
        val abc = listOf(A, B, C)
        val results = listOf(result(A to 50, B to 0, C to 0), result(A to 0, B to 50, C to 0))
        assertEquals(MatchStatus.NextSet(3, listOf(C, A, B)), matchStatus(MatchConfig.Classic, abc, results))
    }

    @Test
    fun `M7 les points de manche IMO sont enregistres`() {
        val set =
            newSet(GameConfig.ImoTournament)
                .given(A, score = 30)
                .given(B, misses = 2)
                .turnOf(B)
                .play()
        assertEquals(SetResult(listOf(A, B), mapOf(A to 50, B to 0)), set.toResult())
        assertNull(newSet().toResult())
    }

    @Test
    fun `M8 composition modifiable entre deux manches seulement`() {
        assertEquals(ScoringError.LINEUP_LOCKED, checkLineupChange(newSet()))
        assertNull(checkLineupChange(newSet().given(A, score = 45).play(5)))
        assertNull(checkLineupChange(null))
    }

    @Test
    fun `preset IMO`() {
        assertEquals(NextStarter.Alternate, MatchConfig.ImoTournament.nextStarter)
        assertEquals(A, SetResult(listOf(A, B), emptyMap()).winner)
    }
}
