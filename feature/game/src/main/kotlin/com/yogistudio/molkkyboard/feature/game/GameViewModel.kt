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
import com.yogistudio.molkkyboard.core.model.MissPenalty
import com.yogistudio.molkkyboard.core.model.UserSettings
import com.yogistudio.molkkyboard.core.model.isExemptFromMissLimit
import com.yogistudio.molkkyboard.core.model.targetFor
import com.yogistudio.molkkyboard.core.scoring.Abandon
import com.yogistudio.molkkyboard.core.scoring.AddPlayer
import com.yogistudio.molkkyboard.core.scoring.Correct
import com.yogistudio.molkkyboard.core.scoring.Delete
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.Fault
import com.yogistudio.molkkyboard.core.scoring.LogEntry
import com.yogistudio.molkkyboard.core.scoring.Outcome
import com.yogistudio.molkkyboard.core.scoring.PointsKind
import com.yogistudio.molkkyboard.core.scoring.RemovePlayer
import com.yogistudio.molkkyboard.core.scoring.Reorder
import com.yogistudio.molkkyboard.core.scoring.SetEvent
import com.yogistudio.molkkyboard.core.scoring.SetState
import com.yogistudio.molkkyboard.core.scoring.SetStatus
import com.yogistudio.molkkyboard.core.scoring.StandardRules
import com.yogistudio.molkkyboard.core.scoring.TeamState
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import com.yogistudio.molkkyboard.core.scoring.Undo
import com.yogistudio.molkkyboard.core.scoring.WrongTeam
import com.yogistudio.molkkyboard.core.scoring.describe
import com.yogistudio.molkkyboard.core.scoring.getOrNull
import com.yogistudio.molkkyboard.core.scoring.points
import com.yogistudio.molkkyboard.core.scoring.start
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
                emitConsequences(before, after, team, result)
            }
        }

        /**
         * Animations d'un lancer validé, par priorité : la victoire passe avant tout ; un 12 qui fait dépasser la
         * cible ne se célèbre pas (seul le retour en arrière est mis en scène) ; une élimination qui désigne un
         * vainqueur s'enchaîne avec la victoire.
         */
        private suspend fun emitConsequences(before: SetState, after: SetState, team: TeamState, result: ThrowResult) {
            val updated = after.team(team.id) ?: return
            val points = result.points
            val overshoot = points > 0 && team.score + points > before.config.targetFor(team.id)
            val snapshot = games.observe(gameId).first() ?: return
            if (points == TWELVE && !overshoot) effects.send(GameEffect.Twelve(strike = result.isStrike))
            when {
                overshoot -> {
                    effects.send(GameEffect.Overshoot(updated.score))
                }

                updated.eliminated && !team.eliminated -> {
                    effects.send(
                        GameEffect.Eliminated(snapshot.teamUi(updated), after.config.missLimit),
                    )
                }
            }
            val winner = after.winner
            if (after.status == SetStatus.FINISHED && winner != null) {
                effects.send(GameEffect.Won(snapshot.team(winner)?.name ?: winner.value))
            }
            if (after.status == SetStatus.FINISHED) games.close(gameId)
        }

        private val ThrowResult.isStrike: Boolean
            get() =
                when (this) {
                    is ThrowResult.Pins -> pins.size == TWELVE
                    is ThrowResult.Points -> kind == PointsKind.PIN_COUNT
                }

        fun undo() {
            viewModelScope.launch {
                val snapshot = games.observe(gameId).first() ?: return@launch
                val last =
                    snapshot.currentSet.replay.effectiveLog
                        .lastOrNull() ?: return@launch
                val outcome = games.append(gameId, Undo(EventId(ids.newId()), last.id))
                if (outcome is Outcome.Ok) reconcile(snapshot, outcome.value.state)
                selection.value = Selection.None
            }
        }

        /** Corrige un lancer passé du déroulé : tout est recalculé (scores, retours à 25, éliminations, vainqueur). */
        fun correct(target: EventId, result: ThrowResult) =
            edit(Correct(EventId(ids.newId()), target, Throw(EventId(ids.newId()), result)))

        /** Supprime un lancer ou une faute du déroulé ; les lancers suivants sont réattribués par position. */
        fun delete(target: EventId) = edit(Delete(EventId(ids.newId()), target))

        private fun edit(event: SetEvent) {
            viewModelScope.launch {
                val snapshot = games.observe(gameId).first() ?: return@launch
                when (val outcome = games.append(gameId, event)) {
                    is Outcome.Err -> {
                        effects.send(GameEffect.EditRefused(outcome.error))
                    }

                    is Outcome.Ok -> {
                        reconcile(snapshot, outcome.value.state)
                        effects.send(GameEffect.Edited)
                    }
                }
            }
        }

        /**
         * Après une annulation ou une correction : la partie se rouvre si la victoire a disparu (l'écran de fin se
         * referme) ou se clôt si elle apparaît ; une élimination défaite se rejoue à l'envers, une nouvelle victoire
         * est célébrée.
         */
        private suspend fun reconcile(snapshot: GameSnapshot, after: SetState) {
            val before = snapshot.currentSet.replay.state
            val finished = after.status == SetStatus.FINISHED
            if (finished && snapshot.closedAt == null) games.close(gameId)
            if (!finished && snapshot.closedAt != null) games.reopen(gameId)
            after.teams
                .filter { !it.eliminated && before.team(it.id)?.eliminated == true }
                .forEach { effects.send(GameEffect.EliminationCancelled(snapshot.teamUi(it), before.config.missLimit)) }
            // Victoire nouvelle, ou vainqueur changé par la correction.
            val winner = after.winner.takeIf { finished }
            val newWinner = before.status != SetStatus.FINISHED || before.winner != winner
            if (winner != null && newWinner) effects.send(GameEffect.Won(snapshot.team(winner)?.name ?: winner.value))
        }

        fun abandon() {
            viewModelScope.launch {
                if (games.append(gameId, Abandon(EventId(ids.newId()))) is Outcome.Ok) games.close(gameId)
            }
        }

        private fun GameSnapshot.toUiState(prefs: UserSettings, sel: Selection, mode: QuickPadMode): GameUiState {
            val replay = currentSet.replay
            val setState = replay.state
            val teamsUi = setState.teams.map { teamUi(it, active = it.id == setState.turn) }
            val current = setState.currentTeam
            return GameUiState(
                loading = false,
                teams = teamsUi,
                currentTeam = teamsUi.firstOrNull { it.active },
                currentPlayerName = current?.nextPlayer?.let(::playerName),
                target = current?.let { config.targetFor(it.id) } ?: config.targetScore,
                missLimit = config.missLimit,
                round = setState.round,
                missWarning =
                    current != null &&
                        config.missLimit > 1 &&
                        current.misses == config.missLimit - 1 &&
                        config.onMissLimit != MissPenalty.Nothing &&
                        !config.isExemptFromMissLimit(current.id),
                inputMode = prefs.inputMode,
                quickMode = mode,
                selection = sel,
                preview = current?.let { preview(setState, it, sel) } ?: Preview(),
                canUndo = replay.effectiveLog.isNotEmpty(),
                finished = setState.status == SetStatus.FINISHED,
                abandoned = setState.status == SetStatus.ABANDONED,
                winnerName = setState.winner?.let { team(it)?.name ?: it.value },
                ranking = setState.ranking.mapNotNull { id -> teamsUi.firstOrNull { it.id == id } },
                log = logRounds(),
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
                // En mode Ludique sans élimination (ou pour un joueur exempté), le 3e raté n'élimine pas.
                val eliminates =
                    misses >= state.config.missLimit &&
                        state.config.onMissLimit == MissPenalty.Eliminate &&
                        !state.config.isExemptFromMissLimit(team.id)
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
            private const val TWELVE = 12

            /** Argument de navigation : identifiant de la partie. */
            const val GAME_ID = "gameId"
            private const val STOP_TIMEOUT_MS = 5_000L
        }
    }

/** Une équipe telle que l'écran l'affiche. */
internal fun GameSnapshot.teamUi(team: TeamState, active: Boolean = false): TeamUi {
    val info = team(team.id)
    return TeamUi(
        id = team.id,
        name = info?.name ?: team.id.value,
        colorIndex = info?.colorIndex ?: 0,
        score = team.score,
        misses = team.misses,
        active = active,
        out = team.eliminated || team.left,
        eliminated = team.eliminated,
        remaining = (config.targetFor(team.id) - team.score).coerceAtLeast(0),
        members = team.players.map(::playerName),
    )
}

/** Déroulé de la manche en cours : le journal effectif rejoué et attribué, groupé par tour. */
internal fun GameSnapshot.logRounds(): List<LogRound> {
    val initial = start(config, currentSet.lineup).getOrNull() ?: return emptyList()
    return describe(initial, currentSet.replay.effectiveLog)
        .map { logItem(it) }
        .groupBy { it.round }
        .map { (round, items) -> LogRound(round, items) }
}

private fun GameSnapshot.logItem(entry: LogEntry): LogItemUi {
    val event = entry.event
    val team = entry.team?.let(::team)
    val base =
        LogItemUi(
            id = event.id,
            round = entry.round,
            kind = LogKind.ABANDON,
            teamName = team?.name ?: entry.team?.value,
            colorIndex = team?.colorIndex ?: 0,
            playerName = entry.player?.let(::playerName),
            scoreBefore = entry.scoreBefore,
            scoreAfter = entry.scoreAfter,
        )
    return when (event) {
        is Throw -> base.copy(kind = LogKind.THROW, result = event.result, points = event.result.points)
        is Fault -> base.copy(kind = LogKind.FAULT, faultKind = event.kind)
        is WrongTeam -> base.copy(kind = LogKind.WRONG_TEAM, result = event.result, points = event.result.points)
        is AddPlayer -> base.copy(kind = LogKind.JOINED, playerName = playerName(event.player))
        is RemovePlayer -> base.copy(kind = LogKind.LEFT, playerName = playerName(event.player))
        is Reorder -> base.copy(kind = LogKind.REORDER)
        is Abandon -> base
    }
}
