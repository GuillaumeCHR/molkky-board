package com.yogistudio.molkkyboard.feature.intro

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import com.yogistudio.molkkyboard.core.designsystem.brand.drawYogiStudioMark
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

private val PinBody = Color(0xFFE9D3AE)
private val PinTop = Color(0xFFF7E9CF)
private val PinShade = Color(0x73D2B486)
private val MolkkyBody = Color(0xFFD9B98C)
private val MolkkyBand = Color(0xFFB8955E)

/** Couleurs de la scène, issues de la palette « Nordic Bold » du thème (clair ou sombre). */
internal class SceneColors(
    val background: Color,
    val forest: Color,
    val saffron: Color,
    val coral: Color,
)

/**
 * Dessine l'intro « strike » à un instant donné, façon manga mais en aplats « Nordic Bold » (ni trame, ni encrage
 * épais) : lignes de vitesse, image d'impact figée, quilles intactes qui s'envolent avec leur traînée, scintillement
 * au loin, puis la signature Yogi Studio. Les formes sont construites une seule fois (aucune allocation par image).
 */
internal class ScenePainter(
    private val sim: StrikeSimulation,
) {
    private val pose = FloatArray(3)
    private val pinBody =
        Path().apply {
            val w = sim.pinWidth
            val h = sim.pinHeight
            val r = w * 0.22f
            moveTo(-w / 2, -h / 2 + h * 0.14f)
            lineTo(w / 2, -h / 2)
            lineTo(w / 2, h / 2 - r)
            quadraticTo(w / 2, h / 2, w / 2 - r, h / 2)
            lineTo(-w / 2 + r, h / 2)
            quadraticTo(-w / 2, h / 2, -w / 2, h / 2 - r)
            close()
        }
    private val pinBevel =
        Path().apply {
            val w = sim.pinWidth
            val h = sim.pinHeight
            moveTo(-w / 2, -h / 2 + h * 0.14f)
            lineTo(w / 2, -h / 2)
            lineTo(w / 2, -h / 2 + h * 0.09f)
            lineTo(-w / 2, -h / 2 + h * 0.23f)
            close()
        }

    /** Éclat radial de l'image d'impact : étoile à pointes arrondies (courbes, construite une fois). */
    private val burst =
        Path().apply {
            val outer = minOf(sim.width, sim.height) * 0.62f
            val inner = outer * 0.58f
            val step = PI.toFloat() / BURST_POINTS
            for (k in 0 until BURST_POINTS) {
                val valley = 2 * k * step - PI.toFloat() / 2
                val peak = valley + step
                val next = valley + 2 * step
                if (k == 0) moveTo(cos(valley) * inner, sin(valley) * inner)
                // La pointe sert de point de contrôle : elle est arrondie.
                quadraticTo(cos(peak) * outer, sin(peak) * outer, cos(next) * inner, sin(next) * inner)
            }
            close()
        }

    fun draw(
        scope: DrawScope,
        time: Long,
        numbers: Array<TextLayoutResult>,
        presents: TextLayoutResult,
        colors: SceneColors,
    ) = with(scope) {
        val frozen = time in IntroTimeline.IMPACT until IntroTimeline.FREEZE_END
        // Un seul aplat safran, ≈ 100 ms (pas de flash blanc, pas de clignotement : WCAG 2.3.1).
        drawRect(if (frozen) colors.saffron else colors.background)
        val shake = shakeOffset(time)
        // Léger zoom de la « caméra » pendant le lancer, pour l'anticipation.
        val zoom =
            1f + ZOOM * easeInOutCubic(progress(time, IntroTimeline.SETUP_END, IntroTimeline.IMPACT)) *
                (1f - progress(time, IntroTimeline.FREEZE_END, IntroTimeline.FREEZE_END + ZOOM_OUT_MS))
        translate(left = shake) {
            scale(zoom, pivot = Offset(sim.impactX, sim.centerY)) {
                when {
                    time < IntroTimeline.IMPACT -> {
                        drawThrowLines(time, colors)
                        drawPins(time, numbers)
                    }

                    frozen -> {
                        drawImpact(colors)
                    }

                    else -> {
                        drawFlyingPins(time, numbers, colors)
                    }
                }
                if (time in IntroTimeline.SETUP_END..IntroTimeline.STRIKE_END && !frozen) drawMolkky(time)
            }
        }
        if (time >= IntroTimeline.STRIKE_END) drawSignature(time, presents)
        if (time in IntroTimeline.LOGOTYPE_IN..IntroTimeline.LOGOTYPE_SHOWN) drawTitleLines(time, colors)
    }

    /** Tremblement de l'écran à l'impact, amorti en 0,4 s. */
    private fun shakeOffset(time: Long): Float {
        val dt = (time - IntroTimeline.IMPACT) / 1_000f
        if (dt < 0f || dt > SHAKE_SECONDS) return 0f
        return SHAKE_AMPLITUDE * sim.unit * sin(2f * PI.toFloat() * SHAKE_HZ * dt) * exp(-dt / SHAKE_DECAY)
    }

    /** Étape 1 : les quilles apparaissent rangée par rangée, avec un rebond. */
    private fun DrawScope.drawPins(time: Long, numbers: Array<TextLayoutResult>) {
        for (i in 0 until StrikeSimulation.PIN_COUNT) {
            val delay = sim.pinRow[i] * ROW_DELAY
            val scale = springCurve(progress(time, delay, delay + PIN_POP_MS))
            if (scale <= 0f) continue
            drawPin(sim.pinX[i], sim.pinY[i], 0f, scale, numbers[sim.pinNumbers[i]], 1f)
        }
    }

    private fun DrawScope.drawPin(
        x: Float,
        y: Float,
        angle: Float,
        scale: Float,
        label: TextLayoutResult,
        alpha: Float,
    ) {
        translate(x, y) {
            rotate(angle, pivot = Offset.Zero) {
                scale(scale, pivot = Offset.Zero) {
                    drawPath(pinBody, PinBody, alpha = alpha)
                    drawRect(
                        PinShade,
                        topLeft = Offset(sim.pinWidth * 0.32f, -sim.pinHeight / 2),
                        size = Size(sim.pinWidth * 0.18f, sim.pinHeight * 0.85f),
                        alpha = alpha,
                    )
                    drawPath(pinBevel, PinTop, alpha = alpha)
                    drawText(
                        label,
                        topLeft = Offset(-label.size.width / 2f, -label.size.height / 2f + sim.pinHeight * 0.08f),
                        alpha = alpha,
                    )
                }
            }
        }
    }

    /** Étape 2 : lignes de vitesse vert forêt et safran, convergentes vers les quilles, de plus en plus rapides. */
    private fun DrawScope.drawThrowLines(time: Long, colors: SceneColors) {
        val p = progress(time, IntroTimeline.SETUP_END, IntroTimeline.IMPACT)
        if (p <= 0f) return
        drawSpeedLines(sim.impactX, sim.centerY, p * p, colors, outward = false)
    }

    /** Étape 5 : dernière salve de lignes de vitesse, qui jaillissent autour du logotype. */
    private fun DrawScope.drawTitleLines(time: Long, colors: SceneColors) {
        val p = progress(time, IntroTimeline.LOGOTYPE_IN, IntroTimeline.LOGOTYPE_SHOWN)
        drawSpeedLines(size.width / 2, size.height / 2, easeOutCubic(p), colors, outward = true, fade = 1f - p)
    }

    /**
     * Lignes radiales autour de (cx, cy) ; [p] fait avancer chaque ligne (vers le centre, ou vers l'extérieur si
     * [outward]). Couleurs alternées vert forêt / safran.
     */
    private fun DrawScope.drawSpeedLines(
        cx: Float,
        cy: Float,
        p: Float,
        colors: SceneColors,
        outward: Boolean,
        fade: Float = 1f,
    ) {
        val stroke = LINE_STROKE * sim.unit
        for (l in 0 until StrikeSimulation.SPEED_LINES) {
            val local = ((p - sim.lineDelay[l]) / (1f - sim.lineDelay[l])).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val reach = sim.lineReach[l]
            val start = if (outward) reach * LINE_INNER + reach * local else reach * (2f - local)
            val end = start + sim.lineLength[l] * (if (outward) 1f else local)
            val c = sim.lineCos[l]
            val s = sim.lineSin[l]
            drawLine(
                color = if (l % 2 == 0) colors.forest else colors.saffron,
                start = Offset(cx + c * start, cy + s * start),
                end = Offset(cx + c * end, cy + s * end),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
                alpha = LINE_ALPHA * fade,
            )
        }
    }

    /** Étape 3a : image figée. Éclat radial corail sur aplat safran, quilles en silhouettes vert forêt. */
    private fun DrawScope.drawImpact(colors: SceneColors) {
        translate(sim.impactX, sim.centerY) { drawPath(burst, colors.coral.copy(alpha = BURST_ALPHA)) }
        // Silhouettes un peu réduites : elles restent distinctes au lieu de former un bloc.
        for (i in 0 until StrikeSimulation.PIN_COUNT) {
            translate(sim.pinX[i], sim.pinY[i]) {
                scale(SILHOUETTE_SCALE, pivot = Offset.Zero) { drawPath(pinBody, colors.forest) }
            }
        }
        molkkyPose(IntroTimeline.IMPACT)
        drawMolkkyShape(pose[0], pose[1], pose[2], colors.forest, colors.forest)
    }

    /** Étape 3b : les 12 quilles, intactes et numérotées, s'envolent avec une petite traînée ; certaines au loin. */
    private fun DrawScope.drawFlyingPins(time: Long, numbers: Array<TextLayoutResult>, colors: SceneColors) {
        for (i in 0 until StrikeSimulation.PIN_COUNT) {
            val label = numbers[sim.pinNumbers[i]]
            val scale = sim.scale(i, time)
            if (sim.pinFar[i] && scale < SPARKLE_SCALE) {
                drawSparkle(i, time, colors)
                continue
            }
            // Traînée : deux fantômes un peu en arrière dans le temps.
            for (ghost in TRAIL downTo 1) {
                val t = time - ghost * TRAIL_STEP_MS
                if (t < IntroTimeline.FREEZE_END) continue
                drawPin(sim.x(i, t), sim.y(i, t), sim.rotation(i, t), sim.scale(i, t), label, TRAIL_ALPHA / ghost)
            }
            drawPin(sim.x(i, time), sim.y(i, time), sim.rotation(i, time), scale, label, 1f)
        }
    }

    /** Petite étoile safran qui clignote une seule fois là où la quille a disparu au loin. */
    private fun DrawScope.drawSparkle(pin: Int, time: Long, colors: SceneColors) {
        val gone = farGoneTime(pin)
        val p = progress(time, gone, gone + SPARKLE_MS)
        if (p <= 0f || p >= 1f) return
        val size = sim.pinWidth * SPARKLE_SIZE * sin(p * PI.toFloat())
        val x = sim.x(pin, gone)
        val y = sim.y(pin, gone)
        val stroke = SPARKLE_STROKE * sim.unit
        drawLine(colors.saffron, Offset(x - size, y), Offset(x + size, y), stroke, StrokeCap.Round)
        drawLine(colors.saffron, Offset(x, y - size), Offset(x, y + size), stroke, StrokeCap.Round)
    }

    /** Instant où une quille lointaine devient trop petite et scintille (pas de 20 ms, sans allocation). */
    private fun farGoneTime(pin: Int): Long {
        var t = IntroTimeline.FREEZE_END
        while (t < IntroTimeline.STRIKE_END && sim.scale(pin, t) >= SPARKLE_SCALE) t += SPARKLE_SEARCH_STEP
        return t
    }

    /** Le mölkky arrive du bas en tournoyant (traînée), puis retombe et roule hors champ après l'impact. */
    private fun DrawScope.drawMolkky(time: Long) {
        for (ghost in MOLKKY_TRAIL downTo 0) {
            val t = time - ghost * TRAIL_STEP_MS
            if (t < IntroTimeline.SETUP_END) continue
            molkkyPose(t)
            val alpha = if (ghost == 0) 1f else 0.28f * (1f - ghost.toFloat() / (MOLKKY_TRAIL + 1))
            drawMolkkyShape(pose[0], pose[1], pose[2], MolkkyBody.copy(alpha = alpha), MolkkyBand.copy(alpha = alpha))
        }
    }

    private fun DrawScope.drawMolkkyShape(x: Float, y: Float, angle: Float, body: Color, band: Color) {
        val w = sim.pinWidth * 1.15f
        val h = sim.pinWidth * 2.1f
        rotate(angle, pivot = Offset(x, y)) {
            drawRoundRect(body, Offset(x - w / 2, y - h / 2), Size(w, h), CornerRadius(w * 0.3f))
            drawRect(band, Offset(x - w / 2, y - h * 0.1f), Size(w, h * 0.2f))
        }
    }

    /** Écrit dans [pose] la position (x, y) et l'angle du mölkky à l'instant [t]. */
    private fun molkkyPose(t: Long) {
        val startY = sim.height + sim.pinWidth * 2
        if (t <= IntroTimeline.IMPACT) {
            // Accélère vers le point d'impact.
            val p = progress(t, IntroTimeline.SETUP_END, IntroTimeline.IMPACT)
            val eased = p * p
            pose[0] = sim.impactX + (1f - eased) * sim.width * 0.12f
            pose[1] = startY + (sim.impactY - startY) * eased
            pose[2] = p * SPIN_DEGREES
        } else {
            // Retombe puis roule hors champ, vers la droite.
            val p = progress(t, IntroTimeline.FREEZE_END, IntroTimeline.FREEZE_END + MOLKKY_EXIT_MS)
            pose[0] = sim.impactX + p * p * sim.width * 0.7f
            pose[1] = sim.impactY + easeOutCubic(p) * sim.pinWidth * 2.5f
            pose[2] = SPIN_DEGREES + p * ROLL_DEGREES
        }
    }

    /** Étape 4 : le cercle safran se trace, le personnage apparaît avec un rebond, puis « Yogi Studio présente ». */
    private fun DrawScope.drawSignature(time: Long, presents: TextLayoutResult) {
        val fadeOut = 1f - progress(time, IntroTimeline.SIGNATURE_END, IntroTimeline.SIGNATURE_END + SIGNATURE_FADE)
        if (fadeOut <= 0f) return
        val side = minOf(size.width, size.height) * 0.42f
        val topLeft = Offset((size.width - side) / 2, size.height * 0.42f - side / 2)
        val start = IntroTimeline.STRIKE_END
        drawYogiStudioMark(
            ringProgress = easeOutCubic(progress(time, start, start + RING_MS)),
            characterScale =
                springCurve(
                    progress(time, start + CHARACTER_DELAY, start + CHARACTER_DELAY + CHARACTER_MS),
                ),
            alpha = fadeOut,
            side = side,
            topLeft = topLeft,
        )
        val textAlpha = progress(time, start + PRESENTS_DELAY, start + PRESENTS_DELAY + PRESENTS_MS) * fadeOut
        if (textAlpha > 0f) {
            drawText(
                presents,
                topLeft = Offset((size.width - presents.size.width) / 2, topLeft.y + side + 16f * sim.unit),
                alpha = textAlpha,
            )
        }
    }

    private companion object {
        const val ROW_DELAY = 80L
        const val PIN_POP_MS = 420L
        const val TRAIL = 2
        const val TRAIL_STEP_MS = 30L
        const val TRAIL_ALPHA = 0.35f
        const val MOLKKY_TRAIL = 4
        const val MOLKKY_EXIT_MS = 800L
        const val SPIN_DEGREES = 900f
        const val ROLL_DEGREES = 540f
        const val ZOOM = 0.08f
        const val ZOOM_OUT_MS = 300L
        const val SHAKE_SECONDS = 0.4f
        const val SHAKE_AMPLITUDE = 16f
        const val SHAKE_HZ = 22f
        const val SHAKE_DECAY = 0.12f
        const val SILHOUETTE_SCALE = 0.8f
        const val BURST_POINTS = 12
        const val BURST_ALPHA = 0.55f
        const val LINE_STROKE = 3f
        const val LINE_ALPHA = 0.85f
        const val LINE_INNER = 0.6f
        const val SPARKLE_SCALE = 0.15f
        const val SPARKLE_MS = 220L
        const val SPARKLE_SIZE = 0.35f
        const val SPARKLE_STROKE = 4f
        const val SPARKLE_SEARCH_STEP = 20L
        const val RING_MS = 450L
        const val CHARACTER_DELAY = 300L
        const val CHARACTER_MS = 450L
        const val PRESENTS_DELAY = 450L
        const val PRESENTS_MS = 300L
        const val SIGNATURE_FADE = 200L
    }
}
