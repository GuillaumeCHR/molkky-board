package com.yogistudio.molkkyboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yogistudio.molkkyboard.core.data.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Partie en cours à reprendre : scores et joueur dont c'est le tour. */
data class OngoingGame(
    val id: String,
    val scores: List<Pair<String, Int>>,
    val nextPlayer: String?,
)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        games: GameRepository,
    ) : ViewModel() {
        @OptIn(ExperimentalCoroutinesApi::class)
        val ongoing: StateFlow<OngoingGame?> =
            games
                .observeOngoingGameId()
                .flatMapLatest { id ->
                    if (id == null) {
                        flowOf(null)
                    } else {
                        games.observe(id).map { snapshot ->
                            snapshot?.let { game ->
                                val state = game.currentSet.replay.state
                                OngoingGame(
                                    id = game.id,
                                    scores = state.teams.map { (game.team(it.id)?.name ?: it.id.value) to it.score },
                                    nextPlayer = state.currentPlayer?.let(game::playerName),
                                )
                            }
                        }
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
