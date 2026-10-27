package com.yogistudio.molkkyboard.core.data

import com.yogistudio.molkkyboard.core.stats.SetSummary
import com.yogistudio.molkkyboard.core.stats.StatsGame
import com.yogistudio.molkkyboard.core.stats.StatsSet
import com.yogistudio.molkkyboard.core.stats.StatsTeam
import com.yogistudio.molkkyboard.core.stats.summarize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Résumés de toutes les manches des parties closes, recalculés depuis les journaux à chaque changement. */
interface StatsRepository {
    val summaries: Flow<List<SetSummary>>
}

@Singleton
class JournalStatsRepository
    @Inject
    constructor(
        games: GameRepository,
    ) : StatsRepository {
        override val summaries: Flow<List<SetSummary>> =
            games
                .observeHistory()
                .map { history -> history.flatMap { summarize(it.toStatsGame()) } }
                .flowOn(Dispatchers.Default)
    }

/** Une partie vue par `core:stats` : identités durables des équipes et résultat du rejeu de chaque manche. */
fun GameSnapshot.toStatsGame(): StatsGame =
    StatsGame(
        id = id,
        playedAt = createdAt,
        config = config,
        teams = teams.map { StatsTeam(it.id, it.memberKey, it.name) },
        sets = sets.map { StatsSet(it.index, it.lineup, it.replay) },
        abandoned = abandoned,
        playerNames = players.associate { it.id to it.name },
    )
