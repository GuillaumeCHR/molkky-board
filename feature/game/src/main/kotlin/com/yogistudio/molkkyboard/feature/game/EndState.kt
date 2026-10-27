package com.yogistudio.molkkyboard.feature.game

import com.yogistudio.molkkyboard.core.data.GameSnapshot
import com.yogistudio.molkkyboard.core.data.toStatsGame
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.scoring.MatchStatus
import com.yogistudio.molkkyboard.core.stats.PERFECT_GAME_THROWS
import com.yogistudio.molkkyboard.core.stats.SetSummary
import com.yogistudio.molkkyboard.core.stats.series
import com.yogistudio.molkkyboard.core.stats.summarize

/** Écran de fin d'une manche : vainqueur, statistiques de la manche, match, série. */
data class EndUi(
    val winner: TeamUi?,
    /** Lancers de l'équipe gagnante dans la manche (fautes comprises). */
    val winnerThrows: Int = 0,
    val perfect: Boolean = false,
    /** Record battu pendant cette partie (annoncé une seule fois, puis rappelé ici). */
    val newRecord: Boolean = false,
    val teamStats: List<TeamStatsUi> = emptyList(),
    val match: MatchUi? = null,
    /** Récapitulatif quand les mêmes équipes enchaînent les manches (au moins deux). */
    val series: SeriesUi? = null,
)

/** Une équipe dans les statistiques de la manche. */
data class TeamStatsUi(
    val team: TeamUi,
    val throws: Int,
    val misses: Int,
    val average: Float,
)

/** Match en plusieurs manches gagnantes. */
data class MatchUi(
    val setsToWin: Int,
    val wins: List<Pair<TeamUi, Int>>,
    /** Vainqueur du match, une fois les manches gagnantes atteintes. */
    val winner: TeamUi?,
    /** Numéro de la prochaine manche à jouer (le Mölkkout éventuel une fois joué). */
    val nextSet: Int?,
    val molkkout: MolkkoutUi?,
)

/** Mölkkout en cours : il désigne l'équipe qui commence la belle. */
data class MolkkoutUi(
    val totals: List<Pair<TeamUi, Int>>,
    val nextTeam: String?,
    val nextPlayer: String?,
    val winner: String?,
    val throws: Int,
)

data class SeriesUi(
    val sets: Int,
    val rows: List<SeriesRowUi>,
)

data class SeriesRowUi(
    val name: String,
    val colorIndex: Int,
    val wins: Int,
    val scores: List<Int>,
    val total: Int,
)

/**
 * Fin de la manche en cours, calculée depuis les journaux. [history] : résumés des parties closes (la partie
 * elle-même en est retirée, elle est résumée ici depuis son état le plus récent).
 */
internal fun GameSnapshot.endUi(teamsUi: List<TeamUi>, history: List<SetSummary>, newRecord: Boolean): EndUi {
    val state = currentSet.replay.state
    val byId = teamsUi.associateBy { it.id }
    val mine = summarize(toStatsGame())
    val summary = mine.firstOrNull { it.setIndex == currentSet.index }
    val winner = state.winner?.let(byId::get)
    val winnerThrows = summary?.winnerTeam?.throws ?: 0
    val teamStats =
        state.ranking.ifEmpty { state.teams.map { it.id } }.mapNotNull { teamId ->
            val team = byId[teamId] ?: return@mapNotNull null
            val players =
                state
                    .team(teamId)
                    ?.players
                    .orEmpty()
                    .mapNotNull { summary?.players?.get(it) }
            val throws = players.sumOf { it.throws }
            TeamStatsUi(
                team = team,
                throws = summary?.teams?.firstOrNull { it.memberKey == team(teamId)?.memberKey }?.throws ?: throws,
                misses = players.sumOf { it.misses },
                average = if (throws == 0) 0f else players.sumOf { it.points }.toFloat() / throws,
            )
        }
    val all = history.filterNot { it.gameId == id } + mine
    val seriesUi =
        series(all, id, currentSet.index)?.let { found ->
            SeriesUi(
                sets = found.sets.size,
                rows =
                    found.rows.map { row ->
                        val team = teams.firstOrNull { it.memberKey == row.memberKey }
                        SeriesRowUi(row.name, team?.colorIndex ?: 0, row.wins, row.scores, row.total)
                    },
            )
        }
    return EndUi(
        winner = winner,
        winnerThrows = winnerThrows,
        perfect = winner != null && winnerThrows in 1..PERFECT_GAME_THROWS && summary?.standard == true,
        newRecord = newRecord,
        teamStats = teamStats,
        match = matchUi(byId),
        series = seriesUi,
    )
}

private fun GameSnapshot.matchUi(byId: Map<TeamId, TeamUi>): MatchUi? {
    if (matchConfig.setsToWin <= 1) return null
    val status = match ?: return null
    val wins = setWins
    val molkkout =
        molkkoutState?.let { state ->
            MolkkoutUi(
                totals =
                    state.teams.mapNotNull { lineup ->
                        byId[lineup.id]?.let { it to (state.totals[lineup.id] ?: 0) }
                    },
                nextTeam = state.nextTeam?.id?.let { byId[it]?.name },
                nextPlayer = state.nextPlayer?.let(::playerName),
                winner = state.winner?.let { byId[it]?.name },
                throws = molkkout.size,
            )
        }
    return MatchUi(
        setsToWin = matchConfig.setsToWin,
        wins = sets.first().lineup.mapNotNull { lineup -> byId[lineup.id]?.let { it to (wins[lineup.id] ?: 0) } },
        winner = (status as? MatchStatus.Finished)?.winner?.let(byId::get),
        nextSet = (status as? MatchStatus.NextSet)?.number,
        molkkout = molkkout,
    )
}
