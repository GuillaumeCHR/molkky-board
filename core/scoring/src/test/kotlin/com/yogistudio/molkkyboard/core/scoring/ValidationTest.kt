package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.MissPenalty
import com.yogistudio.molkkyboard.core.model.Overshoot
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamOverride
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/** `SCORING_SPEC.md` § 12. */
class ValidationTest {
    @ParameterizedTest
    @ValueSource(ints = [0, 13, -1])
    fun `X1 quille inexistante`(pin: Int) =
        assertEquals(ScoringError.INVALID_PIN, newSet().tryThrow(A, pin).errorOrNull())

    @Test
    fun `X2 quille en double`() = assertEquals(ScoringError.DUPLICATE_PIN, newSet().tryThrow(A, 4, 4).errorOrNull())

    @Test
    fun `X3 moins de deux equipes`() =
        assertEquals(ScoringError.NOT_ENOUGH_TEAMS, start(GameConfig.Classic, soloTeams(1)).errorOrNull())

    @Test
    fun `X4 equipe sans joueur`() =
        assertEquals(
            ScoringError.EMPTY_TEAM,
            start(GameConfig.Classic, listOf(lineup(A, alice), lineup(B))).errorOrNull(),
        )

    @Test
    fun `X5 meme joueur dans deux equipes`() =
        assertEquals(
            ScoringError.PLAYER_IN_MULTIPLE_TEAMS,
            start(GameConfig.Classic, listOf(lineup(A, alice), lineup(B, bruno, alice))).errorOrNull(),
        )

    @Test
    fun `meme equipe deux fois`() =
        assertEquals(
            ScoringError.DUPLICATE_TEAM,
            start(GameConfig.Classic, listOf(lineup(A, alice), lineup(A, bruno))).errorOrNull(),
        )

    @Test
    fun `X6 configurations invalides`() {
        val invalid =
            listOf(
                GameConfig(targetScore = 0),
                GameConfig(overshoot = Overshoot.ResetTo(50)),
                GameConfig(overshoot = Overshoot.ResetTo(-1)),
                GameConfig(missLimit = 0),
                GameConfig(onMissLimit = MissPenalty.ResetScore(50)),
                GameConfig(teamOverrides = mapOf(A to TeamOverride(startScore = 50))),
                GameConfig(teamOverrides = mapOf(A to TeamOverride(targetScore = 20))),
                GameConfig(teamOverrides = mapOf(TeamId("Z") to TeamOverride(startScore = 5))),
            )
        invalid.forEach { config ->
            assertEquals(ScoringError.INVALID_CONFIG, start(config, soloTeams()).errorOrNull(), config.toString())
        }
    }

    @Test
    fun `X7 aucun evenement de jeu sur une manche terminee`() {
        val finished = newSet().given(A, score = 45).play(5)
        listOf(
            Throw(nextId(), B, bruno, listOf(1)),
            Fault(nextId(), B, bruno, FaultKind.TRESPASS),
            WrongTeam(nextId(), B, bruno, listOf(1)),
            Abandon(nextId()),
        ).forEach { assertEquals(ScoringError.SET_FINISHED, reduce(finished, it).errorOrNull()) }
    }

    @Test
    fun `outils de resultat`() {
        val ok: Outcome<Int> = Outcome.Ok(1)
        val err: Outcome<Int> = Outcome.Err(ScoringError.INVALID_PIN)
        assertEquals(1, ok.getOrNull())
        assertEquals(null, err.getOrNull())
        assertEquals(null, ok.errorOrNull())
        org.junit.jupiter.api
            .assertThrows<IllegalStateException> { err.getOrThrow() }
    }
}
