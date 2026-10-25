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
 * Tests de propriétés maison sur des journaux aléatoires (graine fixe, donc reproductibles) :
 * invariants de l'état et `U9` : `fold(L + e) puis Undo == fold(L)`.
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

    private fun Random.pins(): List<Int> =
        when (nextInt(10)) {
            0, 1 -> emptyList()
            in 2..6 -> listOf(nextInt(1, 13))
            else -> ALL_PINS.shuffled(this).take(nextInt(2, 6))
        }

    /** Un événement valide la plupart du temps, parfois invalide pour exercer les rejets. */
    private fun Random.event(state: SetState): PlayEvent {
        val team = state.currentTeam!!
        val other = state.teams.filter { it.active && it.id != team.id }.randomOrNull(this)
        return when (nextInt(100)) {
            in 0..79 -> {
                Throw(nextId(), team.id, team.nextPlayer, pins())
            }

            in 80..87 -> {
                Fault(nextId(), team.id, team.nextPlayer, FaultKind.entries.random(this))
            }

            in 88..92 -> {
                if (other !=
                    null
                ) {
                    WrongTeam(nextId(), other.id, other.nextPlayer, pins())
                } else {
                    Abandon(nextId())
                }
            }

            in 93..97 -> {
                Throw(nextId(), team.id, team.nextPlayer, listOf(nextInt(0, 14)))
            }

            else -> {
                Abandon(nextId())
            }
        }
    }

    private fun SetState.assertInvariants() {
        teams.forEach { team ->
            assertTrue(team.score in 0..config.targetFor(team.id), "score hors bornes : $team")
            assertTrue(team.misses >= 0 && team.pendingForfeit >= 0)
        }
        when (status) {
            SetStatus.IN_PROGRESS -> {
                assertTrue(currentTeam!!.active, "l'équipe au tour doit être en lice")
                assertTrue(teams.count { it.active } >= 2)
                assertTrue(ranking.isEmpty())
            }

            SetStatus.FINISHED -> {
                assertEquals(teams.map { it.id }.toSet(), ranking.toSet())
            }

            SetStatus.ABANDONED -> {
                assertEquals(null, turn)
            }
        }
    }

    @ParameterizedTest(name = "graine {0}")
    @MethodSource("seeds")
    fun `journaux aleatoires invariants et annulation`(seed: Int) {
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
    }

    companion object {
        @JvmStatic
        fun seeds() = (1..300).toList()
    }
}
