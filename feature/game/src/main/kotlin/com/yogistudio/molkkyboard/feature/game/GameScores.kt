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
import com.yogistudio.molkkyboard.core.designsystem.component.RoundBadge
import com.yogistudio.molkkyboard.core.designsystem.component.ScoreRing
import com.yogistudio.molkkyboard.core.designsystem.component.ScoreboardRow
import com.yogistudio.molkkyboard.core.designsystem.component.guideAnchor
import com.yogistudio.molkkyboard.core.designsystem.component.rememberGuideAnchors
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
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

// Bandeau des scores et tableau des scores de l'écran de jeu.

/**
 * Bandeau des autres joueurs et de leur score (jamais tronqués), avec une poignée qui invite à le tirer vers
 * le haut : on ouvre ainsi le tableau des scores, ou d'un simple tap (et par l'action TalkBack équivalente).
 * Les équipes hors jeu glissent en fin de bandeau.
 */
@Composable
internal fun ScoresStrip(state: GameUiState, onOpen: () -> Unit) {
    val colors = MolkkyBoardTheme.colors.teams
    val openLabel = stringResource(R.string.game_scoreboard_open)
    val threshold = with(LocalDensity.current) { OPEN_DRAG_THRESHOLD.toPx() }
    val currentOpen by rememberUpdatedState(onOpen)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(SCORES_STRIP_TAG)
                // Glissement vertical (dans les deux sens) repéré dès la passe initiale : les puces et le défilement
                // horizontal du bandeau ne peuvent plus l'intercepter (le geste ne marchait pas sur téléphone).
                .pointerInput(threshold) { detectVerticalSwipe(threshold) { currentOpen() } }
                .clickable(onClickLabel = openLabel, onClick = onOpen),
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
                val color = colors[team.colorIndex % colors.size]
                PlayerChip(
                    name = team.name,
                    color = color,
                    score = team.score,
                    badge = { AvatarBadge(team, color.onFill) },
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
internal fun ScoreboardSheet(state: GameUiState, onDismiss: () -> Unit) {
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

/** Avatar d'une puce : emoji du joueur ou emblème de l'équipe, sinon l'initiale. */
@Composable
private fun AvatarBadge(team: TeamUi, onFill: Color) {
    Text(
        team.avatar ?: team.name.take(1).uppercase(),
        style = MaterialTheme.typography.titleMedium,
        color = onFill,
    )
}

private const val OUT_ALPHA = 0.45f
private val CHIP_MAX_WIDTH = 200.dp
private val OPEN_DRAG_THRESHOLD = 24.dp
