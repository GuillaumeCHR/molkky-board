package com.yogistudio.molkkyboard.feature.intro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.brand.drawYogiStudioMark
import com.yogistudio.molkkyboard.core.designsystem.sound.Sound
import com.yogistudio.molkkyboard.core.designsystem.sound.rememberSoundEffects
import com.yogistudio.molkkyboard.core.designsystem.theme.BricolageGrotesque
import com.yogistudio.molkkyboard.core.designsystem.theme.Inter
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/** État du chargement de démarrage, mené en parallèle de l'animation (jamais dépendant d'elle). */
enum class IntroLoadState {
    Loading,
    Ready,

    /** Le chargement a échoué : on ouvre quand même le menu, en mode hors ligne. */
    Failed,
    ;

    val done: Boolean get() = this != Loading
}

/**
 * Intro « strike » (≈ 4,5 s) : mise en place des quilles, lancer et lignes de vitesse, image d'impact
 * figée et quilles qui s'envolent intactes, signature Yogi Studio, puis le logotype
 * Molkky Board claque. [onFinished] est appelé quand l'animation
 * ET le chargement sont terminés ; si le chargement traîne, on reste sur la signature avec un indicateur.
 * Un tap passe directement au logotype. Avec « Réduire les animations », simple fondu du logo Yogi Studio.
 *
 * @param logotype logotype Molkky Board ; l'appelant y applique l'élément partagé vers l'accueil.
 * @param short version courte : impact puis logotype (≈ 1,6 s), sans la signature sauf si le chargement traîne.
 * @param frameTimeMs fige l'intro à cet instant (aperçus et captures) au lieu de la jouer.
 * @param sounds réglage « Sons » : le choc de l'impact (muet en silencieux).
 */
@Composable
fun IntroScreen(
    loadState: IntroLoadState,
    onFinished: () -> Unit,
    logotype: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    short: Boolean = false,
    frameTimeMs: Long? = null,
    sounds: Boolean = false,
) {
    val reduced = MolkkyBoardTheme.reducedMotion
    val end = if (reduced) IntroTimeline.REDUCED_END else IntroTimeline.END
    val hold = if (reduced) IntroTimeline.REDUCED_END / 2 else IntroTimeline.SIGNATURE_END
    val startTime = frameTimeMs ?: if (short && !reduced) IntroTimeline.SHORT_START else 0L
    val elapsed = remember { mutableLongStateOf(startTime) }
    val currentLoad by rememberUpdatedState(loadState)
    val currentOnFinished by rememberUpdatedState(onFinished)

    val effects = rememberSoundEffects(sounds && frameTimeMs == null)
    if (frameTimeMs == null) {
        IntroClock(
            elapsed,
            reduced,
            short,
            hold,
            end,
            { currentLoad.done },
            currentOnFinished,
        ) { effects?.play(Sound.IMPACT) }
    }
    val waiting by remember { derivedStateOf { !currentLoad.done && elapsed.longValue >= hold } }
    val skipLabel = stringResource(R.string.intro_skip_hint)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .pointerInput(reduced) {
                    detectTapGestures {
                        // Passer : directement au logotype (ou à la signature tant que le chargement continue).
                        val target =
                            if (currentLoad.done) {
                                maxOf(
                                    elapsed.longValue,
                                    if (reduced) end else IntroTimeline.LOGOTYPE_SHOWN,
                                )
                            } else {
                                hold
                            }
                        elapsed.longValue = maxOf(elapsed.longValue, target)
                    }
                }.semantics { contentDescription = skipLabel },
        contentAlignment = Alignment.Center,
    ) {
        if (reduced) {
            ReducedIntro(elapsed)
        } else {
            StrikeScene(elapsed)
            // Le logotype entre en claquant (ressort), porté par une dernière salve de lignes de vitesse.
            Box(
                Modifier.graphicsLayer {
                    val p = progress(elapsed.longValue, IntroTimeline.LOGOTYPE_IN, IntroTimeline.LOGOTYPE_SHOWN)
                    alpha = (p * LOGOTYPE_FADE_SPEED).coerceAtMost(1f)
                    val slam = LOGOTYPE_START_SCALE + (1f - LOGOTYPE_START_SCALE) * springCurve(p)
                    scaleX = slam
                    scaleY = slam
                },
            ) { logotype(Modifier) }
        }
        if (waiting) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp)
                        .size(28.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Fait avancer le temps de l'intro image par image. On reste sur [hold] tant que le chargement n'est pas
 * fini ; [onFinished] est appelé quand l'animation ET le chargement sont terminés.
 */
@Composable
private fun IntroClock(
    elapsed: MutableLongState,
    reduced: Boolean,
    short: Boolean,
    hold: Long,
    end: Long,
    loaded: () -> Boolean,
    onFinished: () -> Unit,
    onImpact: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val currentLoaded by rememberUpdatedState(loaded)
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(reduced) {
        var last = withFrameMillis { it }
        var finished = false
        while (!finished) {
            withFrameMillis { now ->
                val before = elapsed.longValue
                var next = before + (now - last)
                last = now
                // Version courte : après l'explosion, directement au logotype (ou à la signature si on attend).
                val crossesJump = before < IntroTimeline.SHORT_JUMP && next >= IntroTimeline.SHORT_JUMP
                if (short && !reduced && crossesJump) {
                    next = if (currentLoaded()) IntroTimeline.LOGOTYPE_IN else hold
                }
                // On attend le chargement sur la signature : jamais de menu à moitié prêt.
                if (!currentLoaded() && next > hold) next = maxOf(before, hold)
                if (!reduced && before < IntroTimeline.IMPACT && next >= IntroTimeline.IMPACT) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onImpact()
                }
                elapsed.longValue = next
                finished = next >= end && currentLoaded()
            }
        }
        currentOnFinished()
    }
}

@Composable
private fun ReducedIntro(elapsed: MutableLongState) {
    Canvas(Modifier.size(160.dp)) {
        val t = elapsed.longValue
        val alpha = progress(t, 0, 300) * (1f - progress(t, 700, IntroTimeline.REDUCED_END))
        drawYogiStudioMark(alpha = alpha)
    }
}

@Composable
private fun StrikeScene(elapsed: MutableLongState) {
    val scheme = MaterialTheme.colorScheme
    val brand = MolkkyBoardTheme.colors
    val colors =
        remember(scheme, brand) {
            SceneColors(
                background = scheme.background,
                forest = scheme.primary,
                saffron = brand.saffron,
                coral = brand.coral,
            )
        }
    val ink = scheme.onBackground
    val measurer = rememberTextMeasurer()
    val presents = stringResource(R.string.intro_presents)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val simulation = remember(width, height) { StrikeSimulation(width, height) }
        val painter = remember(simulation) { ScenePainter(simulation) }
        // Numéros des quilles : toujours foncés, sur le bois clair (clair comme sombre).
        val numberStyle =
            TextStyle(
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = with(density) { (simulation.pinWidth * 0.42f).toSp() },
                color = Color(0xFF1B1B18),
            )
        val numbers = remember(simulation, numberStyle) { Array(13) { measurer.measure(it.toString(), numberStyle) } }
        val presentsLayout =
            remember(presents, ink) {
                measurer.measure(
                    presents,
                    TextStyle(
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = with(density) { (14f * simulation.unit).toSp() },
                        color = ink,
                    ),
                )
            }
        Canvas(Modifier.fillMaxSize()) { painter.draw(this, elapsed.longValue, numbers, presentsLayout, colors) }
    }
}

private const val LOGOTYPE_START_SCALE = 1.6f
private const val LOGOTYPE_FADE_SPEED = 3f
