package com.yogistudio.molkkyboard.feature.game

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionButton
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionKind
import com.yogistudio.molkkyboard.core.designsystem.component.FitRollingNumber
import com.yogistudio.molkkyboard.core.designsystem.component.FitToScreen
import com.yogistudio.molkkyboard.core.designsystem.component.GuideAnchors
import com.yogistudio.molkkyboard.core.designsystem.component.GuideBubble
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.component.MissIndicator
import com.yogistudio.molkkyboard.core.designsystem.component.PinGrid
import com.yogistudio.molkkyboard.core.designsystem.component.PlayerChip
import com.yogistudio.molkkyboard.core.designsystem.component.QuickPad
import com.yogistudio.molkkyboard.core.designsystem.component.QuickPadMode
import com.yogistudio.molkkyboard.core.designsystem.component.RollingNumber
import com.yogistudio.molkkyboard.core.designsystem.component.RoundBadge
import com.yogistudio.molkkyboard.core.designsystem.component.ScoreRing
import com.yogistudio.molkkyboard.core.designsystem.component.ScoreboardRow
import com.yogistudio.molkkyboard.core.designsystem.component.guideAnchor
import com.yogistudio.molkkyboard.core.designsystem.component.rememberGuideAnchors
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.sound.Sound
import com.yogistudio.molkkyboard.core.designsystem.sound.rememberSoundEffects
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.scoring.EventId
import com.yogistudio.molkkyboard.core.scoring.NextStarter
import com.yogistudio.molkkyboard.core.scoring.ScoringError
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Actions de l'écran de jeu (l'écran lui-même reste sans état, pour les aperçus et les captures). */
data class GameActions(
    val onBack: () -> Unit = {},
    val onTogglePin: (Int, Boolean) -> Unit = { _, _ -> },
    val onQuick: (Int?) -> Unit = {},
    val onQuickMode: (QuickPadMode) -> Unit = {},
    val onMiss: () -> Unit = {},
    val onValidate: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onInputMode: (InputMode) -> Unit = {},
    val onAbandon: () -> Unit = {},
    val onNewGame: () -> Unit = {},
    val onHome: () -> Unit = {},
    val onCorrect: (EventId, ThrowResult) -> Unit = { _, _ -> },
    val onDelete: (EventId) -> Unit = {},
    val onReorder: (List<TeamId>) -> Unit = {},
    val onAddPlayer: (String) -> Unit = {},
    val onRemovePlayer: (PlayerId) -> Unit = {},
    val onCancelPendingOrder: () -> Unit = {},
    val onRematch: (NextStarter) -> Unit = {},
    val onNextSet: () -> Unit = {},
    val onMolkkout: (Set<Int>) -> Unit = {},
    val onMolkkoutUndo: () -> Unit = {},
    val onGuideDone: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onStartPlaying: (neverAgain: Boolean) -> Unit = {},
)

@Composable
fun GameScreen(
    onBack: () -> Unit,
    onNewGame: () -> Unit,
    onHome: () -> Unit,
    onOpenGame: (String) -> Unit = {},
    onShare: () -> Unit = {},
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var overshoot by remember { mutableStateOf<OvershootShow?>(null) }
    var twelve by remember { mutableStateOf<TwelveShow?>(null) }
    var elimination by remember { mutableStateOf<EliminationShow?>(null) }
    var victory by remember { mutableStateOf<VictoryShow?>(null) }
    val voice = state.voice
    val announcer = rememberVoiceAnnouncer(voice.enabled)
    val currentAnnouncer by rememberUpdatedState(announcer)
    val currentVoice by rememberUpdatedState(voice)
    val sounds = rememberSoundEffects(state.sounds)
    val currentSounds by rememberUpdatedState(sounds)
    var animationKey by remember { mutableIntStateOf(0) }
    var logMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(logMessage) {
        if (logMessage == null) return@LaunchedEffect
        delay(LOG_MESSAGE_MS)
        logMessage = null
    }

    KeepScreenOn()
    LaunchedEffect(viewModel) {
        viewModel.effectFlow.collect { effect ->
            sheetMessage(context, effect)?.let { logMessage = it }
            when (effect) {
                is GameEffect.Validated -> {
                    scope.launch { showValidated(snackbar, context, effect, viewModel::undo) }
                }

                is GameEffect.Overshoot -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    overshoot = OvershootShow(effect.team, effect.from, effect.resetTo, ++animationKey)
                }

                is GameEffect.PlayerJoined,
                is GameEffect.PlayerLeft,
                GameEffect.OrderChanged,
                is GameEffect.OrderScheduled,
                -> {
                    Unit
                }

                GameEffect.Edited -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }

                is GameEffect.EditRefused -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                }

                is GameEffect.Twelve -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    twelve = TwelveShow(effect.strike, ++animationKey)
                    currentSounds?.play(Sound.TWELVE)
                }

                is GameEffect.EliminationCancelled -> {
                    elimination = EliminationShow(effect.team, effect.missLimit, reverse = true, key = ++animationKey)
                }

                is GameEffect.Eliminated -> {
                    elimination = EliminationShow(effect.team, effect.missLimit, reverse = false, key = ++animationKey)
                    currentSounds?.play(Sound.ELIMINATED)
                    scope.launch {
                        // Double vibration à l'élimination.
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        delay(DOUBLE_VIBRATION_GAP_MS)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

                is GameEffect.Won -> {
                    victory =
                        VictoryShow(
                            effect.name,
                            effect.colorIndex,
                            effect.target,
                            effect.newRecord,
                            effect.perfect,
                            ++animationKey,
                        )
                    currentSounds?.play(Sound.VICTORY)
                    scope.launch {
                        // Double vibration de célébration.
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        delay(DOUBLE_VIBRATION_GAP_MS)
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    }
                }

                is GameEffect.OpenGame -> {
                    onOpenGame(effect.gameId)
                }

                is GameEffect.Announce -> {
                    currentAnnouncer?.speak(effect.text.text(context), currentVoice.rate, currentVoice.volume)
                }
            }
        }
    }
    GameContent(
        state = state,
        actions =
            GameActions(
                onBack = onBack,
                onTogglePin = viewModel::togglePin,
                onQuick = viewModel::selectQuick,
                onQuickMode = viewModel::setQuickMode,
                onMiss = viewModel::toggleMiss,
                onValidate = viewModel::validate,
                onUndo = viewModel::undo,
                onInputMode = viewModel::setInputMode,
                onAbandon = viewModel::abandon,
                onNewGame = onNewGame,
                onHome = onHome,
                onCorrect = viewModel::correct,
                onDelete = viewModel::delete,
                onReorder = viewModel::reorder,
                onAddPlayer = viewModel::addLatePlayer,
                onRemovePlayer = viewModel::removePlayer,
                onCancelPendingOrder = viewModel::cancelPendingOrder,
                onRematch = viewModel::rematch,
                onNextSet = viewModel::nextSet,
                onMolkkout = viewModel::molkkoutThrow,
                onMolkkoutUndo = viewModel::undoMolkkout,
                onGuideDone = viewModel::finishGuide,
                onShare = onShare,
                onStartPlaying = viewModel::startPlaying,
            ),
        logMessage = logMessage,
        snackbar = snackbar,
        overshoot = overshoot,
        onOvershootShown = { overshoot = null },
        twelve = twelve,
        onTwelveDone = { twelve = null },
        elimination = elimination,
        onEliminationDone = { elimination = null },
        victory = victory,
        onVictoryDone = { victory = null },
    )
}

/** Snackbar « Alice : 7 points », avec « Annuler » pour revenir en arrière d'un tap. */
private suspend fun showValidated(
    snackbar: SnackbarHostState,
    context: Context,
    effect: GameEffect.Validated,
    onUndo: () -> Unit,
) {
    val what =
        if (effect.points == 0) {
            context.getString(R.string.game_validated_miss)
        } else {
            context.getString(R.string.game_validated_points, effect.points)
        }
    snackbar.currentSnackbarData?.dismiss()
    val result =
        snackbar.showSnackbar(
            message = context.getString(R.string.game_validated, effect.name, what),
            actionLabel = context.getString(R.string.game_undo_action),
            duration = SnackbarDuration.Short,
        )
    if (result == SnackbarResult.ActionPerformed) onUndo()
}

/** Retour affiché dans le déroulé ou la gestion des joueurs, pour les effets qui en ont un. */
private fun sheetMessage(context: Context, effect: GameEffect): String? =
    when (effect) {
        is GameEffect.PlayerJoined -> context.getString(R.string.players_joined, effect.name)
        is GameEffect.PlayerLeft -> context.getString(R.string.players_left, effect.name)
        GameEffect.OrderChanged -> context.getString(R.string.players_order_saved)
        is GameEffect.OrderScheduled -> context.getString(R.string.players_order_scheduled, effect.round)
        GameEffect.Edited -> context.getString(R.string.log_edited)
        is GameEffect.EditRefused -> context.getString(refusalMessage(effect.error))
        else -> null
    }

private fun refusalMessage(error: ScoringError): Int =
    when (error) {
        ScoringError.CORRECTION_INVALIDATES_LOG -> R.string.log_refused_invalidates
        ScoringError.NOT_BETWEEN_ROUNDS -> R.string.players_refused_round
        else -> R.string.log_refused
    }

/** Écran maintenu allumé pendant une partie. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
fun GameContent(
    state: GameUiState,
    actions: GameActions,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    overshoot: OvershootShow? = null,
    onOvershootShown: () -> Unit = {},
    twelve: TwelveShow? = null,
    onTwelveDone: () -> Unit = {},
    elimination: EliminationShow? = null,
    onEliminationDone: () -> Unit = {},
    victory: VictoryShow? = null,
    onVictoryDone: () -> Unit = {},
    scoreboardOpen: Boolean = false,
    logOpen: Boolean = false,
    playersOpen: Boolean = false,
    logMessage: String? = null,
) {
    var showScoreboard by rememberSaveable { mutableStateOf(scoreboardOpen) }
    var showLog by rememberSaveable { mutableStateOf(logOpen) }
    var showPlayers by rememberSaveable { mutableStateOf(playersOpen) }
    var showRules by rememberSaveable { mutableStateOf(false) }
    var showPlacement by rememberSaveable { mutableStateOf(false) }
    val anchors = rememberGuideAnchors()
    // Surface parente : fond et couleur de texte du thème (clair, sombre, Plein soleil).
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                GameTopBar(state, actions, anchors, onOpenScores = { showScoreboard = true }, onOpenLog = {
                    showLog = true
                }, onOpenPlayers = { showPlayers = true }, onOpenRules = {
                    showRules =
                        true
                }, onOpenPlacement = { showPlacement = true })
                if (state.loading) return@Column
                ScoresStrip(state, onOpen = { showScoreboard = true })
                ActiveCard(state)
                PreviewLine(state, actions)
                // Pendant « 12 ! », les quilles de la grille s'effacent (elles s'envolent) puis reviennent en fondu.
                val pinsAlpha by animateFloatAsState(if (twelve != null) TWELVE_PINS_ALPHA else 1f, label = "pins")
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    InputArea(state, actions, Modifier.fillMaxSize().alpha(pinsAlpha).guideAnchor(anchors, GUIDE_INPUT))
                    // Les quilles s'envolent depuis la grille ; la bulle claque au centre de la zone de saisie.
                    TwelveOverlay(twelve, onTwelveDone)
                }
                ActionButtons(state, actions, anchors)
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
            OvershootSticker(overshoot, onOvershootShown, Modifier.align(Alignment.Center))
            // Un 12 gagnant claque d'abord, puis la victoire ; l'écran de fin suit.
            if ((state.finished || state.abandoned) && twelve == null) {
                EndPanel(state, actions, onOpenLog = { showLog = true }, onOpenPlacement = { showPlacement = true })
            }
            EliminationOverlay(elimination, onEliminationDone)
            if (twelve == null) VictoryOverlay(victory, onVictoryDone)
            if (twelve == null && elimination == null &&
                !state.readyCard
            ) {
                GameGuide(state, anchors, actions.onGuideDone)
            }
        }
    }
    if (!state.loading) {
        GameSheets(
            state = state,
            actions = actions,
            logMessage = logMessage,
            scoreboard = showScoreboard,
            log = showLog,
            players = showPlayers,
            onDismiss = { sheet ->
                when (sheet) {
                    Sheet.SCOREBOARD -> showScoreboard = false
                    Sheet.LOG -> showLog = false
                    Sheet.PLAYERS -> showPlayers = false
                }
            },
        )
    }
    HelpSheets(state, actions, showRules, onRulesClosed = { showRules = false }, showPlacement) { showPlacement = it }
}

private enum class Sheet { SCOREBOARD, LOG, PLAYERS }

/** Feuilles qui couvrent l'écran de saisie sans la perdre : tableau des scores, déroulé, joueurs. */
@Composable
private fun GameSheets(
    state: GameUiState,
    actions: GameActions,
    logMessage: String?,
    scoreboard: Boolean,
    log: Boolean,
    players: Boolean,
    onDismiss: (Sheet) -> Unit,
) {
    if (scoreboard) ScoreboardSheet(state, onDismiss = { onDismiss(Sheet.SCOREBOARD) })
    if (log) LogSheet(state, actions, logMessage, onDismiss = { onDismiss(Sheet.LOG) })
    if (players) PlayersSheet(state, actions, logMessage, onDismiss = { onDismiss(Sheet.PLAYERS) })
}

/** Grille de quilles ou pavé rapide, selon le mode de saisie. */
@Composable
private fun InputArea(state: GameUiState, actions: GameActions, modifier: Modifier) {
    Box(modifier.padding(horizontal = MBDimens.ScreenPadding), contentAlignment = Alignment.Center) {
        when (state.inputMode) {
            InputMode.PINS -> {
                PinGrid(
                    knocked = (state.selection as? Selection.Pins)?.pins.orEmpty(),
                    onToggle = actions.onTogglePin,
                    enabled = !state.finished,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            InputMode.QUICK -> {
                FitToScreen(Modifier.fillMaxSize()) {
                    QuickPad(
                        selected = (state.selection as? Selection.Quick)?.value,
                        onSelect = actions.onQuick,
                        mode = state.quickMode,
                        onModeChange = actions.onQuickMode,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameTopBar(
    state: GameUiState,
    actions: GameActions,
    anchors: GuideAnchors,
    onOpenScores: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenPlayers: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenPlacement: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    // En grande police, les boutons passent avant le titre « Partie » (évident sur cet écran plein écran).
    val title = if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) "" else stringResource(R.string.game_title)
    MBTopBar(title, actions.onBack) {
        // Accès visible au tableau des scores, sans dépendre d'un geste.
        TextButton(onClick = onOpenScores, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
            Text(stringResource(R.string.game_scores_button), style = MaterialTheme.typography.titleMedium)
        }
        // Placement des quilles, discret mais toujours là (utile entre deux manches).
        IconButton(onClick = onOpenPlacement, modifier = Modifier.size(MBDimens.MinTouch)) {
            Icon(MBIcons.Pins, contentDescription = stringResource(R.string.placement_title))
        }
        IconButton(
            onClick = actions.onUndo,
            enabled = state.canUndo,
            modifier = Modifier.size(MBDimens.MinTouch).guideAnchor(anchors, GUIDE_UNDO),
        ) {
            Icon(MBIcons.Undo, contentDescription = stringResource(R.string.game_undo))
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(MBDimens.MinTouch)) {
                Icon(MBIcons.More, contentDescription = stringResource(R.string.game_menu))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.game_menu_log)) }, onClick = {
                    menu = false
                    onOpenLog()
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.game_menu_players)) }, onClick = {
                    menu = false
                    onOpenPlayers()
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.game_menu_rules)) }, onClick = {
                    menu = false
                    onOpenRules()
                })
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.game_menu_abandon),
                            color = MolkkyBoardTheme.colors.coralText,
                        )
                    },
                    onClick = {
                        menu = false
                        actions.onAbandon()
                    },
                )
            }
        }
    }
}

/** Le joueur actif en très grand : jauge, score, nom, ratés. */
@Composable
private fun ActiveCard(state: GameUiState) {
    val team = state.currentTeam ?: return
    val colors = MolkkyBoardTheme.colors.teams
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MBDimens.ScreenPadding, vertical = 6.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ScoreRing(
                team.score,
                colors[team.colorIndex % colors.size].fill,
                target = state.target,
                size = 150.dp,
                strokeWidth = 13.dp,
            ) {
                FitRollingNumber(team.score, MolkkyBoardTheme.typography.scoreHero)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // La pastille passe sous le nom quand la place manque (grandes polices) : le nom n'est jamais coupé.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        listOfNotNull(team.avatar, team.name).joinToString(" "),
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    RoundBadge(state.round)
                }
                val player = state.currentPlayerName
                if (player != null && player != team.name) {
                    Text(
                        player,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MissIndicator(
                    team.misses.coerceAtMost(state.missLimit),
                    limit = state.missLimit,
                    warning = state.missWarning,
                )
            }
        }
    }
}

@Composable
private fun previewText(state: GameUiState): String {
    val preview = state.preview
    return when (preview.kind) {
        PreviewKind.NONE -> {
            stringResource(
                if (state.inputMode ==
                    InputMode.PINS
                ) {
                    R.string.game_hint_pins
                } else {
                    R.string.game_hint_quick
                },
            )
        }

        PreviewKind.MISS -> {
            stringResource(R.string.game_preview_miss, preview.misses, state.missLimit)
        }

        PreviewKind.ELIMINATION -> {
            stringResource(R.string.game_preview_elimination)
        }

        PreviewKind.POINTS -> {
            stringResource(R.string.game_preview_points, preview.points, preview.newScore)
        }

        PreviewKind.OVERSHOOT -> {
            stringResource(R.string.game_preview_overshoot, preview.points, preview.newScore)
        }

        PreviewKind.WIN -> {
            stringResource(R.string.game_preview_win, preview.points, preview.newScore)
        }
    }
}

@Composable
private fun previewColor(kind: PreviewKind) =
    when (kind) {
        PreviewKind.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
        PreviewKind.MISS, PreviewKind.ELIMINATION, PreviewKind.OVERSHOOT -> MolkkyBoardTheme.colors.coralText
        PreviewKind.WIN -> MaterialTheme.colorScheme.primary
        PreviewKind.POINTS -> MaterialTheme.colorScheme.onSurface
    }

@Composable
private fun PreviewLine(state: GameUiState, actions: GameActions) {
    val preview = state.preview
    val text = previewText(state)
    val color = previewColor(preview.kind)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = MBDimens.ScreenPadding, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewText(text, preview.kind, color, Modifier.weight(1f))
        // Bascule discrète entre la grille de quilles et le pavé rapide.
        InputModeSwitch(state, actions)
    }
}

@Composable
private fun PreviewText(text: String, kind: PreviewKind, color: Color, modifier: Modifier) {
    val idle = kind == PreviewKind.NONE
    Text(
        text,
        style = if (idle) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium,
        fontWeight = if (idle) null else FontWeight.ExtraBold,
        color = color,
        modifier = modifier,
        maxLines = 1,
    )
}

@Composable
private fun InputModeSwitch(state: GameUiState, actions: GameActions) {
    val other = if (state.inputMode == InputMode.PINS) InputMode.QUICK else InputMode.PINS
    TextButton(onClick = { actions.onInputMode(other) }) {
        Text(
            stringResource(
                if (other ==
                    InputMode.QUICK
                ) {
                    R.string.game_switch_to_quick
                } else {
                    R.string.game_switch_to_pins
                },
            ),
        )
    }
}

@Composable
private fun ActionButtons(state: GameUiState, actions: GameActions, anchors: GuideAnchors) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(MBDimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(MBDimens.Gap),
    ) {
        val missSelected = state.selection == Selection.Miss
        BigActionButton(
            text = stringResource(R.string.game_miss),
            onClick = actions.onMiss,
            kind = if (missSelected) BigActionKind.Miss else BigActionKind.Secondary,
            icon = MBIcons.Cross,
            enabled = !state.finished,
            modifier = Modifier.weight(1f),
        )
        BigActionButton(
            text = stringResource(R.string.game_validate),
            onClick = actions.onValidate,
            kind = BigActionKind.Validate,
            icon = MBIcons.Check,
            enabled = state.canValidate,
            modifier = Modifier.weight(1f).guideAnchor(anchors, GUIDE_VALIDATE),
        )
    }
}

/** Un dépassement à mettre en scène : l'équipe qui a dépassé, son score au-delà de la cible et le score de repli. */
data class OvershootShow(
    val team: TeamUi?,
    val from: Int,
    val resetTo: Int,
    val key: Int,
)

/**
 * Dépassement de la cible, façon « dommage, raté » : un autocollant corail « Aïe ! » surgit en penchant, avec le
 * nom de celui qui a dépassé et son score qui dégringole (55 → 25) pendant que l'autocollant tremble. La carte du
 * joueur actif montre déjà le joueur suivant : c'est l'autocollant qui porte le joueur fautif.
 */
@Composable
private fun OvershootSticker(show: OvershootShow?, onShown: () -> Unit, modifier: Modifier) {
    var visible by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<OvershootShow?>(null) }
    var score by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    val reduced = MolkkyBoardTheme.reducedMotion
    LaunchedEffect(show) {
        if (show == null) return@LaunchedEffect
        current = show
        score = if (reduced) show.resetTo else show.from
        visible = true
        if (!reduced) {
            delay(STICKER_DROP_DELAY_MS)
            // « Shake » horizontal, puis le score retombe (compteur qui défile vers le bas).
            score = show.resetTo
            listOf(-18f, 16f, -12f, 9f, -5f, 0f).forEach {
                shake.animateTo(
                    it,
                    spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh),
                )
            }
        }
        delay(STICKER_MS)
        visible = false
        onShown()
    }
    val shown = current ?: return
    val colors = MolkkyBoardTheme.colors.teams
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            scaleIn(
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                initialScale = 0.3f,
            ) +
                fadeIn(),
        exit = fadeOut(),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MolkkyBoardTheme.colors.coral,
            contentColor = MolkkyBoardTheme.colors.onCoral,
            shadowElevation = 8.dp,
            modifier = Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) }.rotate(-8f),
        ) {
            Column(
                Modifier.padding(horizontal = 32.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.game_overshoot_title), style = MolkkyBoardTheme.typography.scoreLarge)
                shown.team?.let { team ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(12.dp).background(colors[team.colorIndex % colors.size].fill, CircleShape))
                        Text(
                            team.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        RollingNumber(score, MolkkyBoardTheme.typography.scoreSmall)
                    }
                }
                Text(
                    stringResource(R.string.game_overshoot_subtitle, shown.resetTo),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

private const val DOUBLE_VIBRATION_GAP_MS = 180L
private const val TWELVE_PINS_ALPHA = 0.1f
private const val STICKER_MS = 1_400L
private const val STICKER_DROP_DELAY_MS = 350L
private const val LOG_MESSAGE_MS = 4_000L

private const val LARGE_FONT_SCALE = 1.3f
