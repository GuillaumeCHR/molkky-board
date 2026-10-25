package com.yogistudio.molkkyboard.feature.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yogistudio.molkkyboard.core.data.GameRepository
import com.yogistudio.molkkyboard.core.data.GameSnapshot
import com.yogistudio.molkkyboard.core.data.IdGenerator
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.designsystem.component.QuickPadMode
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.UserSettings
import com.yogistudio.molkkyboard.core.model.targetFor
import com.yogistudio.molkkyboard.core.scoring.Abandon
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.Outcome
import com.yogistudio.molkkyboard.core.scoring.PointsKind
import com.yogistudio.molkkyboard.core.scoring.SetState
import com.yogistudio.molkkyboard.core.scoring.SetStatus
import com.yogistudio.molkkyboard.core.scoring.StandardRules
import com.yogistudio.molkkyboard.core.scoring.TeamState
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import com.yogistudio.molkkyboard.core.scoring.Undo
import com.yogistudio.molkkyboard.core.scoring.points
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val games: GameRepository,
        private val settings: SettingsRepository,
        private val ids: IdGenerator,
    ) : ViewModel() {
        private val gameId: String = checkNotNull(savedStateHandle[GAME_ID])
        private val selection = MutableStateFlow<Selection>(Selection.None)
        private val quickMode = MutableStateFlow(QuickPadMode.SinglePin)
        private val effects = Channel<GameEffect>(Channel.BUFFERED)
        val effectFlow: Flow<GameEffect> = effects.receiveAsFlow()

        val state: StateFlow<GameUiState> =
            combine(
                games.observe(gameId).filterNotNull(),
                settings.settings,
                selection,
                quickMode,
            ) { snapshot, prefs, sel, mode ->
                snapshot.toUiState(prefs, sel, mode)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GameUiState())

        fun togglePin(pin: Int, knocked: Boolean) =
            selection.update { current ->
                val pins = (current as? Selection.Pins)?.pins.orEmpty()
                val next = if (knocked) pins + pin else pins - pin
                if (next.isEmpty()) Selection.None else Selection.Pins(next)
            }

        fun selectQuick(value: Int?) =
            selection.update {
                if (value ==
                    null
                ) {
                    Selection.None
                } else {
                    Selection.Quick(value)
                }
            }

        fun setQuickMode(mode: QuickPadMode) {
            quickMode.value = mode
            selection.update { if (mode == QuickPadMode.PinCount && it == Selection.Quick(1)) Selection.None else it }
        }

        /** « Raté » sélectionne le raté ; il n'est enregistré qu'avec « Valider ». */
        fun toggleMiss() = selection.update { if (it == Selection.Miss) Selection.None else Selection.Miss }

        fun setInputMode(mode: InputMode) {
            selection.value = Selection.None
            viewModelScope.launch { settings.setInputMode(mode) }
        }

        fun validate() {
            val chosen = selection.value
            val result =
                when (chosen) {
                    Selection.None -> {
                        return
                    }

                    Selection.Miss -> {
                        ThrowResult.Pins(emptyList())
                    }

                    is Selection.Pins -> {
                        ThrowResult.Pins(chosen.pins.sorted())
                    }

                    is Selection.Quick -> {
                        if (chosen.value == 0) {
                            ThrowResult.Pins(emptyList())
                        } else {
                            ThrowResult.Points(
                                chosen.value,
                                if (quickMode.value ==
                                    QuickPadMode.SinglePin
                                ) {
                                    PointsKind.SINGLE_PIN
                                } else {
                                    PointsKind.PIN_COUNT
                                },
                            )
                        }
                    }
                }
            viewModelScope.launch {
                val before =
                    games
                        .observe(gameId)
                        .first()
                        ?.currentSet
                        ?.replay
                        ?.state ?: return@launch
                val team = before.currentTeam ?: return@launch
                val name =
                    games
                        .observe(gameId)
                        .first()
                        ?.team(team.id)
                        ?.name ?: team.id.value
                val outcome = games.append(gameId, Throw(EventId(ids.newId()), result))
                if (outcome !is Outcome.Ok) return@launch
                selection.value = Selection.None
                val after = outcome.value.state
                val points = result.points
                effects.send(GameEffect.Validated(name, points))
                emitConsequences(before, after, team, points, name)
            }
        }

        private suspend fun emitConsequences(
            before: SetState,
            after: SetState,
            team: TeamState,
            points: Int,
            name: String,
        ) {
            val updated = after.team(team.id) ?: return
            val target = before.config.targetFor(team.id)
            when {
                after.status == SetStatus.FINISHED && after.winner == team.id -> effects.send(GameEffect.Won(name))
                points > 0 && team.score + points > target -> effects.send(GameEffect.Overshoot(updated.score))
                updated.eliminated && !team.eliminated -> effects.send(GameEffect.Eliminated(name))
            }
            if (after.status == SetStatus.FINISHED) games.close(gameId)
        }

        fun undo() {
            viewModelScope.launch {
                val snapshot = games.observe(gameId).first() ?: return@launch
                val last =
                    snapshot.currentSet.replay.effectiveLog
                        .lastOrNull() ?: return@launch
                val wasClosed = snapshot.closedAt != null
                if (games.append(gameId, Undo(EventId(ids.newId()), last.id)) is Outcome.Ok &&
                    wasClosed
                ) {
                    games.reopen(gameId)
                }
                selection.value = Selection.None
            }
        }

        fun abandon() {
            viewModelScope.launch {
                if (games.append(gameId, Abandon(EventId(ids.newId()))) is Outcome.Ok) games.close(gameId)
            }
        }

        private fun GameSnapshot.toUiState(prefs: UserSettings, sel: Selection, mode: QuickPadMode): GameUiState {
            val replay = currentSet.replay
            val setState = replay.state
            val teamsUi =
                setState.teams.map { team ->
                    val info = team(team.id)
                    TeamUi(
                        id = team.id,
                        name = info?.name ?: team.id.value,
                        colorIndex = info?.colorIndex ?: 0,
                        score = team.score,
                        misses = team.misses,
                        active = team.id == setState.turn,
                        out = team.eliminated || team.left,
                    )
                }
            val current = setState.currentTeam
            return GameUiState(
                loading = false,
                teams = teamsUi,
                currentTeam = teamsUi.firstOrNull { it.active },
                currentPlayerName = current?.nextPlayer?.let(::playerName),
                target = current?.let { config.targetFor(it.id) } ?: config.targetScore,
                missLimit = config.missLimit,
                inputMode = prefs.inputMode,
                quickMode = mode,
                selection = sel,
                preview = current?.let { preview(setState, it, sel) } ?: Preview(),
                canUndo = replay.effectiveLog.isNotEmpty(),
                finished = setState.status == SetStatus.FINISHED,
                abandoned = setState.status == SetStatus.ABANDONED,
                winnerName = setState.winner?.let { team(it)?.name ?: it.value },
                ranking = setState.ranking.mapNotNull { id -> teamsUi.firstOrNull { it.id == id } },
            )
        }

        private fun preview(state: SetState, team: TeamState, sel: Selection): Preview {
            val points =
                when (sel) {
                    Selection.None -> {
                        return Preview()
                    }

                    Selection.Miss -> {
                        0
                    }

                    is Selection.Pins -> {
                        com.yogistudio.molkkyboard.core.scoring
                            .points(sel.pins)
                    }

                    is Selection.Quick -> {
                        sel.value
                    }
                }
            if (points == 0) {
                val misses = team.misses + 1
                val eliminates = misses >= state.config.missLimit
                return Preview(if (eliminates) PreviewKind.ELIMINATION else PreviewKind.MISS, misses = misses)
            }
            val target = state.config.targetFor(team.id)
            val newScore = StandardRules.scoreAfterThrow(state.config, team, points)
            val kind =
                when {
                    team.score + points > target -> PreviewKind.OVERSHOOT
                    newScore == target -> PreviewKind.WIN
                    else -> PreviewKind.POINTS
                }
            return Preview(kind, points, newScore)
        }

        companion object {
            /** Argument de navigation : identifiant de la partie. */
            const val GAME_ID = "gameId"
            private const val STOP_TIMEOUT_MS = 5_000L
        }
    }
