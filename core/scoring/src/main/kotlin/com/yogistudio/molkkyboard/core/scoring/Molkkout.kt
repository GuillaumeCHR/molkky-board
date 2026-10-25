package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import kotlinx.serialization.Serializable

/** Quilles du Mölkkout, alignées 6, 4, 12, 10, 8 et remises en place après chaque lancer. */
val MOLKKOUT_PINS: Set<Int> = setOf(4, 6, 8, 10, 12)

@Serializable
data class MolkkoutThrow(
    val team: TeamId,
    val player: PlayerId,
    val pins: List<Int>,
)

/**
 * État d'un Mölkkout entre deux équipes. Les lancers alternent `A BB AA BB …` ; à égalité après la série
 * réglementaire, chaque joueur relance une fois (même alternance) jusqu'au départage.
 */
data class MolkkoutState(
    val teams: List<TeamLineup>,
    val totals: Map<TeamId, Int>,
    /** 0 = série réglementaire, puis tours supplémentaires. */
    val round: Int = 0,
    val throwInRound: Int = 0,
    val throwsByTeam: Map<TeamId, Int> = teams.associate { it.id to 0 },
    val winner: TeamId? = null,
) {
    /** Lancers de chaque équipe dans le tour en cours. */
    val throwsPerTeamInRound: Int
        get() = teams.maxOf { if (round == 0) regularThrows(it.players.size) else it.players.size }

    val nextTeam: TeamLineup?
        get() = if (winner != null) null else teams[if ((throwInRound + 1) / 2 % 2 == 0) 0 else 1]

    val nextPlayer: PlayerId? get() = nextTeam?.let(::playerOf)

    /** Les joueurs d'une équipe lancent à tour de rôle, d'un tour à l'autre. */
    internal fun playerOf(team: TeamLineup): PlayerId = team.players[throwsByTeam.getValue(team.id) % team.players.size]

    companion object {
        /** Seul : 3 lancers ; à deux : 2 chacun ; à trois ou plus : 1 chacun. */
        fun regularThrows(teamSize: Int): Int =
            when (teamSize) {
                1 -> SOLO_THROWS
                2 -> PAIR_THROWS
                else -> teamSize
            }

        private const val SOLO_THROWS = 3
        private const val PAIR_THROWS = 4
    }
}

object Molkkout {
    fun start(teams: List<TeamLineup>): Outcome<MolkkoutState> =
        when {
            teams.size != 2 -> Outcome.Err(ScoringError.INVALID_CONFIG)
            teams.any { it.players.isEmpty() } -> Outcome.Err(ScoringError.EMPTY_TEAM)
            else -> Outcome.Ok(MolkkoutState(teams = teams, totals = teams.associate { it.id to 0 }))
        }

    fun reduce(state: MolkkoutState, event: MolkkoutThrow): Outcome<MolkkoutState> {
        val team = state.nextTeam ?: return Outcome.Err(ScoringError.SET_FINISHED)
        val error =
            when {
                event.team != team.id -> ScoringError.NOT_YOUR_TURN
                event.player != state.playerOf(team) -> ScoringError.NOT_YOUR_TURN
                else -> validatePins(event.pins) { it in MOLKKOUT_PINS }
            }
        if (error != null) return Outcome.Err(error)

        val totals = state.totals + (team.id to state.totals.getValue(team.id) + points(event.pins))
        val throwsByTeam = state.throwsByTeam + (team.id to state.throwsByTeam.getValue(team.id) + 1)
        val throwInRound = state.throwInRound + 1
        val next = state.copy(totals = totals, throwsByTeam = throwsByTeam, throwInRound = throwInRound)
        if (throwInRound < 2 * state.throwsPerTeamInRound) return Outcome.Ok(next)

        val (a, b) = state.teams.map { totals.getValue(it.id) }
        return Outcome.Ok(
            when {
                a > b -> next.copy(winner = state.teams[0].id)
                b > a -> next.copy(winner = state.teams[1].id)
                else -> next.copy(round = state.round + 1, throwInRound = 0)
            },
        )
    }

    fun replay(teams: List<TeamLineup>, throws: List<MolkkoutThrow>): Outcome<MolkkoutState> =
        throws.fold(start(teams)) { acc, event -> acc.flatMap { reduce(it, event) } }
}
