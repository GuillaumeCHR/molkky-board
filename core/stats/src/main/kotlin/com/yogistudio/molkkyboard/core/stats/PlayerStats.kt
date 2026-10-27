package com.yogistudio.molkkyboard.core.stats

import com.yogistudio.molkkyboard.core.model.PlayerId

/** Statistiques simples d'un joueur, sur toutes ses manches (y compris en équipe et en mode Ludique). */
data class PlayerStats(
    val player: PlayerId,
    val sets: Int,
    val wins: Int,
    val throws: Int,
    val misses: Int,
    val points: Int,
    /** Moyenne de points par lancer de chaque manche jouée, dans l'ordre chronologique (graphique). */
    val averages: List<SetAverage>,
    val bestStreak: Int,
    /** Victoire la plus rapide aux règles officielles, en lancers de l'équipe. */
    val fastestWin: Int?,
    val twelves: Int = 0,
    val resets: Int = 0,
) {
    val average: Float get() = if (throws == 0) 0f else points.toFloat() / throws
    val missRate: Float get() = if (throws == 0) 0f else misses.toFloat() / throws
}

data class SetAverage(
    val gameId: String,
    val setIndex: Int,
    val playedAt: Long,
    val average: Float,
)

fun playerStats(summaries: List<SetSummary>, player: PlayerId): PlayerStats {
    val mine =
        summaries
            .filter { summary -> summary.teams.any { player in it.players } }
            .sortedWith(compareBy({ it.playedAt }, { it.gameId }, { it.setIndex }))
    val lines = mine.mapNotNull { summary -> summary.players[player]?.let { summary to it } }
    val wins = mine.filter { summary -> summary.eligible && summary.winnerTeam?.players?.contains(player) == true }
    return PlayerStats(
        player = player,
        sets = mine.size,
        wins = wins.size,
        throws = lines.sumOf { it.second.throws },
        misses = lines.sumOf { it.second.misses },
        points = lines.sumOf { it.second.points },
        averages =
            lines
                .filter { it.second.throws > 0 }
                .map { (summary, line) ->
                    SetAverage(summary.gameId, summary.setIndex, summary.playedAt, line.points.toFloat() / line.throws)
                },
        bestStreak = lines.maxOfOrNull { it.second.bestStreak } ?: 0,
        fastestWin = wins.filter { it.standard }.mapNotNull { it.winnerTeam?.throws }.minOrNull(),
        twelves = lines.sumOf { it.second.twelves },
        resets = lines.sumOf { it.second.resets },
    )
}
