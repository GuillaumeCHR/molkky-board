package com.yogistudio.molkkyboard.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yogistudio.molkkyboard.core.data.GameRepository
import com.yogistudio.molkkyboard.core.data.GameSnapshot
import com.yogistudio.molkkyboard.core.scoring.SetStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Une équipe dans une ligne de l'historique. */
data class HistoryTeam(
    val name: String,
    val colorIndex: Int,
    val score: Int,
    val winner: Boolean,
)

/** Une partie close : date, équipes et scores de la dernière manche, mentions éventuelles. */
data class HistoryItem(
    val gameId: String,
    val playedAt: Long,
    val teams: List<HistoryTeam>,
    val sets: Int,
    val abandoned: Boolean,
    /** Modifiée après la fin : exclue des records (elle reste dans l'historique). */
    val modifiedAfterFinish: Boolean,
    /** Corrigée en cours de partie : simple mention. */
    val corrected: Boolean,
)

data class HistoryUiState(
    val loading: Boolean = true,
    val items: List<HistoryItem> = emptyList(),
)

@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val games: GameRepository,
    ) : ViewModel() {
        val state: StateFlow<HistoryUiState> =
            games
                .observeHistory()
                .map { history -> HistoryUiState(loading = false, items = history.map { it.toItem() }) }
                .flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState())

        /** Suppression définitive (après confirmation à l'écran) : la partie disparaît aussi des statistiques. */
        fun delete(gameId: String) {
            viewModelScope.launch { games.delete(gameId) }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

internal fun GameSnapshot.toItem(): HistoryItem {
    val state = currentSet.replay.state
    val winner = state.winner.takeIf { state.status == SetStatus.FINISHED }
    val order = state.ranking.ifEmpty { state.teams.sortedByDescending { it.score }.map { it.id } }
    return HistoryItem(
        gameId = id,
        playedAt = createdAt,
        teams =
            order.mapNotNull { id ->
                val team = state.team(id) ?: return@mapNotNull null
                val info = team(id)
                HistoryTeam(info?.name ?: id.value, info?.colorIndex ?: 0, team.score, id == winner)
            },
        sets = sets.size,
        abandoned = abandoned,
        modifiedAfterFinish = sets.any { it.replay.modifiedAfterFinish },
        corrected = sets.any { it.replay.hasCorrections },
    )
}
