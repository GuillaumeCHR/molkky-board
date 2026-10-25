package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.TeamId
import kotlinx.serialization.Serializable

/** Ordre de passage de la manche suivante. */
@Serializable
enum class NextStarter {
    /** Ordre identique. */
    Same,

    /** La première équipe passe en dernier (alternance à deux équipes). */
    Rotate,

    /** Le dernier du classement commence, l'ordre relatif est conservé. */
    LoserFirst,

    /** Le vainqueur joue en dernier, l'ordre relatif est conservé. */
    WinnerLast,

    /** Rotation, sauf la belle : l'équipe qui a le plus de points sur les manches précédentes commence. */
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
        val ImoTournament = MatchConfig(nextStarter = NextStarter.Rotate, tieBreak = TieBreak.None)
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
 * État d'un match à partir des manches terminées. [teams] est l'ordre de passage de la 1re manche ; l'ordre
 * de chaque manche suivante est déduit du précédent selon [MatchConfig.nextStarter].
 */
fun matchStatus(
    config: MatchConfig,
    teams: List<TeamId>,
    results: List<SetResult>,
    molkkoutWinner: TeamId? = null,
): MatchStatus {
    val wins = teams.associateWith { team -> results.count { it.winner == team } }
    wins.entries.firstOrNull { it.value >= config.setsToWin }?.let { return MatchStatus.Finished(it.key) }

    var order = teams
    results.forEachIndexed { index, result ->
        val decisive = index == results.lastIndex && teams.size == 2 && wins.values.all { it == config.setsToWin - 1 }
        order =
            if (decisive && config.nextStarter == NextStarter.DecisiveByPoints) {
                val starter = decisiveStarter(config, teams, results, order, molkkoutWinner)
                if (starter < 0) return MatchStatus.MolkkoutRequired(results.size + 1)
                order.startingWith(teams[starter])
            } else {
                nextOrder(config.nextStarter, order, result)
            }
    }
    return MatchStatus.NextSet(results.size + 1, order)
}

/** Ordre de la manche suivante, hors belle aux points. */
private fun nextOrder(starter: NextStarter, order: List<TeamId>, result: SetResult): List<TeamId> =
    when (starter) {
        NextStarter.Same -> order
        NextStarter.Rotate, NextStarter.DecisiveByPoints -> order.startingWith(order[1 % order.size])
        NextStarter.LoserFirst -> order.startingWith(result.ranking.last())
        NextStarter.WinnerLast -> order.startingWith(order[(order.indexOf(result.winner) + 1) % order.size])
    }

/**
 * Belle à deux équipes : index dans [teams] de l'équipe qui commence (la plus haute somme de points ; à égalité,
 * le vainqueur du Mölkkout), ou -1 si le Mölkkout reste à jouer.
 */
private fun decisiveStarter(
    config: MatchConfig,
    teams: List<TeamId>,
    results: List<SetResult>,
    order: List<TeamId>,
    molkkoutWinner: TeamId?,
): Int {
    val totals = teams.map { team -> results.sumOf { it.setPoints[team] ?: 0 } }
    return when {
        totals[0] > totals[1] -> 0
        totals[1] > totals[0] -> 1
        config.tieBreak == TieBreak.None -> teams.indexOf(order[1])
        else -> teams.indexOf(molkkoutWinner)
    }
}

/** Même ordre relatif, en commençant par [first]. */
private fun List<TeamId>.startingWith(first: TeamId): List<TeamId> {
    val from = indexOf(first)
    return indices.map { this[(from + it) % size] }
}
