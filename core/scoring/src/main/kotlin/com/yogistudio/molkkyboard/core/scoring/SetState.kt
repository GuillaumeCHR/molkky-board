package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.EliminationSetScore
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.targetFor

enum class SetStatus { IN_PROGRESS, FINISHED, ABANDONED }

/** État immuable d'une équipe dans une manche. */
data class TeamState(
    val id: TeamId,
    val players: List<PlayerId>,
    val nextPlayerIdx: Int = 0,
    val score: Int = 0,
    val misses: Int = 0,
    val eliminated: Boolean = false,
    /** Place obtenue en atteignant la cible (1 = vainqueur), en mode « jouer les places ». */
    val finishedRank: Int? = null,
    /** Lancers réguliers à perdre après une faute « mauvaise équipe ». */
    val pendingForfeit: Int = 0,
    /** Tours joués (lancers, fautes et lancers perdus) : sert à repérer la fin d'un tour. */
    val turns: Int = 0,
    /** L'équipe a quitté la partie (plus aucun joueur). */
    val left: Boolean = false,
) {
    val nextPlayer: PlayerId get() = players[nextPlayerIdx]

    /** Encore en lice : ni éliminée, ni classée, ni partie. */
    val active: Boolean get() = !eliminated && finishedRank == null && !left

    /** Le tour de l'équipe est consommé : son prochain joueur lancera la fois suivante. */
    internal fun consumeTurn(): TeamState = copy(nextPlayerIdx = (nextPlayerIdx + 1) % players.size, turns = turns + 1)
}

/** État immuable d'une manche. Obtenu uniquement par [start] puis [reduce] (ou [replay]). */
data class SetState(
    val config: GameConfig,
    val teams: List<TeamState>,
    /** Équipe qui doit lancer ; `null` si la manche n'est pas en cours. */
    val turn: TeamId?,
    val status: SetStatus = SetStatus.IN_PROGRESS,
    /** Classement final (1er, 2e, …), rempli à la fin de la manche. */
    val ranking: List<TeamId> = emptyList(),
    /** Équipes dans l'ordre de leur élimination (sert au classement). */
    val eliminationOrder: List<TeamId> = emptyList(),
    /** Équipes dans l'ordre de leur départ (classées en dernier). */
    val departureOrder: List<TeamId> = emptyList(),
    val rules: ScoringRules = StandardRules,
) {
    fun team(id: TeamId): TeamState? = teams.firstOrNull { it.id == id }

    val currentTeam: TeamState? get() = turn?.let(::team)

    val currentPlayer: PlayerId? get() = currentTeam?.nextPlayer

    val winner: TeamId? get() = ranking.firstOrNull()

    /** Fin d'un tour : toutes les équipes en lice ont joué le même nombre de fois. */
    val betweenRounds: Boolean get() =
        teams
            .filter { it.active }
            .map { it.turns }
            .distinct()
            .size <= 1

    /**
     * Points de manche (utiles au match et au départage) : scores réels, ou, en mode IMO
     * [EliminationSetScore.WinnerTargetLoserZero], cible pour le vainqueur et 0 pour les éliminés et les partis.
     */
    val setPoints: Map<TeamId, Int>
        get() =
            teams.associate { team ->
                val points =
                    when {
                        config.eliminationSetScore == EliminationSetScore.Keep -> team.score
                        status != SetStatus.FINISHED -> team.score
                        team.id == winner -> config.targetFor(team.id)
                        team.eliminated || team.left -> 0
                        else -> team.score
                    }
                team.id to points
            }
}
