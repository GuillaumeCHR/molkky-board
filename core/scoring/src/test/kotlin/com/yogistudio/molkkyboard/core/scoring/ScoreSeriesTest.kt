package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.model.TeamOverride
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Série de scores par tour (`SCORING_SPEC.md` § 18, cas S1 à S10). */
class ScoreSeriesTest {
    private fun pins(vararg pins: Int): SetEvent = Throw(nextId(), ThrowResult.Pins(pins.toList()))

    private fun series(
        log: List<SetEvent>,
        teams: List<TeamLineup> = soloTeams(),
        config: GameConfig = GameConfig.Classic,
    ): ScoreSeries {
        val effective = replay(config, teams, log).getOrThrow().effectiveLog
        val series = scoreSeries(config, teams, effective)
        assertNotNull(series)
        return series!!
    }

    private fun ScoreSeries.of(team: TeamId) = teams.first { it.team == team }.points

    /** A marque [first] à chaque tour, B fait 1 entre deux lancers de A. */
    private fun firstWins(vararg first: Int): List<SetEvent> =
        first.toList().flatMapIndexed { i, p ->
            if (i ==
                first.lastIndex
            ) {
                listOf(pins(p))
            } else {
                listOf(pins(p), pins(1))
            }
        }

    @Test
    fun `S1 partie parfaite - un point par tour, victoire marquee`() {
        val s = series(firstWins(12, 12, 12, 12, 2))
        assertEquals(50, s.target)
        assertEquals(5, s.rounds)
        assertEquals(A, s.winner)
        assertEquals(listOf(0, 12, 24, 36, 48, 50), s.of(A).map { it.score })
        assertEquals((0..5).toList(), s.of(A).map { it.round })
        assertEquals(SeriesMark.WIN, s.of(A).last().mark)
        assertEquals(listOf(0, 1, 2, 3, 4), s.of(B).map { it.score })
        // Les passages, dans l'ordre du journal, servent au mini-reel.
        assertEquals(listOf(A, B, A, B, A, B, A, B, A), s.steps.map { it.team })
        assertNull(s.highlight)
    }

    @Test
    fun `S2 depassement - retour a 25 marque, moment fort`() {
        val s = series(firstWins(12, 12, 12, 9, 9))
        assertEquals(25, s.of(A)[5].score)
        assertEquals(SeriesMark.RESET, s.of(A)[5].mark)
        assertEquals(SeriesHighlight(A, 5, HighlightKind.RESET), s.highlight)
    }

    @Test
    fun `S3 trois rates - la ligne s'arrete sur l'elimination`() {
        val log = List(3) { listOf(pins(5), pins(5), pins()) }.flatten()
        val s = series(log, soloTeams(3))
        assertEquals(listOf(0, 0, 0, 0), s.of(C).map { it.score })
        assertEquals(SeriesMark.ELIMINATED, s.of(C).last().mark)
        assertEquals(SeriesHighlight(C, 3, HighlightKind.ELIMINATED), s.highlight)
    }

    @Test
    fun `S4 victoire par elimination - marque sur le dernier point du vainqueur`() {
        val s = series(List(3) { listOf(pins(5), pins()) }.flatten())
        assertEquals(SeriesMark.ELIMINATED, s.of(B).last().mark)
        assertEquals(SeriesMark.WIN, s.of(A).last().mark)
        assertEquals(A, s.winner)
    }

    @Test
    fun `S5 joueur arrive en retard - sa ligne part du tour d'arrivee`() {
        val d = TeamId("D")
        val log = listOf(pins(5), pins(5), AddPlayer(nextId(), PlayerId("Dana"), d), pins(5), pins(5), pins(7))
        val s = series(log)
        assertEquals(listOf(SeriesPoint(1, 0), SeriesPoint(2, 7)), s.of(d))
        // Avant son arrivée, D compte pour 0 ; ensuite, son dernier score connu.
        val dana = s.teams.first { it.team == d }
        assertEquals(0, dana.scoreAt(0))
        assertEquals(7, dana.scoreAt(5))
    }

    @Test
    fun `S6 joueur retire - sa ligne s'arrete au tour de son depart`() {
        val log = listOf(pins(5), pins(5), pins(6), pins(6), pins(7), RemovePlayer(nextId(), chloe), pins(7), pins(8))
        val s = series(log, soloTeams(3))
        assertEquals(SeriesPoint(2, 6, SeriesMark.LEFT), s.of(C).last())
    }

    @Test
    fun `S7 correction d'un lancer ancien - serie recalculee`() {
        val log = firstWins(12, 12, 12, 12, 2)
        val first = log.first() as Throw
        val s = series(log + Correct(nextId(), first.id, Throw(nextId(), ThrowResult.Pins(listOf(3)))))
        assertEquals(listOf(0, 3, 15, 27, 39, 41), s.of(A).map { it.score })
        assertNull(s.winner)
    }

    @Test
    fun `S8 remontee - le vainqueur mene de 15 points ou plus, moment fort`() {
        // B mène 36 à 3 au tour 3, puis A enchaîne et gagne.
        val log =
            listOf(pins(1), pins(12), pins(1), pins(12), pins(1), pins(12)) +
                listOf(pins(12), pins(1), pins(12), pins(1), pins(12), pins(1), pins(11))
        val s = series(log)
        assertEquals(A, s.winner)
        assertEquals(SeriesHighlight(A, 3, HighlightKind.COMEBACK), s.highlight)
    }

    @Test
    fun `S9 mode famille - la ligne part du score de depart, l'axe va a la plus haute cible`() {
        val overrides = mapOf(B to TeamOverride(startScore = 20, targetScore = 40), C to TeamOverride(startScore = 5))
        val config = GameConfig.Classic.copy(teamOverrides = overrides)
        val s = series(listOf(pins(5), pins(5)), soloTeams(3), config)
        assertEquals(listOf(0, 5), s.of(A).map { it.score })
        assertEquals(listOf(20, 25), s.of(B).map { it.score })
        assertEquals(50, s.target)
    }

    @Test
    fun `S10 aucune saisie - un point de depart par equipe`() {
        val s = series(emptyList())
        assertEquals(0, s.rounds)
        assertEquals(listOf(SeriesPoint(0, 0)), s.of(A))
        assertEquals(emptyList<SeriesStep>(), s.steps)
    }

    @Test
    fun `S11 faute et lancer de la mauvaise equipe - un passage chacun, la faute sans point`() {
        // Tour 1 : A marque 5, B commet une faute. Tour 2 : B lance à la place de A (faute « mauvaise équipe » : B perd
        // son prochain lancer régulier, `RULES.md`), puis A lance ; le tour suivant revient à A.
        val log =
            listOf(
                pins(5),
                Fault(nextId(), FaultKind.TRESPASS),
                WrongTeam(nextId(), B, bruno, ThrowResult.Pins(listOf(4))),
                pins(6),
                pins(7),
            )
        val s = series(log)
        assertEquals(listOf(A, B, B, A, A), s.steps.map { it.team })
        assertEquals(true, s.steps[1].miss)
        assertEquals(listOf(0, 1, 2), s.of(B).map { it.round })
        assertEquals(listOf(0, 5, 11, 18), s.of(A).map { it.score })
    }

    @Test
    fun `S12 vainqueur qui n'a jamais lance - point a son score de depart`() {
        val s = series(listOf(RemovePlayer(nextId(), alice)))
        assertEquals(B, s.winner)
        assertEquals(SeriesMark.LEFT, s.of(A).last().mark)
        assertEquals(SeriesPoint(1, 0, SeriesMark.WIN), s.of(B).last())
    }

    @Test
    fun `S13 manche qui ne demarre pas - pas de serie`() {
        assertNull(scoreSeries(GameConfig.Classic, emptyList(), emptyList()))
    }
}
