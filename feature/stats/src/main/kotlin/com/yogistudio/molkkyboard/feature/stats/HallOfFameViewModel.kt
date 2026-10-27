package com.yogistudio.molkkyboard.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yogistudio.molkkyboard.core.data.StatsRepository
import com.yogistudio.molkkyboard.core.stats.FastestEntry
import com.yogistudio.molkkyboard.core.stats.HallTab
import com.yogistudio.molkkyboard.core.stats.RankingFilter
import com.yogistudio.molkkyboard.core.stats.SetSummary
import com.yogistudio.molkkyboard.core.stats.WinsEntry
import com.yogistudio.molkkyboard.core.stats.fastestRanking
import com.yogistudio.molkkyboard.core.stats.winsRanking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

enum class Period { ALL, YEAR, MONTH }

data class HallFilters(
    val tab: HallTab = HallTab.PLAYERS,
    val period: Period = Period.ALL,
    /** Onglet Équipes : taille d'équipe, `null` pour toutes. */
    val teamSize: Int? = null,
)

data class HallUiState(
    val loading: Boolean = true,
    val filters: HallFilters = HallFilters(),
    val wins: List<WinsEntry> = emptyList(),
    val fastest: List<FastestEntry> = emptyList(),
    /** Tailles d'équipe présentes dans l'historique (filtre de l'onglet Équipes). */
    val teamSizes: List<Int> = emptyList(),
)

/** Hall of Fame local : classements recalculés depuis les journaux à chaque changement de filtre ou de partie. */
@HiltViewModel
class HallOfFameViewModel
    @Inject
    constructor(
        stats: StatsRepository,
    ) : ViewModel() {
        private val filters = MutableStateFlow(HallFilters())

        val state: StateFlow<HallUiState> =
            combine(
                stats.summaries,
                filters,
            ) { summaries, current -> hallState(summaries, current, System.currentTimeMillis()) }
                .flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HallUiState())

        fun selectTab(tab: HallTab) = filters.update { it.copy(tab = tab, teamSize = null) }

        fun selectPeriod(period: Period) = filters.update { it.copy(period = period) }

        fun selectTeamSize(size: Int?) = filters.update { it.copy(teamSize = size) }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

internal fun hallState(
    summaries: List<SetSummary>,
    filters: HallFilters,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): HallUiState {
    val filter = RankingFilter(filters.tab, since = filters.period.since(now, zone), teamSize = filters.teamSize)
    return HallUiState(
        loading = false,
        filters = filters,
        wins = winsRanking(summaries, filter),
        fastest = fastestRanking(summaries, filter),
        teamSizes =
            summaries
                .filter { !it.solo }
                .flatMap { s -> s.teams.map { it.players.size } }
                .distinct()
                .sorted(),
    )
}

/** Début de la période dans le fuseau local (1er janvier ou 1er du mois, à minuit). */
internal fun Period.since(now: Long, zone: ZoneId): Long? {
    val today = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone).toLocalDate()
    val start =
        when (this) {
            Period.ALL -> return null
            Period.YEAR -> today.withDayOfYear(1)
            Period.MONTH -> today.withDayOfMonth(1)
        }
    return start.atStartOfDay(zone).toInstant().toEpochMilli()
}
