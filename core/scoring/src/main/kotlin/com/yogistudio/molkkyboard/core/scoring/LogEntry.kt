package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId

/**
 * Un événement du journal effectif, avec l'équipe et le joueur à qui il est attribué (attribution par
 * position) et le score de cette équipe avant et après. Sert au déroulé de la partie.
 */
data class LogEntry(
    val event: PlayEvent,
    val team: TeamId?,
    val player: PlayerId?,
    val scoreBefore: Int?,
    val scoreAfter: Int?,
)

/** Rejoue [effectiveLog] depuis [initial] et décrit chaque événement. */
fun describe(initial: SetState, effectiveLog: List<PlayEvent>): List<LogEntry> {
    var state = initial
    return effectiveLog.map { event ->
        val team =
            when (event) {
                is WrongTeam -> event.team
                is Throw, is Fault -> state.turn
                else -> null
            }
        val player =
            when (event) {
                is WrongTeam -> event.player
                is Fault -> if (event.kind == FaultKind.WRONG_PLAYER) event.by else state.currentPlayer
                is Throw -> state.currentPlayer
                else -> null
            }
        val before = scoreOf(state, team)
        state = reduce(state, event).getOrThrow()
        LogEntry(event, team, player, before, scoreOf(state, team))
    }
}

/** Score de [team] (toujours présente : le journal effectif a déjà été validé). */
private fun scoreOf(state: SetState, team: TeamId?): Int? =
    if (team ==
        null
    ) {
        null
    } else {
        state.teams.first { it.id == team }.score
    }
