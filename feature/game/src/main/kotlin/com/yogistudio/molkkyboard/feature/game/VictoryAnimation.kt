package com.yogistudio.molkkyboard.feature.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/** Une victoire à célébrer ; [key] distingue deux victoires successives (après une correction). */
data class VictoryShow(
    val name: String,
    val colorIndex: Int,
    val target: Int,
    val newRecord: Boolean,
    val perfect: Boolean,
    val key: Int,
)

/**
 * Victoire (≈ 2,6 s, passable d'un tap) : le score cible « explose » dans la couleur de l'équipe, une pluie de
 * confettis en forme de petites quilles tombe, puis le nom du vainqueur apparaît avec un trophée.
 * « Réduire les animations » : simple fondu du trophée et du nom.
 */
@Composable
fun VictoryOverlay(show: VictoryShow?, onDone: () -> Unit, modifier: Modifier = Modifier) {
    if (show == null) return
    val reduced = MolkkyBoardTheme.reducedMotion
    val teamColors = MolkkyBoardTheme.colors.teams
    val team = teamColors[show.colorIndex % teamColors.size]
    val colors = MolkkyBoardTheme.colors
    var elapsed by remember(show) { mutableFloatStateOf(0f) }
    LaunchedEffect(show) {
        val duration = if (reduced) REDUCED_DURATION_S else DURATION_S
        val start = withFrameNanos { it }
        var now = start
        while ((now - start) / NANOS_PER_SECOND < duration) {
            now = withFrameNanos { it }
            elapsed = (now - start) / NANOS_PER_SECOND
        }
        onDone()
    }
    // Physique de ressort pour le « 50 » et le nom ; le temps écoulé pilote seulement les fondus et la pluie.
    val burstScale = remember(show) { Animatable(BURST_MIN) }
    val nameScale = remember(show) { Animatable(if (reduced) 1f else NAME_MIN) }
    LaunchedEffect(show) {
        if (reduced) return@LaunchedEffect
        launch { burstScale.animateTo(BURST_MAX, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
        delay((NAME_START_S * MILLIS).toLong())
        nameScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }
    val confetti = remember { confetti() }
    val palette = remember(team, colors) { listOf(team.fill, colors.saffron, colors.coral, team.fill, colors.pinWood) }
    Box(
        modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDone,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(Modifier.fillMaxSize().alpha(SCRIM_ALPHA), color = MaterialTheme.colorScheme.background) {}
        if (!reduced) {
            Canvas(Modifier.fillMaxSize()) { drawConfetti(confetti, palette, elapsed - CONFETTI_START_S) }
            // 1. Le « 50 » explose : il grossit avec un rebond puis s'efface.
            val burst = (elapsed / BURST_S).coerceIn(0f, 1f)
            if (burst < 1f) {
                Text(
                    show.target.toString(),
                    style = MolkkyBoardTheme.typography.scoreHero.copy(fontSize = BURST_FONT),
                    color = team.fill,
                    modifier = Modifier.scale(burstScale.value).alpha(1f - burst * burst),
                )
            }
        }
        // 3. Le nom du vainqueur et le trophée.
        val reveal =
            if (reduced) {
                (elapsed / REDUCED_FADE_S).coerceIn(
                    0f,
                    1f,
                )
            } else {
                ((elapsed - NAME_START_S) / NAME_S).coerceIn(0f, 1f)
            }
        if (reveal > 0f) {
            Column(
                Modifier
                    .padding(24.dp)
                    .alpha(reveal)
                    .scale(nameScale.value)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(MBIcons.Trophy, contentDescription = null, tint = colors.saffron, modifier = Modifier.size(96.dp))
                Text(
                    stringResource(R.string.victory_title, show.name),
                    style = MaterialTheme.typography.displaySmall,
                    color = team.fill,
                    textAlign = TextAlign.Center,
                )
                if (show.perfect) VictoryBanner(stringResource(R.string.victory_perfect))
                if (show.newRecord) VictoryBanner(stringResource(R.string.victory_new_record))
            }
        }
    }
}

@Composable
private fun VictoryBanner(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MolkkyBoardTheme.colors.saffron,
        contentColor = MolkkyBoardTheme.colors.onSaffron,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

/** Un confetti en forme de petite quille : départ en haut, dérive, vitesse de chute et rotation propres. */
private class Confetto(
    val x: Float,
    val speed: Float,
    val drift: Float,
    val spin: Float,
    val scale: Float,
    val color: Int,
)

/** Pluie déterministe (graine fixe) : les captures restent stables. */
private fun confetti(): List<Confetto> {
    val random = Random(SEED)
    return List(CONFETTI_COUNT) {
        Confetto(
            x = random.nextFloat(),
            speed = MIN_FALL + random.nextFloat() * FALL_RANGE,
            drift = (random.nextFloat() - HALF) * MAX_DRIFT,
            spin = (random.nextFloat() - HALF) * MAX_SPIN,
            scale = MIN_SCALE + random.nextFloat() * SCALE_RANGE,
            color = random.nextInt(PALETTE_SIZE),
        )
    }
}

private fun DrawScope.drawConfetti(confetti: List<Confetto>, palette: List<Color>, time: Float) {
    if (time <= 0f) return
    val w = size.minDimension * PIN_W
    val h = size.minDimension * PIN_H
    confetti.forEachIndexed { index, c ->
        // Départs échelonnés par petits groupes : la pluie arrive en vagues.
        val t = time - (index % STAGGER_GROUPS) * CONFETTI_STAGGER_S / STAGGER_GROUPS
        if (t <= 0f) return@forEachIndexed
        val y = -h + size.height * c.speed * t
        if (y > size.height + h) return@forEachIndexed
        val x = size.width * c.x + size.width * c.drift * sin(t * DRIFT_FREQ)
        rotate(c.spin * t, Offset(x, y)) {
            val pw = w * c.scale
            val ph = h * c.scale
            val color = palette[c.color % palette.size]
            drawRoundRect(color, Offset(x - pw / 2, y - ph / 2), Size(pw, ph), CornerRadius(pw * CORNER))
            // Extrémité biseautée plus claire, comme les quilles de la grille.
            drawRoundRect(
                Color.White.copy(alpha = TOP_ALPHA),
                Offset(x - pw / 2, y - ph / 2),
                Size(pw, ph * TOP),
                CornerRadius(pw * CORNER),
            )
        }
    }
}

private const val NANOS_PER_SECOND = 1_000_000_000f
private const val DURATION_S = 2.6f
private const val REDUCED_DURATION_S = 1.2f
private const val REDUCED_FADE_S = 0.4f
private const val BURST_S = 0.9f
private const val BURST_MIN = 0.3f
private const val BURST_MAX = 1.3f
private const val MILLIS = 1_000f
private val BURST_FONT = 180.sp
private const val CONFETTI_START_S = 0.25f
private const val NAME_START_S = 0.8f
private const val NAME_S = 0.5f
private const val NAME_MIN = 0.6f
private const val SCRIM_ALPHA = 0.9f
private const val SEED = 50
private const val CONFETTI_COUNT = 48
private const val CONFETTI_STAGGER_S = 0.9f
private const val STAGGER_GROUPS = 7
private const val MIN_FALL = 0.45f
private const val FALL_RANGE = 0.35f
private const val MAX_DRIFT = 0.08f
private const val DRIFT_FREQ = 3f
private const val MAX_SPIN = 540f
private const val MIN_SCALE = 0.7f
private const val SCALE_RANGE = 0.6f
private const val HALF = 0.5f
private const val PALETTE_SIZE = 5
private const val PIN_W = 0.035f
private const val PIN_H = 0.07f
private const val CORNER = 0.35f
private const val TOP = 0.25f
private const val TOP_ALPHA = 0.35f
