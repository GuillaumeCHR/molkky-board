package com.yogistudio.molkkyboard.feature.intro

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

private val WOOD =
    arrayOf(
        Color(0xFFE9D3AE),
        Color(0xFFD9B98C),
        Color(0xFFCFAE7E),
        Color(0xFFF4EBDD),
        Color(0xFFE2C99E),
    )
private val PinBody = Color(0xFFE9D3AE)
private val PinTop = Color(0xFFF7E9CF)
private val PinShade = Color(0x73D2B486)
private val MolkkyBody = Color(0xFFD9B98C)
private val MolkkyBand = Color(0xFFB8955E)

/**
 * Dessine la scène de l'intro à un instant donné. Les formes sont construites une seule fois ; pendant
 * l'animation, seul un [Path] réutilisé est réinitialisé pour les éclats (aucune allocation par image).
 */
internal class ScenePainter(
    private val sim: ShatterSimulation,
) {
    private val shard = Path()
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

    fun draw(
        scope: DrawScope,
        time: Long,
        numbers: Array<TextLayoutResult>,
        presents: TextLayoutResult,
        background: Color,
    ) = with(scope) {
        drawRect(background)
        val shake = shakeOffset(time)
        translate(left = shake) {
            if (time < IntroTimeline.IMPACT) drawPins(time, numbers) else drawShards(time, numbers)
            if (time in IntroTimeline.SETUP_END..IntroTimeline.IMPACT + MOLKKY_FADE) drawMolkky(time)
        }
        if (time >= IntroTimeline.SHATTER_END) drawSignature(time, presents)
    }

    /** Petit tremblement de l'écran à l'impact, amorti en 0,4 s. */
    private fun shakeOffset(time: Long): Float {
        val dt = (time - IntroTimeline.IMPACT) / 1_000f
        if (dt < 0f || dt > SHAKE_SECONDS) return 0f
        return SHAKE_AMPLITUDE * sim.unit * sin(2f * PI.toFloat() * SHAKE_HZ * dt) * exp(-dt / SHAKE_DECAY)
    }

    /** Étape 1 : les quilles apparaissent rangée par rangée, avec un rebond. */
    private fun DrawScope.drawPins(time: Long, numbers: Array<TextLayoutResult>) {
        for (i in 0 until ShatterSimulation.PIN_COUNT) {
            val delay = sim.pinRow[i] * ROW_DELAY
            val scale = springCurve(progress(time, delay, delay + PIN_POP_MS))
            if (scale <= 0f) continue
            translate(sim.pinX[i], sim.pinY[i]) {
                scale(scale, pivot = Offset(0f, sim.pinHeight / 2)) {
                    drawPath(pinBody, PinBody)
                    drawRect(
                        PinShade,
                        topLeft = Offset(sim.pinWidth * 0.32f, -sim.pinHeight / 2),
                        size =
                            Size(sim.pinWidth * 0.18f, sim.pinHeight * 0.85f),
                    )
                    drawPath(pinBevel, PinTop)
                    val label = numbers[sim.pinNumbers[i]]
                    drawText(
                        label,
                        topLeft =
                            Offset(
                                -label.size.width / 2f,
                                -label.size.height / 2f + sim.pinHeight * 0.08f,
                            ),
                    )
                }
            }
        }
    }

    /** Étape 2 : le mölkky arrive du bas en tournoyant, avec une traînée ; il rebondit et s'efface après l'impact. */
    private fun DrawScope.drawMolkky(time: Long) {
        for (ghost in TRAIL downTo 0) {
            val t = time - ghost * TRAIL_STEP_MS
            if (t < IntroTimeline.SETUP_END) continue
            molkkyPose(t)
            val x = pose[0]
            val y = pose[1]
            val angle = pose[2]
            val fade =
                if (time >
                    IntroTimeline.IMPACT
                ) {
                    1f - progress(time, IntroTimeline.IMPACT, IntroTimeline.IMPACT + MOLKKY_FADE)
                } else {
                    1f
                }
            val alpha = fade * if (ghost == 0) 1f else 0.28f * (1f - ghost.toFloat() / (TRAIL + 1))
            val w = sim.pinWidth * 1.15f
            val h = sim.pinWidth * 2.1f
            rotate(angle, pivot = Offset(x, y)) {
                drawRoundRect(
                    MolkkyBody.copy(alpha = alpha),
                    Offset(x - w / 2, y - h / 2),
                    Size(w, h),
                    CornerRadius(w * 0.3f),
                )
                drawRect(MolkkyBand.copy(alpha = alpha), Offset(x - w / 2, y - h * 0.1f), Size(w, h * 0.2f))
            }
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
            val p = progress(t, IntroTimeline.IMPACT, IntroTimeline.IMPACT + MOLKKY_FADE)
            pose[0] = sim.impactX - p * sim.width * 0.08f
            pose[1] = sim.impactY + p * sim.pinWidth * 1.5f
            pose[2] = SPIN_DEGREES + p * 120f
        }
    }

    /** Étapes 3 à 5 : éclats précalculés ; un sur quatre rejoint ensuite le logotype. */
    private fun DrawScope.drawShards(time: Long, numbers: Array<TextLayoutResult>) {
        val assemble = easeInOutCubic(progress(time, IntroTimeline.SIGNATURE_END, IntroTimeline.LOGOTYPE_IN + 100))
        for (s in 0 until sim.shardCount) {
            val keeps = sim.assembles(s)
            val alpha =
                if (keeps) {
                    1f - progress(time, IntroTimeline.LOGOTYPE_IN + 150, IntroTimeline.LOGOTYPE_SHOWN)
                } else {
                    1f - progress(time, IntroTimeline.SHATTER_END, IntroTimeline.SIGNATURE_END)
                }
            if (alpha <= 0f) continue
            var x = sim.x(s, time)
            var y = sim.y(s, time)
            var angle = sim.rotation(s, time)
            if (keeps && assemble > 0f) {
                x += (sim.targetX[s] - x) * assemble
                y += (sim.targetY[s] - y) * assemble
                angle *= 1f - assemble
            }
            buildShard(s, x, y, angle)
            drawPath(shard, WOOD[sim.shardColor[s]].copy(alpha = alpha))
            // Le premier éclat de chaque quille emporte son numéro.
            if (s % ShatterSimulation.SHARDS_PER_PIN == 0) {
                val label = numbers[sim.pinNumbers[sim.shardPin[s]]]
                val fade = alpha * (1f - progress(time, IntroTimeline.SHATTER_END, IntroTimeline.SIGNATURE_END))
                if (fade > 0f) {
                    rotate(angle, pivot = Offset(x, y)) {
                        drawText(
                            label,
                            topLeft = Offset(x - label.size.width / 2f, y - label.size.height / 2f),
                            alpha = fade,
                        )
                    }
                }
            }
        }
    }

    private fun buildShard(s: Int, x: Float, y: Float, angle: Float) {
        val radians = angle * PI.toFloat() / 180f
        val c = cos(radians)
        val sn = sin(radians)
        shard.reset()
        for (v in 0 until ShatterSimulation.VERTICES) {
            val i = (s * ShatterSimulation.VERTICES + v) * 2
            val vx = sim.shardShape[i]
            val vy = sim.shardShape[i + 1]
            val px = x + vx * c - vy * sn
            val py = y + vx * sn + vy * c
            if (v == 0) shard.moveTo(px, py) else shard.lineTo(px, py)
        }
        shard.close()
    }

    /** Étape 4 : le cercle safran se trace, le personnage apparaît avec un rebond, puis « Yogi Studio présente ». */
    private fun DrawScope.drawSignature(time: Long, presents: TextLayoutResult) {
        val fadeOut = 1f - progress(time, IntroTimeline.SIGNATURE_END, IntroTimeline.SIGNATURE_END + SIGNATURE_FADE)
        if (fadeOut <= 0f) return
        val side = minOf(size.width, size.height) * 0.42f
        val topLeft = Offset((size.width - side) / 2, size.height * 0.42f - side / 2)
        drawYogiStudioMark(
            ringProgress = easeOutCubic(progress(time, IntroTimeline.SHATTER_END, IntroTimeline.SHATTER_END + RING_MS)),
            characterScale = springCurve(progress(time, CHARACTER_START, CHARACTER_START + CHARACTER_MS)),
            alpha = fadeOut,
            side = side,
            topLeft = topLeft,
        )
        val textAlpha = progress(time, PRESENTS_START, PRESENTS_START + 300) * fadeOut
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
        const val TRAIL = 5
        const val TRAIL_STEP_MS = 28L
        const val MOLKKY_FADE = 300L
        const val SPIN_DEGREES = 900f
        const val SHAKE_SECONDS = 0.4f
        const val SHAKE_AMPLITUDE = 10f
        const val SHAKE_HZ = 22f
        const val SHAKE_DECAY = 0.12f
        const val RING_MS = 500L
        const val CHARACTER_START = 3_000L
        const val CHARACTER_MS = 450L
        const val PRESENTS_START = 3_100L
        const val SIGNATURE_FADE = 200L
    }
}
