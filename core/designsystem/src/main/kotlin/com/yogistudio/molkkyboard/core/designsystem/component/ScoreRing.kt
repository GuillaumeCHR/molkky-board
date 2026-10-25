package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.R
import com.yogistudio.molkkyboard.core.designsystem.theme.MBMotion
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/** Un lancer rapporte au plus 12 points : en dessous de cet écart, la victoire est à un lancer. */
const val MAX_THROW_POINTS = 12

/** À partir de ce score, la jauge commence à virer au safran (25 = score de retour après dépassement). */
private const val WARM_FROM = 25

private const val PULSE_MS = 450
private const val PULSE_COUNT = 3
private const val PULSE_SCALE = 1.35f
private const val FULL_CIRCLE = 360f
private const val START_ANGLE = -90f

/** Vrai quand un seul lancer peut suffire à gagner (il manque entre 1 et 12 points). */
fun isOneThrowFromWin(score: Int, target: Int): Boolean = target - score in 1..MAX_THROW_POINTS

/**
 * Jauge de progression épaisse de 0 à la cible. Elle prend la couleur de l'équipe, vire au safran à
 * l'approche de la cible et pulse (trois fois) quand un seul lancer peut faire gagner. Le contenu (le score par défaut)
 * est centré dans l'anneau.
 */
@Composable
fun ScoreRing(
    score: Int,
    teamColor: Color,
    modifier: Modifier = Modifier,
    target: Int = 50,
    size: Dp = 200.dp,
    strokeWidth: Dp = 18.dp,
    content: @Composable () -> Unit = {
        RollingNumber(
            value = score,
            style = MolkkyBoardTheme.typography.scoreLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    },
) {
    val colors = MolkkyBoardTheme.colors
    val reduced = MolkkyBoardTheme.reducedMotion
    val progress by animateFloatAsState(
        targetValue = (score.toFloat() / target).coerceIn(0f, 1f),
        animationSpec = if (reduced) snap() else MBMotion.gentle(),
        label = "progress",
    )
    val oneThrow = isOneThrowFromWin(score, target)
    val warmth = ((score - WARM_FROM).toFloat() / (target - MAX_THROW_POINTS - WARM_FROM)).coerceIn(0f, 1f)
    val arcColor = if (score >= target) colors.saffron else lerp(teamColor, colors.saffron, warmth)
    // Quelques pulsations quand la victoire devient possible en un lancer, puis l'anneau reste fixe :
    // une animation sans fin coûterait de la batterie pendant toute la partie. La valeur animée n'est lue
    // que pendant le dessin : l'anneau est redessiné sans être recomposé.
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(oneThrow, reduced) {
        pulse.snapTo(1f)
        if (oneThrow && !reduced) {
            repeat(PULSE_COUNT) {
                pulse.animateTo(PULSE_SCALE, tween(PULSE_MS, easing = FastOutSlowInEasing))
                pulse.animateTo(1f, tween(PULSE_MS, easing = FastOutSlowInEasing))
            }
        }
    }
    val description = stringResource(R.string.ds_score, score, target)
    Box(
        modifier =
            modifier
                .size(size)
                .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val base = strokeWidth.toPx()
            val inset = base * 1.35f / 2
            val arcSize =
                androidx.compose.ui.geometry
                    .Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
            val topLeft =
                androidx.compose.ui.geometry
                    .Offset(inset, inset)
            drawArc(colors.ringTrack, 0f, FULL_CIRCLE, false, topLeft, arcSize, style = Stroke(base))
            if (progress > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = START_ANGLE,
                    sweepAngle = FULL_CIRCLE * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = base * pulse.value, cap = StrokeCap.Round),
                )
            }
        }
        // Le contenu tient dans le disque intérieur de l'anneau.
        Box(Modifier.padding(strokeWidth * 1.6f), contentAlignment = Alignment.Center) { content() }
    }
}
