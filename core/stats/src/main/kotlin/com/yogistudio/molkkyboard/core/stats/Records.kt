package com.yogistudio.molkkyboard.core.stats

import com.yogistudio.molkkyboard.core.model.PlayerId

/** Records secondaires du Hall of Fame (`STATS_SPEC.md` § 9). */
enum class SecondaryRecordKind {
    /** Plus grand nombre de « 12 » réussis par un joueur dans une même partie. */
    MOST_TWELVES,

    /** Plus longue série de lancers sans raté d'un joueur dans une manche. */
    LONGEST_STREAK,

    /** Joueur qui a fait le plus de retours à 25 (toutes parties confondues). */
    MOST_RESETS,

    /** Plus belle remontée : le plus grand retard rattrapé par une équipe (ou un joueur) qui gagne la manche. */
    BIGGEST_COMEBACK,
}

/** Détenteur d'un record secondaire, avec la partie où il a été réalisé (le lien vers son déroulé). */
data class SecondaryRecord(
    val kind: SecondaryRecordKind,
    val holder: String,
    val value: Int,
    val gameId: String,
    val playedAt: Long,
)

/**
 * Un détenteur par record, sur les manches qui comptent pour les records « purs » (terminées normalement, règles
 * officielles). À égalité, le premier à l'avoir réalisé garde le record (date, puis identifiant de partie).
 */
fun secondaryRecords(summaries: List<SetSummary>): List<SecondaryRecord> {
    val pure = summaries.filter { it.eligible && it.standard }
    return listOfNotNull(
        mostTwelves(pure),
        longestStreak(pure),
        mostResets(pure),
        biggestComeback(pure),
    )
}

private val FIRST_DONE = compareBy<SecondaryRecord>({ -it.value }, { it.playedAt }, { it.gameId })

private fun SetSummary.nameOf(player: PlayerId): String =
    playerNames[player] ?: teams.firstOrNull { player in it.players && it.players.size == 1 }?.name ?: player.value

private fun mostTwelves(sets: List<SetSummary>): SecondaryRecord? =
    sets
        .groupBy { it.gameId }
        .flatMap { (gameId, gameSets) ->
            val first = gameSets.first()
            gameSets
                .flatMap { it.players.entries }
                .groupBy({ it.key }, { it.value.twelves })
                .map { (player, twelves) ->
                    SecondaryRecord(
                        SecondaryRecordKind.MOST_TWELVES,
                        first.nameOf(player),
                        twelves.sum(),
                        gameId,
                        first.playedAt,
                    )
                }
        }.filter { it.value > 0 }
        .minWithOrNull(FIRST_DONE)

private fun longestStreak(sets: List<SetSummary>): SecondaryRecord? =
    sets
        .flatMap { set ->
            set.players.map { (player, line) ->
                SecondaryRecord(
                    SecondaryRecordKind.LONGEST_STREAK,
                    set.nameOf(player),
                    line.bestStreak,
                    set.gameId,
                    set.playedAt,
                )
            }
        }.filter { it.value > 0 }
        .minWithOrNull(FIRST_DONE)

/** Un total : le record pointe vers la dernière partie où il a progressé. */
private fun mostResets(sets: List<SetSummary>): SecondaryRecord? =
    sets
        .flatMap { set ->
            set.players.filterValues { it.resets > 0 }.map { (player, line) ->
                Triple(player, line.resets, set)
            }
        }.groupBy { it.first }
        .map { (player, entries) ->
            val last = entries.maxWith(compareBy({ it.third.playedAt }, { it.third.gameId }))
            SecondaryRecord(
                SecondaryRecordKind.MOST_RESETS,
                last.third.nameOf(player),
                entries.sumOf { it.second },
                last.third.gameId,
                last.third.playedAt,
            )
        }.minWithOrNull(compareBy({ -it.value }, { it.holder }))

private fun biggestComeback(sets: List<SetSummary>): SecondaryRecord? =
    sets
        .mapNotNull { set ->
            val winner = set.winnerTeam ?: return@mapNotNull null
            SecondaryRecord(
                SecondaryRecordKind.BIGGEST_COMEBACK,
                winner.name,
                winner.maxDeficit,
                set.gameId,
                set.playedAt,
            )
        }.filter { it.value > 0 }
        .minWithOrNull(FIRST_DONE)

/** Badges de joueur (`STATS_SPEC.md` § 9). */
enum class Badge {
    /** Première victoire. */
    FIRST_WIN,

    /** Victoire en 5 lancers (4 × 12 + 2), aux règles officielles. */
    PERFECT_GAME,

    /** « Phénix » : victoire dans une manche où l'équipe est retombée à 25. */
    PHOENIX,

    /** « Sniper » : trois « 12 » ou plus dans une même partie. */
    SNIPER,
}

/** Un badge obtenu, avec la première partie qui l'a valu. */
data class EarnedBadge(
    val badge: Badge,
    val gameId: String,
    val playedAt: Long,
)

/** Badges de [player], dans l'ordre de l'énumération ; seules les manches terminées normalement comptent. */
fun badges(summaries: List<SetSummary>, player: PlayerId): List<EarnedBadge> {
    val mine =
        summaries
            .filter { it.eligible && it.teams.any { team -> player in team.players } }
            .sortedWith(compareBy({ it.playedAt }, { it.gameId }, { it.setIndex }))
    val wins = mine.filter { it.winnerTeam?.players?.contains(player) == true }
    val sniper =
        mine
            .groupBy { it.gameId }
            .values
            .firstOrNull { sets -> sets.sumOf { it.players[player]?.twelves ?: 0 } >= SNIPER_TWELVES }
            ?.first()
    val earned =
        mapOf(
            Badge.FIRST_WIN to wins.firstOrNull(),
            Badge.PERFECT_GAME to wins.firstOrNull { it.standard && it.winnerTeam?.throws == PERFECT_GAME_THROWS },
            Badge.PHOENIX to wins.firstOrNull { (it.winnerTeam?.resets ?: 0) > 0 },
            Badge.SNIPER to sniper,
        )
    return Badge.entries.mapNotNull { badge -> earned[badge]?.let { EarnedBadge(badge, it.gameId, it.playedAt) } }
}

/** Trois « 12 » dans une partie : badge « Sniper ». */
const val SNIPER_TWELVES = 3
