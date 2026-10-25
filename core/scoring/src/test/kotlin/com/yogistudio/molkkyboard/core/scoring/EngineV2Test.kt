package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamOverride
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** `SCORING_SPEC.md` § 15 : moteur v2. */
class EngineV2Test {
    @Nested
    inner class Attribution {
        /** A, B, C ; A {5}, puis {5} saisi deux fois par erreur (attribué à B), puis {3} (attribué à C). */
        private fun duplicated(): Pair<LogBuilder, Throw> {
            val log = LogBuilder(teams = soloTeams(3))
            log.play(5)
            val duplicate = log.play(5)
            log.play(3)
            return log to duplicate
        }

        @Test
        fun `A1 supprimer un doublon reattribue la suite`() {
            val (log, duplicate) = duplicated()
            assertEquals(5, log.state.score(B))
            log.add(Delete(nextId(), duplicate.id))
            val s = log.state
            assertEquals(5, s.score(A))
            assertEquals(3, s.score(B))
            assertEquals(0, s.score(C))
            assertEquals(C, s.turn)
        }

        @Test
        fun `A2 corriger un lancer ne change que sa valeur`() {
            val (log, duplicate) = duplicated()
            log.add(Correct(nextId(), duplicate.id, throwOf(7)))
            assertEquals(listOf(5, 7, 3), listOf(A, B, C).map { log.state.score(it) })
        }

        @Test
        fun `A3 A4 by n'est controle qu'a la saisie`() {
            val s = newSet()
            assertEquals(ScoringError.NOT_YOUR_TURN, checkInput(s, throwOf(4, by = bruno)))
            assertNull(checkInput(s, throwOf(4, by = alice)))
            assertNull(checkInput(s, throwOf(4)))
            // Au rejeu, le lancer appartient au lanceur courant, quel que soit `by`.
            assertEquals(4, reduce(s, throwOf(4, by = bruno)).getOrThrow().score(A))
        }

        @Test
        fun `controle de saisie apres la fin de la manche`() {
            val finished = newSet().given(A, score = 45).play(5)
            assertEquals(ScoringError.SET_FINISHED, checkInput(finished, throwOf(1, by = bruno)))
            assertNull(checkInput(newSet(), Abandon(nextId())))
        }
    }

    @Nested
    inner class QuickInput {
        private fun quick(value: Int, kind: PointsKind) = Throw(nextId(), ThrowResult.Points(value, kind))

        @ParameterizedTest(name = "{0} : {1} {2} → {3}")
        @CsvSource(
            "Q1, 7, SINGLE_PIN, 7",
            "Q2, 7, PIN_COUNT, 7",
            "Q3, 0, SINGLE_PIN, 0",
            "Q3b, 0, PIN_COUNT, 0",
            "Q2b, 12, PIN_COUNT, 12",
        )
        fun `saisie rapide valide`(case: String, value: Int, kind: PointsKind, expected: Int) {
            val s = reduce(newSet(), quick(value, kind)).getOrThrow()
            assertEquals(expected, s.score(A), case)
            assertEquals(if (expected == 0) 1 else 0, s.misses(A), case)
        }

        @ParameterizedTest(name = "{0} : {1} {2}")
        @CsvSource("Q4, 1, PIN_COUNT", "Q5, 13, SINGLE_PIN", "Q5b, 13, PIN_COUNT", "Q5c, -1, SINGLE_PIN")
        fun `saisie rapide invalide`(case: String, value: Int, kind: PointsKind) =
            assertEquals(ScoringError.INVALID_POINTS, reduce(newSet(), quick(value, kind)).errorOrNull(), case)

        @Test
        fun `Q6 partie mixte quilles et saisie rapide`() {
            val pinsOnly = newSet().play(12).play(3).play(2, 4)
            val mixed =
                reduce(newSet(), quick(12, PointsKind.SINGLE_PIN))
                    .getOrThrow()
                    .let { reduce(it, quick(3, PointsKind.SINGLE_PIN)).getOrThrow() }
                    .let { reduce(it, quick(2, PointsKind.PIN_COUNT)).getOrThrow() }
            assertEquals(pinsOnly.teams.map { it.score }, mixed.teams.map { it.score })
        }
    }

    @Nested
    inner class Deletion {
        @Test
        fun `D1 supprimer un depassement`() {
            val log = LogBuilder()
            listOf(12, 12, 12, 9).forEach {
                log.play(it)
                log.play(1)
            }
            val overshoot = log.play(9)
            assertEquals(25, log.state.score(A))
            log.add(Delete(nextId(), overshoot.id))
            assertEquals(45, log.state.score(A))
        }

        @Test
        fun `D2 supprimer un rate efface l'elimination`() {
            val log = LogBuilder()
            val first = log.play()
            log.play(1)
            log.play()
            log.play(1)
            log.play()
            assertTrue(log.state.team(A)!!.eliminated)
            log.add(Delete(nextId(), first.id))
            // Sans ce raté, la suite est réattribuée : la manche reprend.
            assertFalse(log.state.team(A)!!.eliminated)
            assertEquals(SetStatus.IN_PROGRESS, log.state.status)
        }

        @Test
        fun `D3 supprimer le lancer gagnant rouvre la manche`() {
            val log = LogBuilder()
            listOf(12, 12, 12, 9).forEach {
                log.play(it)
                log.play(1)
            }
            val win = log.play(5)
            assertEquals(SetStatus.FINISHED, log.state.status)
            log.add(Delete(nextId(), win.id))
            assertEquals(SetStatus.IN_PROGRESS, log.state.status)
            assertEquals(A, log.state.turn)
        }

        @Test
        fun `D4 une suppression qui invaliderait la suite est refusee`() {
            val log = LogBuilder()
            val first = log.play(12)
            log.play(3)
            log.add(Reorder(nextId(), listOf(B, A)))
            assertTrue(log.result().rejected.isEmpty())
            // Sans le 1er lancer, le Reorder ne tomberait plus entre deux tours : suppression refusée.
            log.add(Delete(nextId(), first.id))
            assertEquals(
                ScoringError.CORRECTION_INVALIDATES_LOG,
                log
                    .result()
                    .rejected
                    .last()
                    .error,
            )
        }

        @Test
        fun `D5 supprimer un evenement inconnu`() {
            val log = LogBuilder()
            log.play(3)
            log.add(Delete(nextId(), EventId("inconnu")))
            assertEquals(
                ScoringError.UNKNOWN_EVENT,
                log
                    .result()
                    .rejected
                    .single()
                    .error,
            )
        }
    }

    @Nested
    inner class Lineup {
        private val chloeTeam = C

        @Test
        fun `L1 un retardataire demarre a 0 en fin d'ordre et lance a la fin du tour`() {
            var s = newSet().play(3).play(4).play(5)
            s = reduce(s, AddPlayer(nextId(), chloe, chloeTeam)).getOrThrow()
            assertEquals(0, s.score(C))
            assertEquals(listOf(A, B, C), s.teams.map { it.id })
            assertEquals(B, s.turn)
            s = s.play(6)
            assertEquals(C, s.turn)
            s = s.play(7)
            assertEquals(A, s.turn)
            assertTrue(s.betweenRounds)
        }

        @Test
        fun `retardataire avec score de depart du mode famille`() {
            val config = GameConfig(teamOverrides = mapOf(A to TeamOverride(startScore = 10)))
            val s = reduce(newSet(config), AddPlayer(nextId(), chloe, chloeTeam)).getOrThrow()
            assertEquals(0, s.score(C))
        }

        @Test
        fun `L2 un joueur rejoint la rotation d'une equipe`() {
            var s = newSet(teams = listOf(lineup(A, alice, arthur), lineup(B, bruno)))
            s = reduce(s, AddPlayer(nextId(), PlayerIds.anna, A)).getOrThrow()
            val throwers = (1..6).map { s.currentPlayer.also { s = s.play(1) } }
            assertEquals(listOf(alice, bruno, arthur, bruno, PlayerIds.anna, bruno), throwers)
        }

        @Test
        fun `L3 un joueur seul part le tour passe et il est classe dernier`() {
            var s = newSet(teams = soloTeams(3)).play(5).given(B, score = 20)
            assertEquals(B, s.turn)
            s = reduce(s, RemovePlayer(nextId(), bruno)).getOrThrow()
            assertTrue(s.team(B)!!.left)
            assertEquals(C, s.turn)
            s =
                s
                    .play(2)
                    .given(A, score = 45)
                    .turnOf(A)
                    .play(5)
            assertEquals(listOf(A, C, B), s.ranking)
        }

        @Test
        fun `L4 s'il ne reste qu'un joueur il gagne`() {
            val s = reduce(newSet(), RemovePlayer(nextId(), bruno)).getOrThrow()
            assertEquals(SetStatus.FINISHED, s.status)
            assertEquals(listOf(A, B), s.ranking)
        }

        @Test
        fun `L5 la rotation continue sans le joueur parti`() {
            var s = newSet(teams = listOf(lineup(A, alice, arthur), lineup(B, bruno))).play(1).play(1)
            assertEquals(arthur, s.currentPlayer)
            s = reduce(s, RemovePlayer(nextId(), arthur)).getOrThrow()
            assertEquals(alice, s.currentPlayer)
            s = reduce(s, RemovePlayer(nextId(), alice)).getOrThrow()
            assertEquals(SetStatus.FINISHED, s.status)
        }

        @Test
        fun `depart d'un joueur avant le lanceur courant dans la rotation`() {
            var s = newSet(teams = listOf(lineup(A, alice, arthur, PlayerIds.anna), lineup(B, bruno))).play(1).play(1)
            assertEquals(arthur, s.currentPlayer)
            s = reduce(s, RemovePlayer(nextId(), alice)).getOrThrow()
            assertEquals(arthur, s.currentPlayer)
        }

        @Test
        fun `L6 reordonner entre deux tours`() {
            var s =
                newSet(
                    teams = listOf(lineup(A, alice, arthur), lineup(B, bruno), lineup(C, chloe)),
                ).play(1).play(1).play(1)
            s = reduce(s, Reorder(nextId(), listOf(C, A, B), mapOf(A to listOf(arthur, alice)))).getOrThrow()
            assertEquals(C, s.turn)
            s = s.play(1)
            assertEquals(arthur, s.currentPlayer)
        }

        @Test
        fun `L7 reordonner en plein tour est refuse`() {
            val s = newSet(teams = soloTeams(3)).play(1)
            assertEquals(ScoringError.NOT_BETWEEN_ROUNDS, reduce(s, Reorder(nextId(), listOf(C, B, A))).errorOrNull())
        }

        @Test
        fun `L8 L9 evenements de joueurs invalides`() {
            val s = newSet(teams = listOf(lineup(A, alice, arthur), lineup(B, bruno)))

            fun err(event: PlayEvent) = reduce(s, event).errorOrNull()
            assertEquals(ScoringError.INVALID_EVENT, err(Reorder(nextId(), listOf(A))))
            assertEquals(ScoringError.INVALID_EVENT, err(Reorder(nextId(), listOf(A, B), mapOf(A to listOf(alice)))))
            assertEquals(ScoringError.INVALID_EVENT, err(Reorder(nextId(), listOf(A, B), mapOf(C to listOf(chloe)))))
            assertEquals(ScoringError.PLAYER_IN_MULTIPLE_TEAMS, err(AddPlayer(nextId(), bruno, A)))
            assertEquals(ScoringError.UNKNOWN_PLAYER, err(RemovePlayer(nextId(), chloe)))
        }

        @Test
        fun `une equipe eliminee ne peut ni recruter ni perdre de joueur`() {
            val s = newSet(teams = soloTeams(3)).given(C, eliminated = true)
            assertEquals(ScoringError.INVALID_EVENT, reduce(s, AddPlayer(nextId(), PlayerIds.anna, C)).errorOrNull())
            assertEquals(ScoringError.INVALID_EVENT, reduce(s, RemovePlayer(nextId(), chloe)).errorOrNull())
        }

        @Test
        fun `une equipe partie peut revenir`() {
            var s = newSet(teams = soloTeams(3)).play(1)
            s = reduce(s, RemovePlayer(nextId(), bruno)).getOrThrow()
            s = reduce(s, AddPlayer(nextId(), PlayerIds.anna, B)).getOrThrow()
            assertFalse(s.team(B)!!.left)
            assertEquals(listOf(PlayerIds.anna), s.team(B)!!.players)
        }

        @Test
        fun `points de manche IMO pour une equipe partie`() {
            var s = newSet(GameConfig.ImoTournament, soloTeams(3)).given(B, score = 20)
            s = reduce(s, RemovePlayer(nextId(), bruno)).getOrThrow().given(A, score = 45).play(5)
            assertEquals(0, s.setPoints[B])
        }
    }

    @Nested
    inner class LineupCorrections {
        @Test
        fun `les evenements de joueurs et l'abandon se corrigent comme les lancers`() {
            val log = LogBuilder(teams = soloTeams(3))
            log.play(1)
            log.play(1)
            log.play(1)
            val add = log.add(AddPlayer(nextId(), PlayerIds.anna, TeamId("D")))
            val reorder = log.add(Reorder(nextId(), listOf(A, B, C, TeamId("D"))))
            val remove = log.add(RemovePlayer(nextId(), chloe))
            val abandon = log.add(Abandon(nextId()))
            log.add(Correct(nextId(), abandon.id, Abandon(nextId())))
            log.add(Correct(nextId(), remove.id, RemovePlayer(nextId(), bruno)))
            log.add(Correct(nextId(), reorder.id, Reorder(nextId(), listOf(C, B, A, TeamId("D")))))
            log.add(Correct(nextId(), add.id, AddPlayer(nextId(), PlayerIds.zoe, TeamId("D"))))
            val result = log.result()
            assertTrue(result.rejected.isEmpty(), result.rejected.toString())
            assertEquals(
                listOf(add.id, reorder.id, remove.id, abandon.id),
                result.effectiveLog.takeLast(4).map { it.id },
            )
            assertEquals(listOf(PlayerIds.zoe), result.state.team(TeamId("D"))!!.players)
        }

        @Test
        fun `nouveau venu quand les equipes n'ont pas toutes joue`() {
            val s =
                reduce(newSet(teams = soloTeams(3)).play(1).play(1), AddPlayer(nextId(), PlayerIds.anna, TeamId("D")))
                    .getOrThrow()
            assertEquals(0, s.team(TeamId("D"))!!.turns)
        }

        @Test
        fun `controle de saisie des fautes`() {
            val s = newSet(teams = listOf(lineup(A, alice, arthur), lineup(B, bruno)))
            assertNull(checkInput(s, Fault(nextId(), FaultKind.WRONG_PLAYER, by = arthur)))
            assertNull(checkInput(s, Fault(nextId(), FaultKind.ABSENT)))
            assertNull(checkInput(s, Fault(nextId(), FaultKind.ABSENT, by = alice)))
            assertNull(checkInput(s, wrongTeam(B, bruno, 3)))
        }
    }

    @Nested
    inner class Records {
        private fun LogBuilder.aliceWins() {
            listOf(12, 12, 12, 12, 2).forEach {
                play(it)
                if (state.status == SetStatus.IN_PROGRESS) play(1)
            }
        }

        @Test
        fun `R1 une correction pendant la partie reste eligible`() {
            val log = LogBuilder()
            val first = log.play(3)
            log.add(Correct(nextId(), first.id, throwOf(12)))
            log.play(1)
            listOf(12, 12, 12, 2).forEach {
                log.play(it)
                if (log.state.status == SetStatus.IN_PROGRESS) log.play(1)
            }
            val result = log.result()
            assertEquals(SetStatus.FINISHED, result.state.status)
            assertTrue(result.hasCorrections)
            assertTrue(result.eligibleForRecords)
        }

        @Test
        fun `R2 annulation immediate du lancer gagnant toleree`() {
            val log = LogBuilder().apply { aliceWins() }
            log.undoLast()
            log.play(2)
            val result = log.result()
            assertEquals(SetStatus.FINISHED, result.state.status)
            assertFalse(result.modifiedAfterFinish)
            assertTrue(result.eligibleForRecords)
        }

        @Test
        fun `annulation non immediate apres la fin`() {
            val log = LogBuilder().apply { aliceWins() }
            log.add(Delete(nextId(), EventId("inconnu")))
            log.undoLast()
            log.play(2)
            assertTrue(log.result().modifiedAfterFinish)
        }

        @Test
        fun `R4 suppression du lancer gagnant puis nouvelle victoire`() {
            val log = LogBuilder().apply { aliceWins() }
            log.add(
                Delete(
                    nextId(),
                    log
                        .result()
                        .effectiveLog
                        .last()
                        .id,
                ),
            )
            log.play(2)
            val result = log.result()
            assertEquals(SetStatus.FINISHED, result.state.status)
            assertTrue(result.modifiedAfterFinish)
            assertFalse(result.eligibleForRecords)
        }
    }

    @Nested
    inner class NextSetOrder {
        private val abc = listOf(A, B, C)
        private val previous = listOf(SetResult(listOf(B, C, A), mapOf(A to 10, B to 50, C to 30)))

        private fun order(starter: NextStarter, teams: List<TeamId> = abc, results: List<SetResult> = previous) =
            (
                matchStatus(
                    MatchConfig(setsToWin = 3, nextStarter = starter),
                    teams,
                    results,
                ) as MatchStatus.NextSet
            ).order

        @Test
        fun `N1 ordre identique`() = assertEquals(listOf(A, B, C), order(NextStarter.Same))

        @Test
        fun `N2 rotation`() = assertEquals(listOf(B, C, A), order(NextStarter.Rotate))

        @Test
        fun `N3 le dernier commence`() = assertEquals(listOf(A, B, C), order(NextStarter.LoserFirst))

        @Test
        fun `N4 le vainqueur joue en dernier`() = assertEquals(listOf(C, A, B), order(NextStarter.WinnerLast))

        @Test
        fun `N5 a deux equipes le vainqueur joue en dernier`() {
            val results = listOf(SetResult(listOf(A, B), mapOf(A to 50, B to 20)))
            assertEquals(listOf(B, A), order(NextStarter.WinnerLast, listOf(A, B), results))
        }
    }
}

/** Joueurs supplémentaires des cas v2. */
object PlayerIds {
    val anna =
        com.yogistudio.molkkyboard.core.model
            .PlayerId("Anna")
    val zoe =
        com.yogistudio.molkkyboard.core.model
            .PlayerId("Zoé")
}

class LogEntriesTest {
    @Test
    fun `chaque evenement est attribue a son lanceur avec les scores avant et apres`() {
        val teams = listOf(lineup(A, alice), lineup(B, bruno, bea))
        val log =
            listOf(
                throwOf(7),
                Fault(nextId(), FaultKind.WRONG_PLAYER, by = bea),
                wrongTeam(B, bruno, 3),
                Fault(nextId(), FaultKind.ABSENT),
                AddPlayer(nextId(), chloe, C),
            )
        val entries = describe(newSet(GameConfig.Classic, teams), log)
        assertEquals(listOf(A, B, B, A, null), entries.map { it.team })
        assertEquals(listOf(alice, bea, bruno, alice, null), entries.map { it.player })
        assertEquals(listOf(0, 0, 0, 7, null), entries.map { it.scoreBefore })
        assertEquals(listOf(7, 0, 0, 7, null), entries.map { it.scoreAfter })
    }
}
