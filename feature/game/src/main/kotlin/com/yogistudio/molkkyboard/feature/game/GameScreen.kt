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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
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
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.component.MissIndicator
import com.yogistudio.molkkyboard.core.designsystem.component.PinGrid
import com.yogistudio.molkkyboard.core.designsystem.component.PlayerChip
import com.yogistudio.molkkyboard.core.designsystem.component.QuickPad
import com.yogistudio.molkkyboard.core.designsystem.component.QuickPadMode
import com.yogistudio.molkkyboard.core.designsystem.component.RoundBadge
import com.yogistudio.molkkyboard.core.designsystem.component.ScoreRing
import com.yogistudio.molkkyboard.core.designsystem.component.ScoreboardRow
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.scoring.EventId
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
)

@Composable
fun GameScreen(
    onBack: () -> Unit,
    onNewGame: () -> Unit,
    onHome: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var overshoot by remember { mutableStateOf<Int?>(null) }
    var shakeKey by remember { mutableIntStateOf(0) }
    var twelve by remember { mutableStateOf<TwelveShow?>(null) }
    var elimination by remember { mutableStateOf<EliminationShow?>(null) }
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
                    scope.launch {
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
                        if (result == SnackbarResult.ActionPerformed) viewModel.undo()
                    }
                }

                is GameEffect.Overshoot -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    overshoot = effect.resetTo
                    shakeKey++
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
                }

                is GameEffect.EliminationCancelled -> {
                    elimination = EliminationShow(effect.team, effect.missLimit, reverse = true, key = ++animationKey)
                }

                is GameEffect.Eliminated -> {
                    elimination = EliminationShow(effect.team, effect.missLimit, reverse = false, key = ++animationKey)
                    scope.launch {
                        // Double vibration à l'élimination.
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        delay(DOUBLE_VIBRATION_GAP_MS)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

                is GameEffect.Won -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
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
            ),
        logMessage = logMessage,
        snackbar = snackbar,
        overshoot = overshoot,
        onOvershootShown = { overshoot = null },
        shakeKey = shakeKey,
        twelve = twelve,
        onTwelveDone = { twelve = null },
        elimination = elimination,
        onEliminationDone = { elimination = null },
    )
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
    overshoot: Int? = null,
    onOvershootShown: () -> Unit = {},
    shakeKey: Int = 0,
    twelve: TwelveShow? = null,
    onTwelveDone: () -> Unit = {},
    elimination: EliminationShow? = null,
    onEliminationDone: () -> Unit = {},
    scoreboardOpen: Boolean = false,
    logOpen: Boolean = false,
    playersOpen: Boolean = false,
    logMessage: String? = null,
) {
    var showScoreboard by rememberSaveable { mutableStateOf(scoreboardOpen) }
    var showLog by rememberSaveable { mutableStateOf(logOpen) }
    var showPlayers by rememberSaveable { mutableStateOf(playersOpen) }
    var showRules by rememberSaveable { mutableStateOf(false) }
    // Surface parente : fond et couleur de texte du thème (clair, sombre, Plein soleil).
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                GameTopBar(state, actions, onOpenScores = { showScoreboard = true }, onOpenLog = {
                    showLog = true
                }, onOpenPlayers = { showPlayers = true }, onOpenRules = {
                    showRules =
                        true
                })
                if (state.loading) return@Column
                ScoresStrip(state, onOpen = { showScoreboard = true })
                ActiveCard(state, shakeKey)
                PreviewLine(state, actions)
                // Pendant « 12 ! », les quilles de la grille s'effacent (elles s'envolent) puis reviennent en fondu.
                val pinsAlpha by animateFloatAsState(if (twelve != null) TWELVE_PINS_ALPHA else 1f, label = "pins")
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    InputArea(state, actions, Modifier.fillMaxSize().alpha(pinsAlpha))
                    // Les quilles s'envolent depuis la grille ; la bulle claque au centre de la zone de saisie.
                    TwelveOverlay(twelve, onTwelveDone)
                }
                ActionButtons(state, actions)
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
            OvershootSticker(overshoot, onOvershootShown, Modifier.align(Alignment.Center))
            // Un 12 gagnant claque d'abord ; l'écran de fin suit.
            if ((state.finished || state.abandoned) && twelve == null) {
                FinishPanel(state, actions, onOpenLog = { showLog = true })
            }
            EliminationOverlay(elimination, onEliminationDone)
        }
    }
    if (showScoreboard && !state.loading) ScoreboardSheet(state, onDismiss = { showScoreboard = false })
    if (showLog && !state.loading) LogSheet(state, actions, logMessage, onDismiss = { showLog = false })
    if (showPlayers && !state.loading) PlayersSheet(state, actions, logMessage, onDismiss = { showPlayers = false })
    if (showRules) RulesSheet(onDismiss = { showRules = false })
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
    onOpenScores: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenPlayers: () -> Unit,
    onOpenRules: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    MBTopBar(stringResource(R.string.game_title), actions.onBack) {
        // Accès visible au tableau des scores, sans dépendre d'un geste.
        TextButton(onClick = onOpenScores, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
            Text(stringResource(R.string.game_scores_button), style = MaterialTheme.typography.titleMedium)
        }
        IconButton(onClick = actions.onUndo, enabled = state.canUndo, modifier = Modifier.size(MBDimens.MinTouch)) {
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

/**
 * Bandeau des autres joueurs et de leur score (jamais tronqués), avec une poignée qui invite à le tirer vers
 * le haut : on ouvre ainsi le tableau des scores, ou d'un simple tap (et par l'action TalkBack équivalente).
 * Les équipes hors jeu glissent en fin de bandeau.
 */
@Composable
private fun ScoresStrip(state: GameUiState, onOpen: () -> Unit) {
    val colors = MolkkyBoardTheme.colors.teams
    val openLabel = stringResource(R.string.game_scoreboard_open)
    val threshold = with(LocalDensity.current) { OPEN_DRAG_THRESHOLD.toPx() }
    var dragged by remember { mutableFloatStateOf(0f) }
    val currentOpen by rememberUpdatedState(onOpen)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                // Glissement vertical dans les deux sens : le bandeau est en haut de l'écran, le « tirer vers le
                // haut » seul n'était pas naturel (retour du test sur téléphone).
                .draggable(
                    state = rememberDraggableState { delta -> dragged += delta },
                    orientation = Orientation.Vertical,
                    onDragStarted = { dragged = 0f },
                    onDragStopped = { if (abs(dragged) > threshold) currentOpen() },
                ).clickable(onClickLabel = openLabel, onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .padding(top = 2.dp, bottom = 4.dp)
                .size(width = 40.dp, height = 5.dp)
                .background(MaterialTheme.colorScheme.outline, CircleShape),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = MBDimens.ScreenPadding, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
        ) {
            val others = state.teams.filterNot { it.active }
            (others.filterNot { it.out } + others.filter { it.out }).forEach { team ->
                PlayerChip(
                    name = team.name,
                    color = colors[team.colorIndex % colors.size],
                    score = team.score,
                    onClick = onOpen,
                    // Largeur bornée : dans un bandeau qui défile, le nom s'abrège au lieu de disparaître.
                    modifier = Modifier.widthIn(max = CHIP_MAX_WIDTH).alpha(if (team.out) OUT_ALPHA else 1f),
                )
            }
        }
    }
}

/**
 * Tableau des scores en temps réel : une feuille qui couvre l'écran de saisie, sans la perdre. Classement par
 * score, joueur actif surligné, équipes hors jeu grisées en bas. On la referme en glissant vers le bas, en
 * touchant à l'extérieur ou avec le bouton retour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreboardSheet(state: GameUiState, onDismiss: () -> Unit) {
    val colors = MolkkyBoardTheme.colors.teams
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = MBDimens.ScreenPadding)
                .padding(bottom = MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.game_scoreboard_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                RoundBadge(state.round)
            }
            Spacer(Modifier.size(8.dp))
            var rank = 0
            var previous: Int? = null
            state.scoreboard.forEachIndexed { index, team ->
                // Égalité : même rang (1, 2, 2, 4).
                if (!team.out && team.score != previous) rank = index + 1
                previous = team.score
                ScoreboardRow(
                    rank = if (team.out) null else rank,
                    name = team.name,
                    color = colors[team.colorIndex % colors.size],
                    score = team.score,
                    remaining = team.remaining,
                    misses = team.misses,
                    missLimit = state.missLimit,
                    active = team.active,
                    status =
                        when {
                            !team.out -> null
                            team.eliminated -> stringResource(R.string.game_eliminated_stamp)
                            else -> stringResource(R.string.game_left)
                        },
                    members = team.members,
                )
            }
        }
    }
}

/** Le joueur actif en très grand : jauge, score, nom, ratés. Tremble en cas de dépassement de la cible. */
@Composable
private fun ActiveCard(state: GameUiState, shakeKey: Int) {
    val team = state.currentTeam ?: return
    val colors = MolkkyBoardTheme.colors.teams
    val shake = remember { Animatable(0f) }
    val reduced = MolkkyBoardTheme.reducedMotion
    LaunchedEffect(shakeKey) {
        if (shakeKey == 0 || reduced) return@LaunchedEffect
        // « Shake » horizontal, puis le score retombe (compteur qui défile vers le bas).
        listOf(-18f, 16f, -12f, 9f, -5f, 0f).forEach {
            shake.animateTo(it, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh))
        }
    }
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MBDimens.ScreenPadding, vertical = 6.dp)
                .offset { IntOffset(shake.value.dp.roundToPx(), 0) },
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
                        team.name,
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
        Text(
            text,
            style =
                if (preview.kind ==
                    PreviewKind.NONE
                ) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.headlineMedium
                },
            fontWeight = if (preview.kind == PreviewKind.NONE) null else FontWeight.ExtraBold,
            color = color,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        // Bascule discrète entre la grille de quilles et le pavé rapide.
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
}

@Composable
private fun ActionButtons(state: GameUiState, actions: GameActions) {
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
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Dépassement de la cible, façon « dommage, raté » : un autocollant corail « Aïe ! Retour à 25 » surgit en
 * penchant, pendant que la carte tremble et que le score dégringole.
 */
@Composable
private fun OvershootSticker(resetTo: Int?, onShown: () -> Unit, modifier: Modifier) {
    var visible by remember { mutableStateOf(false) }
    var value by remember { mutableIntStateOf(0) }
    LaunchedEffect(resetTo) {
        if (resetTo == null) return@LaunchedEffect
        value = resetTo
        visible = true
        delay(STICKER_MS)
        visible = false
        onShown()
    }
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
            modifier = Modifier.rotate(-8f),
        ) {
            Column(
                Modifier.padding(horizontal = 32.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.game_overshoot_title), style = MolkkyBoardTheme.typography.scoreLarge)
                Text(
                    stringResource(R.string.game_overshoot_subtitle, value),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

/** Fin de partie (version simple ; l'animation de victoire et le classement détaillé arrivent au J5). */
@Composable
private fun FinishPanel(state: GameUiState, actions: GameActions, onOpenLog: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f)) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (state.abandoned) {
                    stringResource(
                        R.string.game_abandoned,
                    )
                } else {
                    stringResource(R.string.game_winner, state.winnerName.orEmpty())
                },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            state.ranking.forEachIndexed { index, team ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.game_ranking_line, index + 1, team.name),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(team.score.toString(), style = MolkkyBoardTheme.typography.scoreSmall)
                }
            }
            Spacer(Modifier.size(12.dp))
            BigActionButton(
                stringResource(R.string.game_new_game),
                actions.onNewGame,
                BigActionKind.Primary,
                Modifier.fillMaxWidth(),
            )
            BigActionButton(
                stringResource(R.string.game_home),
                actions.onHome,
                BigActionKind.Secondary,
                Modifier.fillMaxWidth(),
            )
            if (state.finished) {
                TextButton(onClick = actions.onUndo) { Text(stringResource(R.string.game_undo)) }
            }
            TextButton(onClick = onOpenLog) { Text(stringResource(R.string.game_menu_log)) }
        }
    }
}

private const val DOUBLE_VIBRATION_GAP_MS = 180L
private const val OUT_ALPHA = 0.45f
private val CHIP_MAX_WIDTH = 200.dp
private val OPEN_DRAG_THRESHOLD = 24.dp
private const val TWELVE_PINS_ALPHA = 0.1f
private const val STICKER_MS = 1_600L
private const val LOG_MESSAGE_MS = 4_000L
