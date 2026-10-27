package com.yogistudio.molkkyboard.core.stats

/** Une équipe dans le récapitulatif d'une série : victoires, score de chaque manche et total. */
data class SeriesRow(
    val memberKey: String,
    val name: String,
    val wins: Int,
    val scores: List<Int>,
) {
    val total: Int get() = scores.sum()
}

/** Manches enchaînées par les mêmes équipes (revanches, manches d'un match), jusqu'à la manche de référence. */
data class Series(
    val sets: List<SetSummary>,
    val rows: List<SeriesRow>,
)

/**
 * Série qui se termine par la manche ([gameId], [setIndex]) : les manches terminées précédentes, consécutives,
 * jouées par exactement les mêmes équipes. `null` s'il n'y a qu'une manche. Les lignes sont triées par victoires
 * puis par total de points.
 */
fun series(summaries: List<SetSummary>, gameId: String, setIndex: Int): Series? {
    val finished =
        summaries
            .filter { it.winner != null }
            .sortedWith(compareBy({ it.playedAt }, { it.gameId }, { it.setIndex }))
    val end = finished.indexOfFirst { it.gameId == gameId && it.setIndex == setIndex }
    if (end < 0) return null
    val keys = finished[end].teams.map { it.memberKey }.toSet()
    var start = end
    while (start > 0 && finished[start - 1].teams.map { it.memberKey }.toSet() == keys) start--
    val sets = finished.subList(start, end + 1)
    if (sets.size < 2) return null
    val rows =
        finished[end].teams.map { team ->
            SeriesRow(
                memberKey = team.memberKey,
                name = team.name,
                wins = sets.count { it.winner == team.memberKey },
                scores = sets.map { it.team(team.memberKey)?.score ?: 0 },
            )
        }
    return Series(sets, rows.sortedWith(compareByDescending<SeriesRow> { it.wins }.thenByDescending { it.total }))
}
