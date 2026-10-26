package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.EndCondition
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.MissPenalty
import com.yogistudio.molkkyboard.core.model.Overshoot
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.model.targetFor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random

/**
 * Tests de propriétés maison sur des journaux aléatoires (graine fixe, donc reproductibles) : invariants de
 * l'état, `U9` (`fold(L + e) puis Undo == fold(L)`), et rejeu déterministe après suppressions et changements
 * de joueurs.
 */
class PropertyTest {
    private val configs =
        listOf(
            GameConfig.Classic,
            GameConfig.ImoTournament,
            GameConfig(overshoot = Overshoot.Bounce, onMissLimit = MissPenalty.Nothing),
            GameConfig(overshoot = Overshoot.Stay, onMissLimit = MissPenalty.ResetScore(0), catchUpReset = true),
            GameConfig(targetScore = 30, overshoot = Overshoot.ResetTo(15), endCondition = EndCondition.PlayForPlaces),
            GameConfig(missLimit = 2, faultsCountAsMiss = false, endCondition = EndCondition.PlayForPlaces),
        )

    private fun Random.teams(): List<TeamLineup> =
        (0 until nextInt(2, 5)).map { t ->
            TeamLineup(TeamId("T$t"), (0 until nextInt(1, 4)).map { PlayerId("T$t-P$it") })
        }

    private fun Random.randomPins(): List<Int> =
        when (nextInt(10)) {
            0, 1 -> emptyList()
            in 2..6 -> listOf(nextInt(1, 13))
            else -> ALL_PINS.shuffled(this).take(nextInt(2, 6))
        }

    /** Quilles ou saisie rapide ; parfois une saisie rapide invalide (1 quille en « nombre », 13…). */
    private fun Random.result(): ThrowResult =
        if (nextInt(3) == 0) {
            ThrowResult.Points(nextInt(0, 14), PointsKind.entries.random(this))
        } else {
            ThrowResult.Pins(randomPins())
        }

    /** Un événement valide la plupart du temps, parfois invalide pour exercer les rejets. */
    private fun Random.event(state: SetState): PlayEvent {
        val team = state.currentTeam!!
        val other = state.teams.filter { it.active && it.id != team.id }.randomOrNull(this)
        val everyone = state.teams.flatMap { it.players }
        return when (nextInt(100)) {
            in 0..64 -> {
                Throw(nextId(), result())
            }

            in 65..72 -> {
                val kind = FaultKind.entries.random(this)
                Fault(nextId(), kind, by = if (kind == FaultKind.WRONG_PLAYER) team.players.random(this) else null)
            }

            in 73..77 -> {
                if (other !=
                    null
                ) {
                    WrongTeam(nextId(), other.id, other.nextPlayer, result())
                } else {
                    Abandon(nextId())
                }
            }

            in 78..81 -> {
                Throw(nextId(), ThrowResult.Pins(listOf(nextInt(0, 14))))
            }

            in 82..85 -> {
                val target = if (nextBoolean()) state.teams.random(this).id else TeamId("N${nextInt(50)}")
                AddPlayer(nextId(), PlayerId("late-${nextInt(1_000)}"), target)
            }

            in 86..88 -> {
                RemovePlayer(nextId(), everyone.random(this))
            }

            in 89..96 -> {
                val shuffledTeam = state.teams.random(this)
                Reorder(
                    nextId(),
                    state.teams.map { it.id }.shuffled(this),
                    mapOf(shuffledTeam.id to shuffledTeam.players.shuffled(this)),
                )
            }

            else -> {
                Abandon(nextId())
            }
        }
    }

    private fun SetState.assertInvariants() {
        teams.forEach { team ->
            assertTrue(team.score in 0..config.targetFor(team.id), "score hors bornes : $team")
            assertTrue(team.misses >= 0 && team.pendingForfeit >= 0 && team.turns >= 0)
            assertTrue(team.left == team.players.isEmpty(), "équipe partie ⇔ sans joueur : $team")
        }
        when (status) {
            SetStatus.IN_PROGRESS -> {
                assertTrue(currentTeam!!.active, "l'équipe au tour doit être en lice")
                // § 16 : 1 + le moins de tours joués ; l'équipe au tour n'a pas encore lancé dans ce tour.
                assertEquals(1 + teams.filter { it.active }.minOf { it.turns }, round)
                assertEquals(round - 1, currentTeam!!.turns, "l'équipe au tour doit lancer dans le tour $round")
                assertTrue(teams.filter { it.active }.all { it.turns in round - 1..round })
                assertTrue(teams.count { it.active } >= 2)
                assertTrue(ranking.isEmpty())
            }

            SetStatus.FINISHED -> {
                assertEquals(teams.size, ranking.size, "chaque équipe classée une fois : $ranking")
                assertEquals(teams.map { it.id }.toSet(), ranking.toSet())
            }

            SetStatus.ABANDONED -> {
                assertEquals(null, turn)
            }
        }
    }

    @ParameterizedTest(name = "graine {0}")
    @MethodSource("seeds")
    fun `journaux aleatoires invariants annulation et suppression`(seed: Int) {
        val random = Random(seed)
        val config = configs[seed % configs.size]
        val teams = random.teams()
        val initial = start(config, teams).getOrThrow()
        val log = mutableListOf<SetEvent>()
        var state = initial
        repeat(80) {
            if (state.status != SetStatus.IN_PROGRESS) return@repeat
            val event = random.event(state)
            val before = replay(config, teams, log).getOrThrow()
            val outcome = reduce(state, event)
            log += event
            val after = replay(config, teams, log).getOrThrow()
            if (outcome is Outcome.Ok) {
                state = outcome.value
                assertEquals(state, after.state)
                // U9 : ajouter puis annuler ramène exactement à l'état précédent.
                val undone = replay(config, teams, log + Undo(nextId(), event.id)).getOrThrow()
                assertEquals(before.state, undone.state)
            } else {
                assertEquals(before.state, after.state)
            }
            state.assertInvariants()
            assertEquals(state, fold(initial, after.effectiveLog).getOrThrow())
        }
        // Suppressions au hasard : le rejeu reste déterministe et les invariants tiennent.
        repeat(5) {
            val effective = replay(config, teams, log).getOrThrow().effectiveLog
            if (effective.isEmpty()) return@repeat
            log += Delete(nextId(), effective.random(random).id)
            val first = replay(config, teams, log).getOrThrow()
            assertEquals(first, replay(config, teams, log).getOrThrow())
            first.state.assertInvariants()
            assertEquals(first.state, fold(initial, first.effectiveLog).getOrThrow())
        }
    }

    companion object {
        @JvmStatic
        fun seeds() = (1..300).toList()
    }
}
