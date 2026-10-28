package com.yogistudio.molkkyboard.feature.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yogistudio.molkkyboard.core.data.GamePlayer
import com.yogistudio.molkkyboard.core.data.GameRepository
import com.yogistudio.molkkyboard.core.data.GameSnapshot
import com.yogistudio.molkkyboard.core.data.GameTeam
import com.yogistudio.molkkyboard.core.data.IdGenerator
import com.yogistudio.molkkyboard.core.data.PlayerRepository
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.data.StatsRepository
import com.yogistudio.molkkyboard.core.data.memberKey
import com.yogistudio.molkkyboard.core.data.toStatsGame
import com.yogistudio.molkkyboard.core.designsystem.component.QuickPadMode
import com.yogistudio.molkkyboard.core.designsystem.icon.emblemSymbol
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.MissPenalty
import com.yogistudio.molkkyboard.core.model.Overshoot
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
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
import com.yogistudio.molkkyboard.core.scoring.MatchStatus
import com.yogistudio.molkkyboard.core.scoring.MolkkoutThrow
import com.yogistudio.molkkyboard.core.scoring.NextStarter
import com.yogistudio.molkkyboard.core.scoring.Outcome
import com.yogistudio.molkkyboard.core.scoring.PlayEvent
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
import com.yogistudio.molkkyboard.core.stats.PERFECT_GAME_THROWS
import com.yogistudio.molkkyboard.core.stats.SetSummary
import com.yogistudio.molkkyboard.core.stats.isNewRecord
import com.yogistudio.molkkyboard.core.stats.summarize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Une fonction par action de l'écran de jeu : les regrouper rendrait l'interface moins lisible.
@Suppress("TooManyFunctions")
@HiltViewModel
class GameViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val games: GameRepository,
        private val settings: SettingsRepository,
        private val ids: IdGenerator,
        private val stats: StatsRepository,
        players: PlayerRepository,
    ) : ViewModel() {
        /** Avatars des profils locaux (emoji), pour les joueurs seuls. */
        private val avatars: StateFlow<Map<String, String>> =
            players.profiles
                .map { list -> list.mapNotNull { p -> p.avatar?.let { p.id to it } }.toMap() }
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

        private val gameId: String = checkNotNull(savedStateHandle[GAME_ID])
        private val handle = savedStateHandle

        /** Carte « Prêts à jouer ? » déjà passée pour cette partie (survit à la recréation de l'écran). */
        private val readySeen = savedStateHandle.getStateFlow(READY_SEEN, false)
        private val selection = MutableStateFlow<Selection>(Selection.None)
        private val quickMode = MutableStateFlow(QuickPadMode.SinglePin)

        /** Nouvel ordre demandé en plein tour : il s'appliquera au début du tour suivant. */
        private val pendingOrder = MutableStateFlow<List<TeamId>?>(null)

        /** Record battu pendant cette partie : annoncé une fois, puis rappelé sur l'écran de fin. */
        private val newRecord = MutableStateFlow(false)
        private val effects = Channel<GameEffect>(Channel.BUFFERED)
        val effectFlow: Flow<GameEffect> = effects.receiveAsFlow()

        private val snapshots = games.observe(gameId).filterNotNull()

        /** L'historique n'est résumé qu'une fois la manche finie (série, record) : rien à recalculer pendant le jeu. */
        @OptIn(ExperimentalCoroutinesApi::class)
        private val history: Flow<List<SetSummary>> =
            snapshots
                .map { it.currentSet.replay.state.status == SetStatus.FINISHED }
                .distinctUntilChanged()
                .flatMapLatest { finished -> if (finished) stats.summaries else flowOf(emptyList()) }

        val state: StateFlow<GameUiState> =
            combine(
                snapshots,
                settings.settings,
                combine(selection, quickMode, pendingOrder, readySeen, ::InputState),
                combine(history, newRecord, avatars, ::Triple),
            ) { snapshot, prefs, input, (past, record, faces) ->
                snapshot.toUiState(prefs, input.selection, input.mode, past, record, faces).copy(
                    pendingOrder = input.pending,
                    readyCard = snapshot.showsReadyCard(prefs, input.readySeen),
                    sounds = prefs.sounds,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GameUiState())

        private data class InputState(
            val selection: Selection,
            val mode: QuickPadMode,
            val pending: List<TeamId>?,
            val readySeen: Boolean,
        )

        /** « Prêts à jouer ? » : au tout début de la première manche, si la carte est active. */
        private fun GameSnapshot.showsReadyCard(prefs: UserSettings, seen: Boolean): Boolean =
            prefs.showReadyCard &&
                !seen &&
                currentSet.index == 0 &&
                currentSet.replay.effectiveLog.isEmpty() &&
                currentSet.replay.state.status == SetStatus.IN_PROGRESS

        /** « C'est parti » : la carte se ferme ; « Ne plus afficher » est retenu. */
        fun startPlaying(neverAgain: Boolean) {
            handle[READY_SEEN] = true
            viewModelScope.launch {
                settings.recordGameStarted()
                if (neverAgain) settings.setReadyCard(false)
            }
        }

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
            val result = chosen.toResult(quickMode.value) ?: return
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
                // La sélection est vidée avant l'écriture : le joueur suivant n'hérite jamais des quilles du précédent,
                // même un instant. Elle revient si le moteur refuse le lancer.
                selection.value = Selection.None
                val outcome = games.append(gameId, Throw(EventId(ids.newId()), result))
                if (outcome !is Outcome.Ok) {
                    selection.value = chosen
                    return@launch
                }
                val after = outcome.value.state
                val points = result.points
                effects.send(GameEffect.Validated(name, points, (result as? ThrowResult.Pins)?.pins.orEmpty().toSet()))
                emitConsequences(before, after, team, result)
                applyPendingOrder(after)
                announceNext(
                    before,
                    games
                        .observe(gameId)
                        .first()
                        ?.currentSet
                        ?.replay
                        ?.state ?: after,
                )
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
            if (points == TWELVE && !overshoot) {
                effects.send(GameEffect.Twelve(strike = result.isStrike))
                announce(Announcement.Twelve)
            }
            when {
                overshoot -> {
                    effects.send(
                        GameEffect.Overshoot(
                            updated.score,
                            snapshot.teamUi(updated, faces = avatars.value),
                            from = team.score + points,
                        ),
                    )
                    announce(Announcement.Reset(updated.score))
                }

                updated.eliminated && !team.eliminated -> {
                    effects.send(
                        GameEffect.Eliminated(snapshot.teamUi(updated, faces = avatars.value), after.config.missLimit),
                    )
                    announce(Announcement.Eliminated(snapshot.team(team.id)?.name ?: team.id.value))
                }
            }
            if (after.status == SetStatus.FINISHED) celebrate(snapshot)
            syncClosed(snapshot)
        }

        /** Victoire en direct : animation, record éventuel (comparé à l'historique d'avant la partie), annonce. */
        private suspend fun celebrate(snapshot: GameSnapshot) {
            val state = snapshot.currentSet.replay.state
            val winner = state.winner ?: return
            val added = summarize(snapshot.toStatsGame()).firstOrNull { it.setIndex == snapshot.currentSet.index }
            val record =
                added != null && isNewRecord(stats.summaries.first().filterNot { it.gameId == gameId }, added)
            if (record) newRecord.value = true
            val throws = added?.winnerTeam?.throws ?: 0
            val info = snapshot.team(winner)
            effects.send(
                GameEffect.Won(
                    name = info?.name ?: winner.value,
                    colorIndex = info?.colorIndex ?: 0,
                    target = snapshot.config.targetFor(winner),
                    newRecord = record,
                    perfect = added?.standard == true && throws in 1..PERFECT_GAME_THROWS,
                ),
            )
            val team = (state.team(winner)?.players?.size ?: 1) > 1
            announce(Announcement.Victory(info?.name ?: winner.value, team))
        }

        /** Partie close quand le match est fini (ou abandonnée) ; rouverte si une correction défait la fin. */
        private suspend fun syncClosed(snapshot: GameSnapshot) {
            val done = snapshot.abandoned || snapshot.match is MatchStatus.Finished
            if (done && snapshot.closedAt == null) games.close(gameId)
            if (!done && snapshot.closedAt != null) games.reopen(gameId)
        }

        private suspend fun announce(text: Announcement) = effects.send(GameEffect.Announce(text))

        /** « Tour 4 » au changement de tour, puis « Au tour de Léa, il lui faut 7 » quand la victoire est à portée. */
        private suspend fun announceNext(before: SetState, after: SetState) {
            if (after.status != SetStatus.IN_PROGRESS) return
            if (after.round > before.round) announce(Announcement.Round(after.round))
            val team = after.currentTeam ?: return
            val snapshot = games.observe(gameId).first() ?: return
            val needed = (after.config.targetFor(team.id) - team.score).takeIf { it in 1..TWELVE }
            val name = if (team.players.size > 1) snapshot.playerName(team.nextPlayer) else snapshot.team(team.id)?.name
            announce(Announcement.Turn(name ?: team.id.value, needed))
        }

        /** Le lancer sélectionné, tel qu'il sera enregistré ; `null` sans sélection. */
        private fun Selection.toResult(mode: QuickPadMode): ThrowResult? =
            when (this) {
                Selection.None -> {
                    null
                }

                Selection.Miss -> {
                    ThrowResult.Pins(emptyList())
                }

                is Selection.Pins -> {
                    ThrowResult.Pins(pins.sorted())
                }

                is Selection.Quick -> {
                    when {
                        value == 0 -> ThrowResult.Pins(emptyList())
                        mode == QuickPadMode.SinglePin -> ThrowResult.Points(value, PointsKind.SINGLE_PIN)
                        else -> ThrowResult.Points(value, PointsKind.PIN_COUNT)
                    }
                }
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
            val updated = games.observe(gameId).first() ?: return
            syncClosed(updated)
            after.teams
                .filter { !it.eliminated && before.team(it.id)?.eliminated == true }
                .forEach {
                    effects.send(
                        GameEffect.EliminationCancelled(
                            snapshot.teamUi(it, faces = avatars.value),
                            before.config.missLimit,
                        ),
                    )
                }
            // Victoire nouvelle, ou vainqueur changé par la correction.
            val winner = after.winner.takeIf { finished }
            val newWinner = before.status != SetStatus.FINISHED || before.winner != winner
            if (winner != null && newWinner) celebrate(updated)
        }

        /** Nouvel ordre de passage, entre deux tours seulement (le moteur refuse sinon). */
        fun reorder(order: List<TeamId>) {
            viewModelScope.launch {
                val state =
                    games
                        .observe(gameId)
                        .first()
                        ?.currentSet
                        ?.replay
                        ?.state ?: return@launch
                if (!state.betweenRounds) {
                    // Les règles n'autorisent un nouvel ordre qu'entre deux tours : on le garde pour le tour suivant.
                    pendingOrder.value = order
                    effects.send(GameEffect.OrderScheduled(state.round + 1))
                    return@launch
                }
                when (val outcome = games.append(gameId, Reorder(EventId(ids.newId()), order))) {
                    is Outcome.Err -> effects.send(GameEffect.EditRefused(outcome.error))
                    is Outcome.Ok -> effects.send(GameEffect.OrderChanged)
                }
            }
        }

        fun cancelPendingOrder() {
            pendingOrder.value = null
        }

        /** Applique l'ordre en attente dès que le tour est terminé. */
        private suspend fun applyPendingOrder(after: SetState) {
            val order = pendingOrder.value ?: return
            if (after.status != SetStatus.IN_PROGRESS || !after.betweenRounds) return
            pendingOrder.value = null
            // Les équipes arrivées depuis la demande se placent en fin d'ordre.
            val full =
                order.filter { id -> after.teams.any { it.id == id } } +
                    after.teams.map { it.id }.filterNot { it in order }
            if (games.append(
                    gameId,
                    Reorder(EventId(ids.newId()), full),
                ) is Outcome.Ok
            ) {
                effects.send(GameEffect.OrderChanged)
            }
        }

        /** Un joueur en retard rejoint la partie : nouvelle équipe en fin d'ordre, à 0, première couleur libre. */
        fun addLatePlayer(name: String) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return
            viewModelScope.launch {
                val snapshot = games.observe(gameId).first() ?: return@launch
                val player = PlayerId(ids.newId())
                val team = TeamId(player.value)
                val used = snapshot.teams.map { it.colorIndex }.toSet()
                val color = (0 until PALETTE_SIZE).firstOrNull { it !in used } ?: (snapshot.teams.size % PALETTE_SIZE)
                val outcome =
                    games.addParticipant(
                        gameId,
                        AddPlayer(EventId(ids.newId()), player, team),
                        GamePlayer(player, trimmed),
                        GameTeam(team, trimmed, color, memberKey = memberKey(listOf(player))),
                    )
                when (outcome) {
                    is Outcome.Err -> effects.send(GameEffect.EditRefused(outcome.error))
                    is Outcome.Ok -> effects.send(GameEffect.PlayerJoined(trimmed))
                }
            }
        }

        /** Un joueur quitte la partie ; s'il ne reste qu'une équipe en lice, elle gagne la manche. */
        fun removePlayer(player: PlayerId) {
            viewModelScope.launch {
                val snapshot = games.observe(gameId).first() ?: return@launch
                when (val outcome = games.append(gameId, RemovePlayer(EventId(ids.newId()), player))) {
                    is Outcome.Err -> {
                        effects.send(GameEffect.EditRefused(outcome.error))
                    }

                    is Outcome.Ok -> {
                        effects.send(GameEffect.PlayerLeft(snapshot.playerName(player)))
                        reconcile(snapshot, outcome.value.state)
                    }
                }
            }
        }

        /** Revanche : mêmes équipes, ordre de passage selon [order] ; l'écran ouvre la nouvelle partie. */
        fun rematch(order: NextStarter) {
            viewModelScope.launch {
                val id = games.createRematch(gameId, order) ?: return@launch
                effects.send(GameEffect.OpenGame(id))
            }
        }

        /** Manche suivante d'un match, dans l'ordre que le match impose (départage par Mölkkout compris). */
        fun nextSet() {
            viewModelScope.launch {
                val snapshot = games.observe(gameId).first() ?: return@launch
                val next = snapshot.match as? MatchStatus.NextSet ?: return@launch
                val last = snapshot.currentSet.replay.state
                val lineup =
                    next.order.mapNotNull { id ->
                        last.team(id)?.takeUnless { it.left }?.let { TeamLineup(id, it.players) }
                    }
                newRecord.value = false
                games.startNextSet(gameId, lineup)
            }
        }

        /** Lancer de Mölkkout : quilles 4, 6, 8, 10 et 12, remises en place après chaque lancer. */
        fun molkkoutThrow(pins: Set<Int>) {
            viewModelScope.launch {
                val state = games.observe(gameId).first()?.molkkoutState ?: return@launch
                val team = state.nextTeam ?: return@launch
                val player = state.nextPlayer ?: return@launch
                games.appendMolkkout(gameId, MolkkoutThrow(team.id, player, pins.sorted()))
            }
        }

        fun undoMolkkout() {
            viewModelScope.launch { games.undoMolkkout(gameId) }
        }

        fun abandon() {
            viewModelScope.launch {
                if (games.append(gameId, Abandon(EventId(ids.newId()))) is Outcome.Ok) games.close(gameId)
            }
        }

        private fun GameSnapshot.toUiState(
            prefs: UserSettings,
            sel: Selection,
            mode: QuickPadMode,
            history: List<SetSummary>,
            record: Boolean,
            faces: Map<String, String>,
        ): GameUiState {
            val replay = currentSet.replay
            val setState = replay.state
            val teamsUi = setState.teams.map { teamUi(it, active = it.id == setState.turn, faces = faces) }
            val current = setState.currentTeam
            // Le prochain raté serait sanctionné (rien en Ludique « on continue », ni pour un joueur exempté).
            val warning =
                current != null &&
                    config.missLimit > 1 &&
                    current.misses == config.missLimit - 1 &&
                    config.onMissLimit != MissPenalty.Nothing &&
                    !config.isExemptFromMissLimit(current.id)
            return GameUiState(
                loading = false,
                teams = teamsUi,
                currentTeam = teamsUi.firstOrNull { it.active },
                currentPlayerName = current?.nextPlayer?.let(::playerName),
                target = current?.let { config.targetFor(it.id) } ?: config.targetScore,
                missLimit = config.missLimit,
                round = setState.round,
                betweenRounds = setState.betweenRounds,
                missWarning = warning,
                missPenalty = config.onMissLimit.takeIf { warning },
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
                end = if (setState.status == SetStatus.FINISHED) endUi(teamsUi, history, record) else null,
                voice = VoicePrefs(prefs.voiceAnnouncements, prefs.voiceRate, prefs.voiceVolume),
                guide = guideStep(prefs, setState, replay.effectiveLog, sel),
            )
        }

        /** Première partie guidée : quilles, puis « Valider », puis l'annulation, jusqu'à « Compris » ou « Passer ». */
        private fun guideStep(prefs: UserSettings, state: SetState, log: List<PlayEvent>, sel: Selection): GuideStep? =
            when {
                prefs.onboardingDone || state.status != SetStatus.IN_PROGRESS -> null
                log.any { it is Throw || it is Fault } -> GuideStep.UNDO
                sel == Selection.None -> GuideStep.PINS
                else -> GuideStep.VALIDATE
            }

        /** Fin (ou abandon) de la prise en main : elle ne revient que si on la redemande dans les Réglages. */
        fun finishGuide() {
            viewModelScope.launch { settings.setOnboardingDone(true) }
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
            private const val PALETTE_SIZE = 8

            /** Argument de navigation : identifiant de la partie. */
            const val GAME_ID = "gameId"
            private const val READY_SEEN = "ready_seen"
            private const val STOP_TIMEOUT_MS = 5_000L
        }
    }

/** Une équipe telle que l'écran l'affiche. */
internal fun GameSnapshot.teamUi(
    team: TeamState,
    active: Boolean = false,
    faces: Map<String, String> = emptyMap(),
): TeamUi {
    val info = team(team.id)
    // Joueur seul : l'emoji de son profil ; équipe : son emblème.
    val avatar =
        if (team.players.size == 1) {
            players.firstOrNull { it.id == team.players.first() }?.profileId?.let(faces::get)
        } else {
            info?.emblem?.let(::emblemSymbol)
        }
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
        memberIds = team.players,
        avatar = avatar,
    )
}

/** Déroulé de la manche en cours : le journal effectif rejoué et attribué, groupé par tour. */
internal fun GameSnapshot.logRounds(): List<LogRound> {
    val initial = start(config, currentSet.lineup).getOrNull() ?: return emptyList()
    // Score courant de chaque équipe, mis à jour lancer après lancer : donne le récapitulatif de fin de tour.
    val scores = linkedMapOf<TeamId, Int>().apply { initial.teams.forEach { put(it.id, it.score) } }
    return describe(initial, currentSet.replay.effectiveLog)
        .groupBy { it.round }
        .map { (round, entries) ->
            entries.forEach { entry ->
                val team = entry.team ?: (entry.event as? AddPlayer)?.team
                if (team != null) scores[team] = entry.scoreAfter ?: scores[team] ?: 0
            }
            LogRound(
                round = round,
                items = entries.map { logItem(it) },
                standings = scores.map { (id, score) -> (team(id)?.name ?: id.value) to score },
            )
        }
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
