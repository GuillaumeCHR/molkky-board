package fr.molkky.core.scoring

import fr.molkky.core.model.TeamId
import kotlinx.serialization.Serializable

/** Qui commence la manche suivante. */
@Serializable
enum class NextStarter {
    Alternate,
    LoserStarts,

    /** Alternance, sauf la belle : l'équipe qui a le plus de points sur les manches précédentes commence. */
    DecisiveByPoints,
}

@Serializable
enum class TieBreak { Molkkout, None }

/** Déroulé d'un match. L'ordre des équipes de la 1re manche est fourni à [matchStatus] (la première commence). */
@Serializable
data class MatchConfig(
    val setsToWin: Int = 2,
    val nextStarter: NextStarter = NextStarter.DecisiveByPoints,
    val tieBreak: TieBreak = TieBreak.Molkkout,
) {
    companion object {
        val Classic = MatchConfig()
        val ImoTournament = MatchConfig(nextStarter = NextStarter.Alternate, tieBreak = TieBreak.None)
    }
}

/** Résultat d'une manche terminée, tel que le match en a besoin. */
data class SetResult(
    val ranking: List<TeamId>,
    val setPoints: Map<TeamId, Int>,
) {
    val winner: TeamId get() = ranking.first()
}

fun SetState.toResult(): SetResult? = if (status == SetStatus.FINISHED) SetResult(ranking, setPoints) else null

sealed interface MatchStatus {
    data class Finished(
        val winner: TeamId,
    ) : MatchStatus

    /** Manche [number] (à partir de 1) à jouer, avec son ordre de passage. */
    data class NextSet(
        val number: Int,
        val order: List<TeamId>,
    ) : MatchStatus

    /** Égalité de points avant la belle : un Mölkkout désigne l'équipe qui commence. */
    data class MolkkoutRequired(
        val number: Int,
    ) : MatchStatus
}

/**
 * État d'un match à partir des manches terminées. [teams] est l'ordre de passage de la 1re manche ;
 * l'ordre relatif des équipes est conservé, seule l'équipe qui commence change.
 */
fun matchStatus(
    config: MatchConfig,
    teams: List<TeamId>,
    results: List<SetResult>,
    molkkoutWinner: TeamId? = null,
): MatchStatus {
    val wins = teams.associateWith { team -> results.count { it.winner == team } }
    wins.entries.firstOrNull { it.value >= config.setsToWin }?.let { return MatchStatus.Finished(it.key) }

    val index = results.size
    val alternate = teams[index % teams.size]
    val starter =
        when {
            index == 0 -> {
                teams.first()
            }

            config.nextStarter == NextStarter.Alternate -> {
                alternate
            }

            config.nextStarter == NextStarter.LoserStarts -> {
                results.last().ranking.last()
            }

            teams.size == 2 && wins.values.all { it == config.setsToWin - 1 } -> {
                val totals = teams.map { team -> results.sumOf { it.setPoints[team] ?: 0 } }
                when {
                    totals[0] > totals[1] -> teams[0]
                    totals[1] > totals[0] -> teams[1]
                    config.tieBreak == TieBreak.None -> alternate
                    else -> molkkoutWinner ?: return MatchStatus.MolkkoutRequired(index + 1)
                }
            }

            else -> {
                alternate
            }
        }
    val from = teams.indexOf(starter)
    return MatchStatus.NextSet(index + 1, teams.indices.map { teams[(from + it) % teams.size] })
}

/** La composition et l'ordre des joueurs peuvent changer entre deux manches, jamais pendant. */
fun checkLineupChange(current: SetState?): ScoringError? =
    if (current?.status == SetStatus.IN_PROGRESS) ScoringError.LINEUP_LOCKED else null
