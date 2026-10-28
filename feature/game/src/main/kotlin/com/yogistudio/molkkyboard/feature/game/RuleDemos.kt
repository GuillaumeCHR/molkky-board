package com.yogistudio.molkkyboard.feature.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.component.EliminatedStamp
import com.yogistudio.molkkyboard.core.designsystem.component.MissIndicator
import com.yogistudio.molkkyboard.core.designsystem.component.PinView
import com.yogistudio.molkkyboard.core.designsystem.component.RollingNumber
import com.yogistudio.molkkyboard.core.designsystem.component.ScoreRing
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import kotlinx.coroutines.delay

/**
 * Petits schémas animés des règles, dessinés avec les composants du jeu. Chacun déroule ses étapes deux fois puis
 * s'arrête sur la dernière (pas d'animation sans fin) ; « Rejouer » relance. Avec « Réduire les animations », la
 * dernière étape s'affiche directement.
 */
@Composable
private fun rememberDemoStep(steps: Int, replay: Int): Int {
    val reduced = MolkkyBoardTheme.reducedMotion
    var step by remember { mutableIntStateOf(if (reduced) steps - 1 else 0) }
    LaunchedEffect(replay, reduced) {
        if (reduced) {
            step = steps - 1
            return@LaunchedEffect
        }
        repeat(DEMO_CYCLES) {
            for (s in 0 until steps) {
                step = s
                delay(DEMO_STEP_MS)
            }
        }
    }
    return step
}

@Composable
private fun DemoFrame(caption: String, onReplay: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = DEMO_HEIGHT).clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
        Text(
            caption,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (!MolkkyBoardTheme.reducedMotion) {
            TextButton(onClick = onReplay, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
                Text(stringResource(R.string.rules_demo_replay))
            }
        }
    }
}

/** Une quille seule rapporte son numéro ; plusieurs quilles, leur nombre. */
@Composable
internal fun PointsDemo() {
    var replay by remember { mutableIntStateOf(0) }
    val step = rememberDemoStep(POINTS_STEPS, replay)
    val knocked =
        when (step) {
            1 -> setOf(12)
            3 -> setOf(3, 5, 9)
            else -> emptySet()
        }
    val caption =
        when (step) {
            0, 1 -> stringResource(R.string.rules_demo_single)
            else -> stringResource(R.string.rules_demo_several)
        }
    DemoFrame(caption, onReplay = { replay++ }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 5, 12, 9).forEach { PinView(number = it, knocked = it in knocked, onToggle = null, size = 56.dp) }
        }
    }
}

/** 47 + 5 = 52 : plus de 50, retour à 25. */
@Composable
internal fun OvershootDemo() {
    var replay by remember { mutableIntStateOf(0) }
    val step = rememberDemoStep(OVERSHOOT_STEPS, replay)
    val score =
        when (step) {
            0 -> OVERSHOOT_BEFORE
            1 -> OVERSHOOT_BEFORE + OVERSHOOT_THROW
            else -> OVERSHOOT_RESET
        }
    val caption =
        when (step) {
            0 -> stringResource(R.string.rules_demo_before, OVERSHOOT_BEFORE, OVERSHOOT_THROW)
            1 -> stringResource(R.string.rules_demo_over, OVERSHOOT_BEFORE + OVERSHOOT_THROW)
            else -> stringResource(R.string.rules_demo_reset, OVERSHOOT_RESET)
        }
    DemoFrame(caption, onReplay = { replay++ }) {
        // L'anneau plafonne à 50 : à 52, il est plein, en corail.
        ScoreRing(
            score = score.coerceAtMost(TARGET),
            teamColor = if (step == 1) MolkkyBoardTheme.colors.coral else MaterialTheme.colorScheme.primary,
            size = 120.dp,
            strokeWidth = 12.dp,
        ) {
            RollingNumber(score, style = MolkkyBoardTheme.typography.scoreSmall)
        }
    }
}

/** Trois ratés de suite : éliminé. */
@Composable
internal fun MissesDemo() {
    var replay by remember { mutableIntStateOf(0) }
    val step = rememberDemoStep(MISS_STEPS, replay)
    val caption =
        if (step == MISS_STEPS - 1) {
            stringResource(R.string.rules_demo_eliminated)
        } else {
            stringResource(R.string.rules_demo_miss, step, MISS_STEPS - 1)
        }
    DemoFrame(caption, onReplay = { replay++ }) {
        Box(contentAlignment = Alignment.Center) {
            MissIndicator(misses = step, limit = MISS_STEPS - 1, dotSize = 28.dp, warning = step == MISS_STEPS - 2)
            AnimatedVisibility(step == MISS_STEPS - 1, enter = scaleIn()) {
                EliminatedStamp(stringResource(R.string.game_eliminated_stamp))
            }
        }
    }
}

/** Mölkkout : cinq quilles alignées, remises en place après chaque lancer ; le plus haut total gagne. */
@Composable
internal fun MolkkoutDemo() {
    var replay by remember { mutableIntStateOf(0) }
    val step = rememberDemoStep(MOLKKOUT_STEPS, replay)
    val knocked =
        when (step) {
            1 -> setOf(12)
            2 -> setOf(4, 10)
            else -> emptySet()
        }
    val caption =
        when (step) {
            0 -> stringResource(R.string.rules_demo_molkkout_line)
            1 -> stringResource(R.string.rules_demo_molkkout_a)
            2 -> stringResource(R.string.rules_demo_molkkout_b)
            else -> stringResource(R.string.rules_demo_molkkout_end)
        }
    DemoFrame(caption, onReplay = { replay++ }) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MOLKKOUT_ORDER.forEach { PinView(number = it, knocked = it in knocked, onToggle = null, size = 48.dp) }
        }
    }
}

private const val DEMO_CYCLES = 2
private const val DEMO_STEP_MS = 1_100L
private val DEMO_HEIGHT = 130.dp
private const val POINTS_STEPS = 4
private const val OVERSHOOT_STEPS = 3
private const val OVERSHOOT_BEFORE = 47
private const val OVERSHOOT_THROW = 5
private const val OVERSHOOT_RESET = 25
private const val TARGET = 50
private const val MISS_STEPS = 4
private const val MOLKKOUT_STEPS = 4
