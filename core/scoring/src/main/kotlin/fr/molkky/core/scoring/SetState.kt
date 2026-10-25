package fr.molkky.core.scoring

import fr.molkky.core.model.EliminationSetScore
import fr.molkky.core.model.GameConfig
import fr.molkky.core.model.PlayerId
import fr.molkky.core.model.TeamId
import fr.molkky.core.model.targetFor

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
) {
    val nextPlayer: PlayerId get() = players[nextPlayerIdx]

    /** Encore en lice : ni éliminée, ni classée. */
    val active: Boolean get() = !eliminated && finishedRank == null

    internal fun advancePlayer(): TeamState = copy(nextPlayerIdx = (nextPlayerIdx + 1) % players.size)
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
) {
    fun team(id: TeamId): TeamState? = teams.firstOrNull { it.id == id }

    val currentTeam: TeamState? get() = turn?.let(::team)

    val currentPlayer: PlayerId? get() = currentTeam?.nextPlayer

    val winner: TeamId? get() = ranking.firstOrNull()

    /**
     * Points de manche (utiles au match et au départage) : scores réels, ou, en mode IMO
     * [EliminationSetScore.WinnerTargetLoserZero], cible pour le vainqueur et 0 pour les éliminés.
     */
    val setPoints: Map<TeamId, Int>
        get() =
            teams.associate { team ->
                val points =
                    when {
                        config.eliminationSetScore == EliminationSetScore.Keep -> team.score
                        status != SetStatus.FINISHED -> team.score
                        team.id == winner -> config.targetFor(team.id)
                        team.eliminated -> 0
                        else -> team.score
                    }
                team.id to points
            }
}
