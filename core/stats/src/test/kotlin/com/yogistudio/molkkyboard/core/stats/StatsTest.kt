package com.yogistudio.molkkyboard.core.stats

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamOverride
import com.yogistudio.molkkyboard.core.scoring.Abandon
import com.yogistudio.molkkyboard.core.scoring.Fault
import com.yogistudio.molkkyboard.core.scoring.FaultKind
import com.yogistudio.molkkyboard.core.scoring.PointsKind
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import com.yogistudio.molkkyboard.core.scoring.Undo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

// Cas de `docs/STATS_SPEC.md` : chaque test porte l'identifiant de sa ligne.
class StatsTest {
    private val ab = listOf(T("A"), T("B"))
    private val ba = listOf(T("B"), T("A"))
    private val players = RankingFilter(HallTab.PLAYERS)
    private val teams = RankingFilter(HallTab.TEAMS)

    @Nested
    inner class Summary {
        @Test
        fun `R1 lancers de l'équipe gagnante, fautes comprises`() {
            val log = listOf(Fault(nextId(), FaultKind.DROPPED), pins(1)) + perfectLog()
            val summary = summarize(game("g", 1, ab, log)).single()
            assertEquals("A", summary.winner)
            assertEquals(6, summary.winnerTeam!!.throws)
            assertEquals(5, summary.players.getValue(PlayerId("A")).throws)
        }

        @Test
        fun `R2 manche en cours sans vainqueur`() {
            val summary = summarize(game("g", 1, ab, listOf(pins(5)))).single()
            assertNull(summary.winner)
            assertFalse(summary.eligible)
        }

        @Test
        fun `R3 abandon et modification après la fin rendent la manche inéligible`() {
            val abandoned = summarize(game("g1", 1, ab, listOf(pins(5), Abandon(nextId())))).single()
            val corrected = summarize(game("g2", 1, ab, correctedAfterFinish(perfectLog()))).single()
            val flagged = summarize(game("g3", 1, ab, perfectLog(), abandoned = true)).single()
            assertFalse(abandoned.eligible)
            assertFalse(corrected.eligible)
            assertFalse(flagged.eligible)
        }

        @Test
        fun `R4 annulation immédiate du lancer gagnant tolérée`() {
            val log = perfectLog()
            val undone = log + Undo(nextId(), log.last().id) + pins(2)
            assertTrue(summarize(game("g", 1, ab, undone)).single().eligible)
        }

        @Test
        fun `R5 lignes de joueur - ratés, points, quilles seules, série`() {
            val log = listOf(pins(), pins(1), pins(7), pins(1), pins(3, 4), pins(1), pins(9))
            val line = summarize(game("g", 1, ab, log)).single().players.getValue(PlayerId("A"))
            assertEquals(4, line.throws)
            assertEquals(1, line.misses)
            assertEquals(18, line.points)
            assertEquals(mapOf(7 to 1, 9 to 1), line.singlePins)
            assertEquals(3, line.bestStreak)
        }

        @Test
        fun `R6 saisie rapide - compte partout sauf les quilles seules`() {
            val quick = Throw(nextId(), ThrowResult.Points(7, PointsKind.SINGLE_PIN))
            val line = summarize(game("g", 1, ab, listOf(quick))).single().players.getValue(PlayerId("A"))
            assertEquals(1, line.throws)
            assertEquals(7, line.points)
            assertTrue(line.singlePins.isEmpty())
        }
    }

    @Nested
    inner class Wins {
        @Test
        fun `W1 tri par victoires`() {
            val s =
                summaries(
                    game("g1", 1, ab, perfectLog()),
                    game("g2", 2, ab, perfectLog()),
                    game("g3", 3, ba, perfectLog()),
                )
            val ranking = winsRanking(s, players)
            assertEquals(listOf("A" to 2, "B" to 1), ranking.map { it.memberKey to it.wins })
            assertEquals(3, ranking.first().played)
            assertEquals(2f / 3, ranking.first().rate, 1e-6f)
            assertEquals("g2", ranking.first().lastWinGameId)
        }

        @Test
        fun `W2 égalité - le premier à atteindre le total garde le rang`() {
            val s = summaries(game("g1", 1, ba, perfectLog()), game("g2", 2, ab, perfectLog()))
            assertEquals(listOf("B", "A"), winsRanking(s, players).map { it.memberKey })
        }

        @Test
        fun `W3 manches inéligibles exclues`() {
            val s =
                summaries(
                    game("g1", 1, ab, correctedAfterFinish(perfectLog())),
                    game("g2", 2, ab, perfectLog(), abandoned = true),
                )
            assertTrue(winsRanking(s, players).isEmpty())
        }

        @Test
        fun `W4 le mode Ludique compte dans les victoires`() {
            val s = summaries(game("g", 1, ab, perfectLog(), config = GameConfig.Playful))
            assertEquals(1, winsRanking(s, players).single().wins)
        }

        @Test
        fun `W5 onglets - solo dans Joueurs, équipes dans Équipes, filtre de taille`() {
            val duo = listOf(T("AA", listOf("a1", "a2")), T("B"))
            val s = summaries(game("g1", 1, ab, perfectLog()), game("g2", 2, duo, perfectLog()))
            assertEquals(listOf("A"), winsRanking(s, players).map { it.memberKey })
            assertEquals(listOf("AA"), winsRanking(s, teams).map { it.memberKey })
            assertEquals(listOf("AA"), winsRanking(s, teams.copy(teamSize = 2)).map { it.memberKey })
            assertTrue(winsRanking(s, teams.copy(teamSize = 3)).isEmpty())
            assertEquals(2, winsRanking(s, teams).single().size)
        }

        @Test
        fun `W6 période`() {
            val s = summaries(game("old", 10, ab, perfectLog()), game("new", 100, ba, perfectLog()))
            assertEquals(listOf("B"), winsRanking(s, players.copy(since = 50)).map { it.memberKey })
        }

        @Test
        fun `W7 nom le plus récent d'une équipe renommée`() {
            val renamed =
                game("g2", 2, ab, perfectLog()).let { g ->
                    g.copy(
                        teams =
                            g.teams.map {
                                if (it.memberKey ==
                                    "A"
                                ) {
                                    it.copy(name = "Les Bouleaux")
                                } else {
                                    it
                                }
                            },
                    )
                }
            val s = summaries(game("g1", 1, ab, perfectLog()), renamed)
            assertEquals("Les Bouleaux", winsRanking(s, players).first().name)
        }
    }

    @Nested
    inner class Fastest {
        @Test
        fun `F1 partie parfaite en 5 lancers`() {
            val entry = fastestRanking(summaries(game("g", 1, ab, perfectLog())), players).single()
            assertEquals(5, entry.throws)
            assertTrue(entry.perfect)
        }

        @Test
        fun `F2 une ligne par équipe, sa meilleure`() {
            val s =
                summaries(game("g1", 1, ab, slowLog()), game("g2", 2, ab, perfectLog()), game("g3", 3, ba, slowLog()))
            val ranking = fastestRanking(s, players)
            assertEquals(listOf("A" to 5, "B" to 9), ranking.map { it.memberKey to it.throws })
            assertFalse(ranking[1].perfect)
        }

        @Test
        fun `F3 égalité - la plus ancienne, puis l'identifiant de partie`() {
            val s =
                summaries(game("z", 5, ba, perfectLog()), game("y", 1, ab, perfectLog()), game("x", 1, ba, slowLog()))
            assertEquals(listOf("y", "z"), fastestRanking(s, players).map { it.gameId })
            val sameDay = summaries(game("b", 1, ab, perfectLog()), game("a", 1, ba, perfectLog()))
            assertEquals(listOf("a", "b"), fastestRanking(sameDay, players).map { it.gameId })
        }

        @Test
        fun `F4 seulement les règles officielles - ni Ludique ni handicap`() {
            val handicap = GameConfig.Classic.copy(teamOverrides = mapOf(TeamId("B") to TeamOverride(startScore = 10)))
            val s =
                summaries(
                    game("g1", 1, ab, perfectLog(), config = GameConfig.Playful),
                    game("g2", 2, ab, perfectLog(), config = handicap),
                )
            assertTrue(fastestRanking(s, players).isEmpty())
            assertEquals(2, winsRanking(s, players).single().wins)
        }

        @Test
        fun `F5 le règlement IMO compte comme officiel`() {
            val s = summaries(game("g", 1, ab, perfectLog(), config = GameConfig.ImoTournament))
            assertEquals(1, fastestRanking(s, players).size)
        }
    }

    @Nested
    inner class Records {
        @Test
        fun `N1 bat le record existant`() {
            val before = summaries(game("g1", 1, ab, slowLog()))
            val added = summaries(game("g2", 2, ba, perfectLog())).single()
            assertTrue(isNewRecord(before + added, added))
        }

        @Test
        fun `N2 la première victoire n'est pas un record, ni une égalité`() {
            val first = summaries(game("g1", 1, ab, perfectLog())).single()
            assertFalse(isNewRecord(listOf(first), first))
            val tie = summaries(game("g2", 2, ba, perfectLog())).single()
            assertFalse(isNewRecord(listOf(first, tie), tie))
        }

        @Test
        fun `N3 une manche Ludique ne bat pas de record`() {
            val before = summaries(game("g1", 1, ab, slowLog()))
            val added = summaries(game("g2", 2, ab, perfectLog(), config = GameConfig.Playful)).single()
            assertFalse(isNewRecord(before, added))
        }
    }

    @Nested
    inner class Players {
        @Test
        fun `P1 moyenne, taux de ratés, victoires et progression`() {
            val s = summaries(game("g1", 1, ab, perfectLog()), game("g2", 2, ba, firstWins(12, 12, 12, 12, 2)))
            val stats = playerStats(s, PlayerId("A"))
            assertEquals(2, stats.sets)
            assertEquals(1, stats.wins)
            assertEquals(9, stats.throws)
            assertEquals(0, stats.misses)
            assertEquals(54, stats.points)
            assertEquals(6f, stats.average, 1e-6f)
            assertEquals(0f, stats.missRate)
            assertEquals(listOf(10f, 1f), stats.averages.map { it.average })
            assertEquals(5, stats.fastestWin)
            assertEquals(5, stats.bestStreak)
        }

        @Test
        fun `P2 joueur sans lancer`() {
            val stats = playerStats(emptyList(), PlayerId("Z"))
            assertEquals(0f, stats.average)
            assertEquals(0f, stats.missRate)
            assertNull(stats.fastestWin)
        }
    }

    @Nested
    inner class SeriesCases {
        @Test
        fun `Z1 manches consécutives des mêmes équipes`() {
            val s =
                summaries(game("g1", 1, ab, perfectLog()), game("g2", 2, ba, slowLog()), game("g3", 3, ab, slowLog()))
            val series = series(s, "g3", 0)!!
            assertEquals(3, series.sets.size)
            assertEquals(listOf("A" to 2, "B" to 1), series.rows.map { it.memberKey to it.wins })
            assertEquals(listOf(50, 8, 50), series.rows.first().scores)
        }

        @Test
        fun `Z2 une autre composition coupe la série`() {
            val abc = listOf(T("A"), T("B"), T("C"))
            val threeWay = (1..4).flatMap { listOf(pins(12), pins(1), pins(1)) } + pins(2)
            val s =
                summaries(
                    game("g1", 1, ab, perfectLog()),
                    game("g2", 2, abc, threeWay),
                    game("g3", 3, ab, perfectLog()),
                    game("g4", 4, ab, perfectLog()),
                )
            assertEquals(listOf("g3", "g4"), series(s, "g4", 0)!!.sets.map { it.gameId })
        }

        @Test
        fun `Z3 une seule manche ou manche inconnue - pas de série`() {
            val s = summaries(game("g1", 1, ab, perfectLog()))
            assertNull(series(s, "g1", 0))
            assertNull(series(s, "nope", 0))
        }

        @Test
        fun `Z4 à égalité de victoires, le total départage`() {
            val s = summaries(game("g1", 1, ab, perfectLog()), game("g2", 2, ba, slowLog()))
            assertEquals(listOf("A", "B"), series(s, "g2", 0)!!.rows.map { it.memberKey })
        }
    }
}
