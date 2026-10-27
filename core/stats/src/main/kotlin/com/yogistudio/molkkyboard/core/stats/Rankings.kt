package com.yogistudio.molkkyboard.core.stats

/** Onglets du Hall of Fame : parties en solo, ou parties en équipes. */
enum class HallTab { PLAYERS, TEAMS }

/**
 * Filtres d'un classement. [since] : début de la période en millisecondes (calculé par l'appelant dans le fuseau
 * local), `null` pour « tout le temps ». [teamSize] : onglet Équipes seulement.
 */
data class RankingFilter(
    val tab: HallTab,
    val since: Long? = null,
    val teamSize: Int? = null,
)

/** Une ligne du classement des victoires. */
data class WinsEntry(
    val memberKey: String,
    val name: String,
    val size: Int,
    val wins: Int,
    val played: Int,
    /** Date de la dernière victoire comptée : à égalité, le premier à avoir atteint ce total l'emporte. */
    val reachedAt: Long,
    /** Partie de cette victoire, pour ouvrir son déroulé. */
    val lastWinGameId: String?,
) {
    val rate: Float get() = if (played == 0) 0f else wins.toFloat() / played
}

/** La victoire la plus rapide d'une équipe (ou d'un joueur), en lancers. */
data class FastestEntry(
    val memberKey: String,
    val name: String,
    val size: Int,
    val throws: Int,
    val gameId: String,
    val setIndex: Int,
    val playedAt: Long,
) {
    /** 4 × 12 + 2 : impossible de faire mieux. */
    val perfect: Boolean get() = throws <= PERFECT_GAME_THROWS
}

/** Minimum théorique d'une victoire à 50 points : quatre 12 puis un 2. */
const val PERFECT_GAME_THROWS = 5

private fun List<SetSummary>.matching(filter: RankingFilter): List<SetSummary> =
    filter { summary ->
        summary.eligible &&
            (filter.since == null || summary.playedAt >= filter.since) &&
            when (filter.tab) {
                HallTab.PLAYERS -> summary.solo
                HallTab.TEAMS -> !summary.solo
            }
    }.sortedWith(chronological)

private val chronological = compareBy<SetSummary>({ it.playedAt }, { it.gameId }, { it.setIndex })

private fun RankingFilter.accepts(team: SummaryTeam) = teamSize == null || team.players.size == teamSize

/**
 * Classement des victoires : toutes les manches éligibles comptent, quelles que soient les règles. Tri par
 * victoires, puis par date d'obtention du total (le premier garde le rang), puis par identité.
 */
fun winsRanking(summaries: List<SetSummary>, filter: RankingFilter): List<WinsEntry> {
    val entries = linkedMapOf<String, WinsEntry>()
    summaries.matching(filter).forEach { summary ->
        summary.teams.filter(filter::accepts).forEach { team ->
            val won = summary.winner == team.memberKey
            val previous = entries[team.memberKey]
            entries[team.memberKey] =
                WinsEntry(
                    memberKey = team.memberKey,
                    // Le nom le plus récent : une équipe renommée garde son palmarès.
                    name = team.name,
                    size = team.players.size,
                    wins = (previous?.wins ?: 0) + if (won) 1 else 0,
                    played = (previous?.played ?: 0) + 1,
                    reachedAt = if (won) summary.playedAt else previous?.reachedAt ?: summary.playedAt,
                    lastWinGameId = if (won) summary.gameId else previous?.lastWinGameId,
                )
        }
    }
    return entries.values
        .filter { it.wins > 0 }
        .sortedWith(compareByDescending<WinsEntry> { it.wins }.thenBy { it.reachedAt }.thenBy { it.memberKey })
}

/**
 * Victoire la plus rapide, en lancers de l'équipe gagnante : seulement les manches éligibles aux règles
 * officielles. Une ligne par équipe (sa meilleure), triée par lancers, puis la plus ancienne, puis la partie.
 */
fun fastestRanking(summaries: List<SetSummary>, filter: RankingFilter): List<FastestEntry> =
    summaries
        .matching(filter)
        .filter { it.standard }
        .mapNotNull { summary ->
            val winner = summary.winnerTeam?.takeIf(filter::accepts) ?: return@mapNotNull null
            FastestEntry(
                memberKey = winner.memberKey,
                name = winner.name,
                size = winner.players.size,
                throws = winner.throws,
                gameId = summary.gameId,
                setIndex = summary.setIndex,
                playedAt = summary.playedAt,
            )
        }.sortedWith(fastestOrder)
        .distinctBy { it.memberKey }

private val fastestOrder =
    compareBy<FastestEntry>({ it.throws }, { it.playedAt }, { it.gameId }, { it.setIndex })

/**
 * « 🏆 Nouveau record ! » : la manche [added] devient la victoire la plus rapide de son onglet, alors qu'un
 * record existait déjà (la toute première victoire n'est pas annoncée comme un record).
 */
fun isNewRecord(previous: List<SetSummary>, added: SetSummary): Boolean {
    if (!added.eligible || !added.standard || added.winner == null) return false
    val tab = if (added.solo) HallTab.PLAYERS else HallTab.TEAMS
    val others = previous.filterNot { it.gameId == added.gameId && it.setIndex == added.setIndex }
    val before = fastestRanking(others, RankingFilter(tab)).firstOrNull() ?: return false
    val throws = added.winnerTeam?.throws ?: return false
    return throws < before.throws
}
