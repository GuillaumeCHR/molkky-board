package com.yogistudio.molkkyboard.feature.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.yogistudio.molkkyboard.core.designsystem.component.ScoreRing
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.InputMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    KeepScreenOn()
    LaunchedEffect(viewModel) {
        viewModel.effectFlow.collect { effect ->
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

                is GameEffect.Eliminated -> {
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
            ),
        snackbar = snackbar,
        overshoot = overshoot,
        onOvershootShown = { overshoot = null },
        shakeKey = shakeKey,
    )
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
) {
    // Surface parente : fond et couleur de texte du thème (clair, sombre, Plein soleil).
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                GameTopBar(state, actions)
                if (state.loading) return@Column
                OpponentsStrip(state)
                ActiveCard(state, shakeKey)
                PreviewLine(state, actions)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = MBDimens.ScreenPadding),
                    contentAlignment = Alignment.Center,
                ) {
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
                ActionButtons(state, actions)
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
            OvershootSticker(overshoot, onOvershootShown, Modifier.align(Alignment.Center))
            if (state.finished || state.abandoned) FinishPanel(state, actions)
        }
    }
}

@Composable
private fun GameTopBar(state: GameUiState, actions: GameActions) {
    var menu by remember { mutableStateOf(false) }
    var comingSoon by remember { mutableStateOf(false) }
    MBTopBar(stringResource(R.string.game_title), actions.onBack) {
        IconButton(onClick = actions.onUndo, enabled = state.canUndo, modifier = Modifier.size(MBDimens.MinTouch)) {
            Icon(MBIcons.Undo, contentDescription = stringResource(R.string.game_undo))
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(MBDimens.MinTouch)) {
                Icon(MBIcons.More, contentDescription = stringResource(R.string.game_menu))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                listOf(R.string.game_menu_log, R.string.game_menu_players, R.string.game_menu_rules).forEach { label ->
                    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = {
                        menu = false
                        comingSoon = true
                    })
                }
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
    if (comingSoon) {
        LaunchedEffect(Unit) {
            delay(COMING_SOON_MS)
            comingSoon = false
        }
        Text(
            stringResource(R.string.game_coming_soon),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MBDimens.ScreenPadding),
        )
    }
}

/** Bandeau des autres joueurs et de leur score (les scores ne sont jamais tronqués). */
@Composable
private fun OpponentsStrip(state: GameUiState) {
    val colors = MolkkyBoardTheme.colors.teams
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = MBDimens.ScreenPadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
    ) {
        state.teams.filterNot { it.active }.forEach { team ->
            PlayerChip(
                name = team.name,
                color = colors[team.colorIndex % colors.size],
                score = team.score,
                // Largeur bornée : dans un bandeau qui défile, le nom s'abrège au lieu de disparaître.
                modifier = Modifier.widthIn(max = CHIP_MAX_WIDTH).alpha(if (team.out) OUT_ALPHA else 1f),
            )
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
                Text(team.name, style = MaterialTheme.typography.headlineMedium, maxLines = 2)
                val player = state.currentPlayerName
                if (player != null && player != team.name) {
                    Text(
                        player,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MissIndicator(team.misses.coerceAtMost(state.missLimit), limit = state.missLimit)
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
private fun FinishPanel(state: GameUiState, actions: GameActions) {
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
        }
    }
}

private const val DOUBLE_VIBRATION_GAP_MS = 180L
private const val OUT_ALPHA = 0.45f
private val CHIP_MAX_WIDTH = 200.dp
private const val STICKER_MS = 1_600L
private const val COMING_SOON_MS = 2_000L
