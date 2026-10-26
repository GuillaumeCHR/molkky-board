package com.yogistudio.molkkyboard.feature.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.component.EliminatedStamp
import com.yogistudio.molkkyboard.core.designsystem.component.ImpactBurst
import com.yogistudio.molkkyboard.core.designsystem.component.MissIndicator
import com.yogistudio.molkkyboard.core.designsystem.theme.MBColors
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Une animation « 12 ! » à jouer ; [key] distingue deux 12 successifs. */
data class TwelveShow(
    val strike: Boolean,
    val key: Int,
)

/** Une élimination à mettre en scène, ou à défaire ([reverse]) après une annulation. */
data class EliminationShow(
    val team: TeamUi,
    val missLimit: Int,
    val reverse: Boolean,
    val key: Int,
)

/**
 * « 12 ! » : version courte du strike de l'intro (≈ 1 s). Image d'impact figée ≈ 60 ms, puis la quille 12
 * (ou les 12 quilles) file hors champ avec ses lignes de vitesse, et la bulle « 12 ! » claque.
 * Ne bloque jamais la saisie : seule la bulle capte un tap (pour passer), le reste laisse passer les touches.
 */
@Composable
fun TwelveOverlay(
    show: TwelveShow?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (show == null) return
    val reduced = MolkkyBoardTheme.reducedMotion
    var elapsed by remember(show) { mutableFloatStateOf(0f) }
    val burstScale = remember(show) { Animatable(if (reduced) 1f else BURST_START_SCALE) }
    LaunchedEffect(show) {
        val start = withFrameNanos { it }
        var now = start
        while (now - start < TWELVE_DURATION_NS) {
            now = withFrameNanos { it }
            elapsed = (now - start) / NANOS_PER_SECOND
        }
        onDone()
    }
    LaunchedEffect(show) {
        if (reduced) return@LaunchedEffect
        delay(FREEZE_MS)
        burstScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        )
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!reduced) FlyingPins(show.strike, elapsed)
        AnimatedVisibility(visible = true, enter = fadeIn()) {
            ImpactBurst(
                text = stringResource(R.string.game_twelve),
                caption = if (show.strike) stringResource(R.string.game_strike) else null,
                modifier =
                    Modifier
                        .scale(burstScale.value)
                        .rotate(BURST_ANGLE)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onDone() },
            )
        }
    }
}

/** Trajectoire précalculée d'une quille : départ (en fraction de la zone), vitesse, rotation, envol vers le fond. */
private class FlyingPin(
    val number: Int,
    val start: Offset,
    val velocity: Offset,
    val spin: Float,
    val far: Boolean,
)

@Composable
private fun FlyingPins(strike: Boolean, elapsed: Float) {
    val colors = MolkkyBoardTheme.colors
    val measurer = rememberTextMeasurer()
    val numberStyle = MolkkyBoardTheme.typography.pinNumber.copy(color = colors.pinNumber)
    val labels = remember(measurer) { (1..TWELVE).associateWith { measurer.measure(it.toString(), numberStyle) } }
    val pins = remember(strike) { flyingPins(strike) }
    Canvas(Modifier.fillMaxSize()) {
        val side = size.minDimension * FORMATION_RATIO
        val origin = Offset((size.width - side) / 2f, (size.height - side) / 2f)
        // Image d'impact figée : un seul éclat safran (pas de flash répété, critère WCAG 2.3.1).
        if (elapsed < FREEZE_S) drawImpactFlash(colors)
        if (elapsed < SPEED_LINES_S) drawSpeedLines(colors, 1f - elapsed / SPEED_LINES_S)
        val t = (elapsed - FREEZE_S).coerceAtLeast(0f)
        pins.forEach { pin ->
            val scale = if (pin.far) (1f - t * FAR_SHRINK).coerceAtLeast(0f) else 1f
            val cx = origin.x + side * (pin.start.x + pin.velocity.x * t)
            val cy = origin.y + side * (pin.start.y + pin.velocity.y * t + GRAVITY * t * t)
            if (scale > SPARKLE_SCALE) {
                drawPin(Offset(cx, cy), side * PIN_WIDTH * scale, side * PIN_HEIGHT * scale, pin.spin * t, colors) {
                    labels.getValue(pin.number).let { label ->
                        drawText(label, topLeft = Offset(-label.size.width / 2f, -label.size.height / 2f))
                    }
                }
            } else if (t < SPARKLE_END_S) {
                // Clin d'œil au manga : la quille disparaît au loin dans un scintillement.
                drawSparkle(Offset(cx, cy), side * SPARKLE_SIZE, colors)
            }
        }
    }
}

private fun DrawScope.drawImpactFlash(colors: MBColors) {
    drawRect(colors.saffron.copy(alpha = FLASH_ALPHA))
}

private fun DrawScope.drawSpeedLines(colors: MBColors, strength: Float) {
    val c = center
    val length = size.maxDimension
    for (i in 0 until SPEED_LINES) {
        val angle = 2 * PI * i / SPEED_LINES
        val inner = size.minDimension * (SPEED_INNER + (i % SPEED_GROUPS) * SPEED_STAGGER)
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        drawLine(
            color = (if (i % 2 == 0) colors.saffron else colors.pinWoodShade).copy(alpha = strength * SPEED_ALPHA),
            start = Offset(c.x + dx * inner, c.y + dy * inner),
            end = Offset(c.x + dx * length, c.y + dy * length),
            strokeWidth = SPEED_STROKE,
        )
    }
}

private inline fun DrawScope.drawPin(
    center: Offset,
    width: Float,
    height: Float,
    angle: Float,
    colors: MBColors,
    crossinline label: DrawScope.() -> Unit,
) {
    translate(center.x, center.y) {
        rotate(angle, pivot = Offset.Zero) {
            drawRoundRect(
                color = colors.pinWood,
                topLeft = Offset(-width / 2f, -height / 2f),
                size = Size(width, height),
                cornerRadius = CornerRadius(width * PIN_CORNER),
            )
            drawRoundRect(
                color = colors.pinWoodTop,
                topLeft = Offset(-width / 2f, -height / 2f),
                size = Size(width, height * PIN_TOP),
                cornerRadius = CornerRadius(width * PIN_CORNER),
            )
            if (width > LABEL_MIN_WIDTH) label()
        }
    }
}

private fun DrawScope.drawSparkle(center: Offset, size: Float, colors: MBColors) {
    drawLine(colors.saffron, Offset(center.x - size, center.y), Offset(center.x + size, center.y), SPARKLE_STROKE)
    drawLine(colors.saffron, Offset(center.x, center.y - size), Offset(center.x, center.y + size), SPARKLE_STROKE)
}

/** Formation officielle (vue du lanceur), en fraction de la zone : mêmes positions que la grille de saisie. */
private val FORMATION =
    mapOf(
        7 to (0.3f to 0.15f),
        9 to (0.5f to 0.15f),
        8 to (0.7f to 0.15f),
        5 to (0.2f to 0.38f),
        11 to (0.4f to 0.38f),
        12 to (0.6f to 0.38f),
        6 to (0.8f to 0.38f),
        3 to (0.3f to 0.61f),
        10 to (0.5f to 0.61f),
        4 to (0.7f to 0.61f),
        1 to (0.4f to 0.84f),
        2 to (0.6f to 0.84f),
    )

/** Trajectoires déterministes (graine fixe) : les captures restent stables. */
private fun flyingPins(strike: Boolean): List<FlyingPin> {
    val random = Random(SEED)
    val numbers = if (strike) FORMATION.keys.toList() else listOf(TWELVE)
    return numbers.map { number ->
        val (x, y) = FORMATION.getValue(number)
        val far = strike && number in FAR_PINS
        // Le mölkky arrive d'en bas : les quilles partent vers le haut et les côtés.
        val jitter = random.nextFloat() - HALF
        val angle = if (far) UP else UP + (x - HALF) * PI * SPREAD + jitter * JITTER
        val speed = if (far) FAR_SPEED else MIN_SPEED + random.nextFloat() * SPEED_RANGE
        FlyingPin(
            number = number,
            start = Offset(x, y),
            velocity = Offset((cos(angle) * speed).toFloat(), (sin(angle) * speed).toFloat()),
            spin = (random.nextFloat() - HALF) * MAX_SPIN,
            far = far,
        )
    }
}

/**
 * Élimination au 3e raté (≈ 1,8 s, passable d'un tap) : les points de ratés claquent, la carte tremble, la
 * quille-avatar bascule et tombe, puis le tampon « Éliminé » se pose avec un rebond. À l'envers
 * ([EliminationShow.reverse]), le tampon se décolle et l'équipe revient en jeu.
 * « Réduire les animations » : carte grisée et tampon en fondu.
 */
@Composable
fun EliminationOverlay(
    show: EliminationShow?,
    onDone: () -> Unit,
) {
    if (show == null) return
    val reduced = MolkkyBoardTheme.reducedMotion
    var why by remember(show) { mutableStateOf(false) }
    val shake = remember(show) { Animatable(0f) }
    val fall = remember(show) { Animatable(if (show.reverse) 1f else 0f) }
    val stamp = remember(show) { Animatable(if (show.reverse) 1f else 0f) }
    LaunchedEffect(show, why) {
        if (why) return@LaunchedEffect
        playElimination(show.reverse, reduced, shake, fall, stamp)
        onDone()
    }
    val colors = MolkkyBoardTheme.colors.teams
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = SCRIM_ALPHA))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            modifier = Modifier.padding(32.dp).offset { IntOffset(shake.value.dp.roundToPx(), 0) },
        ) {
            Box(contentAlignment = Alignment.Center) {
                // « Réduire les animations » : la carte se grise sous le tampon, sans mouvement.
                Column(
                    Modifier
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                        .alpha(if (reduced && !show.reverse) 1f - stamp.value * REDUCED_GREY else 1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val color = colors[show.team.colorIndex % colors.size]
                    Box(
                        Modifier
                            .size(64.dp)
                            .offset { IntOffset(0, (fall.value * FALL_DISTANCE).dp.roundToPx()) }
                            .rotate(fall.value * FALL_ANGLE)
                            .background(color.fill.copy(alpha = 1f - fall.value * FALL_FADE), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            show.team.name
                                .take(1)
                                .uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = color.onFill,
                        )
                    }
                    Text(show.team.name, style = MaterialTheme.typography.headlineMedium, maxLines = 2)
                    MissIndicator(
                        if (show.reverse) show.missLimit - 1 else show.missLimit,
                        limit = show.missLimit,
                        dotSize = 18.dp,
                    )
                    Text(
                        if (show.reverse) {
                            stringResource(R.string.game_elimination_cancelled, show.team.name)
                        } else {
                            stringResource(R.string.game_eliminated, show.team.name)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!show.reverse) {
                        TextButton(onClick = { why = true }) { Text(stringResource(R.string.game_eliminated_why)) }
                    }
                }
                EliminatedStamp(
                    stringResource(R.string.game_eliminated_stamp),
                    modifier =
                        Modifier
                            .scale(STAMP_START_SCALE - (STAMP_START_SCALE - 1f) * stamp.value)
                            .alpha(stamp.value.coerceIn(0f, 1f)),
                )
            }
        }
    }
    if (why) {
        AlertDialog(
            onDismissRequest = { why = false },
            confirmButton = { TextButton(onClick = { why = false }) { Text(stringResource(R.string.game_ok)) } },
            title = { Text(stringResource(R.string.game_eliminated_why)) },
            text = { Text(stringResource(R.string.game_eliminated_rule, show.missLimit)) },
        )
    }
}

/** Séquence de l'élimination (ou de son annulation), jouée sur les trois valeurs animées de la carte. */
private suspend fun playElimination(
    reverse: Boolean,
    reduced: Boolean,
    shake: Animatable<Float, *>,
    fall: Animatable<Float, *>,
    stamp: Animatable<Float, *>,
) {
    when {
        reverse -> {
            stamp.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
            fall.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            delay(REVERSE_HOLD_MS)
        }

        reduced -> {
            stamp.animateTo(1f, tween(REDUCED_FADE_MS))
            delay(ELIMINATION_HOLD_MS)
        }

        stamp.value < 1f -> {
            delay(DOTS_MS)
            SHAKE_STEPS.forEach {
                shake.animateTo(
                    it,
                    spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh),
                )
            }
            fall.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow))
            stamp.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            )
            delay(ELIMINATION_HOLD_MS)
        }

        else -> {
            delay(ELIMINATION_HOLD_MS)
        }
    }
}

private val SHAKE_STEPS = listOf(-14f, 12f, -9f, 6f, 0f)
private const val TWELVE = 12
private const val SEED = 12
private const val HALF = 0.5f
private const val UP = -PI / 2
private const val SPREAD = 1.1
private const val JITTER = 0.5
private const val SPEED_GROUPS = 3
private val FAR_PINS = setOf(9, 11)
private const val NANOS_PER_SECOND = 1_000_000_000f
private const val TWELVE_DURATION_NS = 1_000_000_000L
private const val FREEZE_MS = 60L
private const val FREEZE_S = 0.06f
private const val SPEED_LINES_S = 0.45f
private const val SPEED_LINES = 20
private const val SPEED_INNER = 0.22f
private const val SPEED_STAGGER = 0.05f
private const val SPEED_ALPHA = 0.8f
private const val SPEED_STROKE = 6f
private const val FLASH_ALPHA = 0.85f
private const val FORMATION_RATIO = 0.8f
private const val PIN_WIDTH = 0.1f
private const val PIN_HEIGHT = 0.17f
private const val PIN_CORNER = 0.35f
private const val PIN_TOP = 0.22f
private const val LABEL_MIN_WIDTH = 40f
private const val GRAVITY = 1.4f
private const val MIN_SPEED = 1.6f
private const val SPEED_RANGE = 0.9f
private const val FAR_SPEED = 0.35f
private const val FAR_SHRINK = 1.4f
private const val SPARKLE_SCALE = 0.15f
private const val SPARKLE_END_S = 0.85f
private const val SPARKLE_SIZE = 0.035f
private const val SPARKLE_STROKE = 5f
private const val MAX_SPIN = 900f
private const val BURST_START_SCALE = 0.2f
private const val BURST_ANGLE = -6f
private const val DOTS_MS = 250L
private const val ELIMINATION_HOLD_MS = 700L
private const val REVERSE_HOLD_MS = 500L
private const val REDUCED_FADE_MS = 300
private const val SCRIM_ALPHA = 0.7f
private const val FALL_DISTANCE = 120f
private const val FALL_ANGLE = 70f
private const val FALL_FADE = 1f
private const val STAMP_START_SCALE = 2f
private const val REDUCED_GREY = 0.45f
