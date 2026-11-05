package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.theme.MBColors
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.designsystem.theme.NordicPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Ce que raconte un point : chute (point corail), élimination (croix corail), départ, victoire (safran). */
enum class CurveDotMark { NONE, RESET, ELIMINATED, LEFT, WIN }

/** Score [score] à la fin du tour [round] (tour 0 : départ). Les valeurs sont toujours exactes. */
data class CurveDot(
    val round: Int,
    val score: Int,
    val mark: CurveDotMark = CurveDotMark.NONE,
)

/**
 * Une ligne de la courbe : couleur et emblème de l'équipe (dessiné en bout de ligne : la couleur n'est jamais le seul
 * repère ; sinon l'initiale [initial]), ses points, et [winner] pour le tracé plus épais et le point safran.
 */
data class GameCurveLine(
    val color: Color,
    val emblem: ImageVector?,
    val initial: String,
    val dots: List<CurveDot>,
    val winner: Boolean = false,
)

/** Moment fort sur lequel la caméra zoome brièvement : la ligne [line] au tour [round]. */
data class GameCurveHighlight(
    val line: Int,
    val round: Int,
)

/** Chronologie de l'animation (3 s maximum), en millisecondes. */
object GameCurveTimeline {
    const val DRAW_END = 1_550f
    const val ZOOM_END = 1_950f
    const val FREEZE_END = 2_030f
    const val BURST_END = 2_450f
    const val END = 3_000f

    /** Image d'impact figée (≈ 80 ms) : la vibration et le son éventuel partent à son début. */
    const val IMPACT = ZOOM_END
}

/**
 * Courbe de la partie (spécification « Courbe de la partie ») : la course vers [target], tour par tour, en mise en
 * scène stylisée — traits épais et arrondis, courbes souples, chutes légèrement exagérées, aucune grille, une ligne
 * de repère safran sur l'objectif. Les points restent à leur valeur exacte.
 *
 * [timeMs] pilote l'animation ([GameCurveTimeline]), lu au dessin seulement (aucune recomposition par image) : tracé
 * de gauche à droite avec des lignes de vitesse derrière la pointe et une caméra qui suit la course, bref zoom sur
 * [highlight], zoom final sur le point gagnant, image d'impact
 * figée sur un aplat safran (un seul flash, jamais de blanc), éclat radial et quilles-confettis aux couleurs du
 * vainqueur, puis dézoom vers la vue complète, qui reste affichée. Sans vainqueur, la courbe se trace puis se dézoome.
 * Graine fixe : rendu déterministe pour les captures. [selectedRound] trace un repère vertical sur ce tour ;
 * [onTap] reçoit le tour le plus proche du toucher ; [roundsLabel] (« 8 tours ») s'affiche en bas à droite.
 */
@Composable
fun GameCurve(
    lines: List<GameCurveLine>,
    rounds: Int,
    target: Int,
    timeMs: () -> Float,
    modifier: Modifier = Modifier,
    highlight: GameCurveHighlight? = null,
    selectedRound: Int? = null,
    onTap: ((round: Int) -> Unit)? = null,
    roundsLabel: String? = null,
    height: Dp = 260.dp,
) {
    val colors = MolkkyBoardTheme.colors
    val style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val initialStyle = MaterialTheme.typography.labelLarge.copy(color = Color.White)
    val measurer = rememberTextMeasurer()
    val glyphs = remember(lines) { lines.map { line -> line.emblem?.let(::glyphOf) } }
    val assets = remember { CurveAssets() }
    val labelWidth =
        remember(target, style) {
            measurer
                .measure(target.toString(), style)
                .size.width
                .toFloat()
        }
    // Le geste garde la dernière version du rappel (l'état « animation finie » change après le premier tap possible).
    val currentOnTap by rememberUpdatedState(onTap)
    val tap =
        if (onTap == null) {
            Modifier
        } else {
            Modifier.pointerInput(rounds) {
                detectTapGestures { offset ->
                    val frame = Frame(size.width.toFloat(), size.height.toFloat(), rounds, target, labelWidth, density)
                    currentOnTap?.invoke(frame.roundAt(offset.x))
                }
            }
        }
    // Le temps n'est lu qu'au dessin : l'animation ne recompose rien, elle redessine seulement le Canvas.
    val painter =
        remember(lines, rounds, target, highlight, selectedRound, roundsLabel, colors, style, labelWidth) {
            CurvePainter(
                CurveData(lines, rounds, target, highlight, selectedRound, roundsLabel),
                CurveLook(colors, style, initialStyle, measurer, glyphs, assets),
                labelWidth,
            )
        }
    Canvas(modifier.fillMaxWidth().height(height).then(tap)) { with(painter) { paint(timeMs()) } }
}

/** Ce que raconte la courbe. */
private class CurveData(
    val lines: List<GameCurveLine>,
    val rounds: Int,
    val target: Int,
    val highlight: GameCurveHighlight?,
    val selectedRound: Int?,
    val roundsLabel: String?,
)

/** Objets créés une seule fois (gardés d'une image à l'autre) : confettis à graine fixe, effets de tracé. */
private class CurveAssets {
    val confetti = Confetti.create()
    val dash = PathEffect.dashPathEffect(floatArrayOf(22f, 14f))
    val rounded = PathEffect.cornerPathEffect(18f)
}

/** Son apparence, préparée pendant la composition. */
private class CurveLook(
    val colors: MBColors,
    val style: TextStyle,
    val initialStyle: TextStyle,
    val measurer: TextMeasurer,
    val glyphs: List<Glyph?>,
    val assets: CurveAssets,
)

/** Dessine une image de la courbe à un instant de l'animation. */
private class CurvePainter(
    data: CurveData,
    look: CurveLook,
    /** Largeur du libellé de la cible, la même que pour situer un toucher. */
    private val labelWidth: Float,
) {
    private val lines = data.lines
    private val rounds = data.rounds
    private val target = data.target
    private val highlight = data.highlight
    private val selectedRound = data.selectedRound
    private val roundsLabel = data.roundsLabel
    private val colors = look.colors
    private val style = look.style
    private val initialStyle = look.initialStyle
    private val measurer = look.measurer
    private val glyphs = look.glyphs
    private val confetti = look.assets.confetti
    private val dash = look.assets.dash
    private val rounded = look.assets.rounded

    private val winnerLine = lines.indexOfFirst { it.winner }.takeIf { it >= 0 }

    fun DrawScope.paint(timeMs: Float) {
        val frame = Frame(size.width, size.height, rounds, target, labelWidth, density)
        val winnerDot = winnerLine?.let { lines[it].dots.lastOrNull() }?.let(frame::at)
        val camera = camera(timeMs, frame, size, lines, highlight, winnerDot)
        val freeze = winnerDot != null && timeMs >= GameCurveTimeline.ZOOM_END && timeMs < GameCurveTimeline.FREEZE_END
        if (freeze) drawRoundRect(colors.saffron, cornerRadius = CornerRadius(24.dp.toPx()))
        val reveal = frame.left + frame.width * drawProgress(timeMs)
        // La caméra place son point de mire au centre de la vue, sans jamais montrer l'extérieur du graphique.
        translate(size.width / 2f - camera.pivot.x, size.height / 2f - camera.pivot.y) {
            scale(camera.scale, camera.pivot) {
                drawTarget(frame, target, style, measurer, if (freeze) NordicPalette.Forest else colors.saffron, dash)
                if (timeMs >= GameCurveTimeline.END) selectedRound?.let { drawRoundGuide(frame, it) }
                clipRect(right = reveal + 1f) {
                    lines.sortedBy { it.winner }.forEach { line ->
                        drawLine(frame, line, if (freeze) NordicPalette.Forest else line.color)
                    }
                }
                drawDecorations(frame, reveal, timeMs)
                if (winnerDot != null && winnerLine != null) {
                    drawBurst(
                        timeMs,
                        winnerDot,
                        lines[winnerLine].color,
                        colors.saffron,
                        colors.coral,
                        confetti,
                        rounded,
                    )
                }
            }
        }
        if (rounds > 0) {
            val text = measurer.measure(roundsLabel ?: rounds.toString(), style)
            drawText(text, topLeft = Offset(frame.right - text.size.width, frame.bottom + 4.dp.toPx()))
        }
    }

    private fun DrawScope.drawRoundGuide(frame: Frame, round: Int) {
        val x = frame.x(round.toFloat())
        drawLine(colors.saffron.copy(alpha = 0.6f), Offset(x, frame.top), Offset(x, frame.bottom), 3.dp.toPx())
    }

    /** Marques déjà atteintes par le tracé, pointes avec leurs lignes de vitesse, emblèmes en bout de ligne. */
    private fun DrawScope.drawDecorations(frame: Frame, reveal: Float, timeMs: Float) {
        val emblems = emblemCenters(frame, lines, 14.dp.toPx(), 4.dp.toPx())
        lines.forEachIndexed { i, line ->
            line.dots.forEach { dot ->
                val at = frame.at(dot)
                if (at.x <= reveal + 1f) drawMark(dot, at, colors.coral, colors.saffron, line, selectedRound)
            }
            val head = frame.head(line, reveal)
            if (head != null && timeMs < GameCurveTimeline.DRAW_END) drawSpeedLines(head, line.color)
            val end = line.dots.lastOrNull()?.let(frame::at)
            val center = emblems[i]
            if (end != null && center != null && end.x <= reveal + 1f) {
                drawEmblem(center, line, glyphs[i], measurer, initialStyle)
            }
        }
    }
}

/** Avancement du tracé (0 à 1), en ressort amorti sur la phase de tracé. */
private fun drawProgress(
    timeMs: Float,
): Float = FastOutSlowInEasing.transform((timeMs / GameCurveTimeline.DRAW_END).coerceIn(0f, 1f))

private data class Camera(
    val scale: Float,
    val pivot: Offset,
)

/**
 * Caméra : pendant le tracé, léger zoom qui suit la pointe (avec un sursaut sur le moment fort), puis zoom sur le point
 * gagnant, maintenu pendant l'impact et l'explosion, et dézoom final vers la vue complète.
 */
private fun camera(
    timeMs: Float,
    frame: Frame,
    size: Size,
    lines: List<GameCurveLine>,
    highlight: GameCurveHighlight?,
    winner: Offset?,
): Camera = rawCamera(timeMs, frame, size, lines, highlight, winner).bounded(size)

/** Point de mire ramené là où la vue zoomée reste entièrement dans le graphique. */
private fun Camera.bounded(size: Size): Camera {
    val halfW = size.width / (2f * scale)
    val halfH = size.height / (2f * scale)
    return copy(
        pivot = Offset(pivot.x.coerceIn(halfW, size.width - halfW), pivot.y.coerceIn(halfH, size.height - halfH)),
    )
}

private fun rawCamera(
    timeMs: Float,
    frame: Frame,
    size: Size,
    lines: List<GameCurveLine>,
    highlight: GameCurveHighlight?,
    winner: Offset?,
): Camera {
    val center = Offset(size.width / 2f, size.height / 2f)
    if (timeMs >= GameCurveTimeline.END) return Camera(1f, center)
    val reveal = frame.left + frame.width * drawProgress(timeMs)
    val leader = lines.mapNotNull { frame.head(it, reveal) }.minByOrNull { it.y } ?: center
    val follow = Camera(FOLLOW_SCALE, Offset(reveal, leader.y))
    val drawing =
        highlightBump(timeMs, frame, highlight)?.let { (bump, at) ->
            Camera(follow.scale + HIGHLIGHT_ZOOM * bump, lerp(follow.pivot, at, bump))
        } ?: follow
    val settled = if (winner != null) Camera(WIN_SCALE, winner) else drawing
    return when {
        timeMs < GameCurveTimeline.DRAW_END -> {
            drawing
        }

        timeMs < GameCurveTimeline.ZOOM_END -> {
            val t =
                cameraEase(
                    (timeMs - GameCurveTimeline.DRAW_END) / (GameCurveTimeline.ZOOM_END - GameCurveTimeline.DRAW_END),
                )
            Camera(drawing.scale + (settled.scale - drawing.scale) * t, lerp(drawing.pivot, settled.pivot, t))
        }

        timeMs < GameCurveTimeline.BURST_END -> {
            settled
        }

        else -> {
            val t =
                cameraEase(
                    (timeMs - GameCurveTimeline.BURST_END) / (GameCurveTimeline.END - GameCurveTimeline.BURST_END),
                )
            Camera(settled.scale + (1f - settled.scale) * t, lerp(settled.pivot, center, t))
        }
    }
}

/** Sursaut de zoom (0 → 1 → 0) quand la pointe passe le moment fort, et sa position. */
private fun highlightBump(timeMs: Float, frame: Frame, highlight: GameCurveHighlight?): Pair<Float, Offset>? {
    highlight ?: return null
    if (frame.rounds == 0) return null
    val at = frame.x(highlight.round.toFloat())
    val passed = (at - frame.left) / frame.width
    // Moment où la pointe atteint ce tour, en inversant l'accélération du tracé (approximation suffisante).
    val reach = GameCurveTimeline.DRAW_END * passed
    val local = (timeMs - reach) / HIGHLIGHT_MS
    if (local !in 0f..1f) return null
    return sin(local * PI).toFloat() to Offset(at, (frame.top + frame.bottom) / 2f)
}

private fun ease(t: Float) = FastOutSlowInEasing.transform(t.coerceIn(0f, 1f))

/** Amorti symétrique de la caméra : le zoom et le dézoom démarrent et s'arrêtent en douceur, sans à-coup. */
private val CameraEasing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

private fun cameraEase(t: Float) = CameraEasing.transform(t.coerceIn(0f, 1f))

private fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

/** Repère de la courbe : zone de tracé, de 0 à la cible, tours de 0 à [rounds]. */
private class Frame(
    width: Float,
    height: Float,
    val rounds: Int,
    private val target: Int,
    labelWidth: Float,
    density: Float,
) {
    private val pad = 18f * density
    val left = labelWidth + pad
    val right = width - pad - EMBLEM_SPACE * density
    val top = pad * 1.4f
    val bottom = height - pad * 1.6f
    val width = (right - left).coerceAtLeast(1f)

    fun x(round: Float) = left + width * round / rounds.coerceAtLeast(1)

    fun y(score: Int) = bottom - (bottom - top) * score.coerceIn(0, target.coerceAtLeast(1)) / target.coerceAtLeast(1)

    fun at(dot: CurveDot) = Offset(x(dot.round.toFloat()), y(dot.score))

    fun roundAt(px: Float): Int = (((px - left) / width) * rounds).roundToInt().coerceIn(0, rounds)

    /** Pointe de [line] sur la verticale [reveal] (interpolée entre deux tours), si la ligne y est déjà. */
    fun head(line: GameCurveLine, reveal: Float): Offset? {
        val dots = line.dots.map(::at)
        if (dots.size < 2 || reveal < dots.first().x || reveal >= dots.last().x) return null
        val i = (1 until dots.size).first { reveal <= dots[it].x }
        val a = dots[i - 1]
        val b = dots[i]
        val t = if (b.x == a.x) 1f else (reveal - a.x) / (b.x - a.x)
        return Offset(reveal, a.y + (b.y - a.y) * t)
    }
}

private fun DrawScope.drawTarget(
    frame: Frame,
    target: Int,
    style: TextStyle,
    measurer: TextMeasurer,
    color: Color,
    dash: PathEffect,
) {
    val y = frame.y(target)
    drawLine(color, Offset(frame.left, y), Offset(frame.right, y), 3.dp.toPx(), StrokeCap.Round, dash)
    val text = measurer.measure(target.toString(), style.copy(color = color))
    drawText(text, topLeft = Offset(frame.left - text.size.width - 8.dp.toPx(), y - text.size.height / 2f))
    // Ligne de base, discrète : le zéro.
    drawLine(
        color.copy(alpha = 0.25f),
        Offset(frame.left, frame.bottom),
        Offset(frame.right, frame.bottom),
        2.dp.toPx(),
        StrokeCap.Round,
    )
}

/**
 * Courbe souple entre les points (spline monotone : jamais de bosse entre deux tours) ; une chute (retour en arrière)
 * plonge un peu sous sa valeur avant de s'y poser : la forme est exagérée, jamais les valeurs.
 */
private fun DrawScope.drawLine(frame: Frame, line: GameCurveLine, color: Color) {
    if (line.dots.size < 2) return
    val points = line.dots.map(frame::at)
    val tangents = monotoneTangents(points)
    val path = Path()
    path.moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        val dx = b.x - a.x
        if (dx <= 0f) {
            path.lineTo(b.x, b.y)
            continue
        }
        val dip = if (line.dots[i].mark == CurveDotMark.RESET) (b.y - a.y) * EXAGGERATION else 0f
        path.cubicTo(
            a.x + dx / 3f,
            a.y + tangents[i - 1] * dx / 3f,
            b.x - dx / 3f,
            b.y - tangents[i] * dx / 3f + dip,
            b.x,
            b.y,
        )
    }
    val width = (if (line.winner) 7.dp else 5.dp).toPx()
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Pentes de Fritsch-Carlson : la courbe passe par chaque point sans dépasser ses voisins. */
private fun monotoneTangents(points: List<Offset>): FloatArray {
    val n = points.size
    val slopes =
        FloatArray(n - 1) { i ->
            val dx = points[i + 1].x - points[i].x
            if (dx <= 0f) 0f else (points[i + 1].y - points[i].y) / dx
        }
    val t = FloatArray(n)
    t[0] = slopes[0]
    t[n - 1] = slopes[n - 2]
    for (i in 1 until n - 1) {
        t[i] = if (slopes[i - 1] * slopes[i] <= 0f) 0f else (slopes[i - 1] + slopes[i]) / 2f
    }
    for (i in 0 until n - 1) {
        if (slopes[i] == 0f) {
            t[i] = 0f
            t[i + 1] = 0f
            continue
        }
        val a = t[i] / slopes[i]
        val b = t[i + 1] / slopes[i]
        val h = a * a + b * b
        if (h > FC_LIMIT) {
            val k = 3f / kotlin.math.sqrt(h)
            t[i] = k * a * slopes[i]
            t[i + 1] = k * b * slopes[i]
        }
    }
    return t
}

private fun DrawScope.drawMark(
    dot: CurveDot,
    at: Offset,
    coral: Color,
    saffron: Color,
    line: GameCurveLine,
    selected: Int?,
) {
    if (selected == dot.round) drawCircle(line.color, 7.dp.toPx(), at)
    when (dot.mark) {
        CurveDotMark.NONE, CurveDotMark.LEFT -> {
            Unit
        }

        CurveDotMark.RESET -> {
            drawCircle(coral, 7.dp.toPx(), at)
        }

        CurveDotMark.ELIMINATED -> {
            val r = 7.dp.toPx()
            val stroke = 4.dp.toPx()
            drawLine(coral, at + Offset(-r, -r), at + Offset(r, r), stroke, StrokeCap.Round)
            drawLine(coral, at + Offset(-r, r), at + Offset(r, -r), stroke, StrokeCap.Round)
        }

        CurveDotMark.WIN -> {
            drawCircle(saffron, 11.dp.toPx(), at)
        }
    }
}

/** Lignes de vitesse derrière la pointe pendant le tracé (trois traits fins qui s'estompent). */
private fun DrawScope.drawSpeedLines(head: Offset, color: Color) {
    val length = 26.dp.toPx()
    for (i in 0 until SPEED_LINES) {
        val dy = (i - 1) * 7.dp.toPx()
        val start = head + Offset(-10.dp.toPx() - i * 6.dp.toPx(), dy)
        drawLine(
            color.copy(alpha = 0.55f - i * 0.15f),
            start,
            start - Offset(length, 0f),
            2.5.dp.toPx(),
            StrokeCap.Round,
        )
    }
    drawCircle(color, 7.dp.toPx(), head)
}

/**
 * Centres des emblèmes, à droite du dernier point de chaque ligne, écartés verticalement pour ne jamais se chevaucher
 * et gardés dans le graphique.
 */
private fun emblemCenters(frame: Frame, lines: List<GameCurveLine>, radius: Float, gap: Float): List<Offset?> {
    val wanted =
        lines.map { line ->
            line.dots
                .lastOrNull()
                ?.let(frame::at)
                ?.let { it + Offset(radius * 1.5f, 0f) }
        }
    val order = wanted.indices.filter { wanted[it] != null }.sortedBy { wanted[it]!!.y }
    val placed = wanted.toMutableList()
    val step = radius * 2 + gap
    var previous = Float.NEGATIVE_INFINITY
    order.forEach { i ->
        val y = maxOf(wanted[i]!!.y, previous + step).coerceAtLeast(frame.top - radius)
        placed[i] = Offset(wanted[i]!!.x, y)
        previous = y
    }
    // Trop bas : on remonte l'ensemble.
    val overflow = (previous - (frame.bottom + radius)).coerceAtLeast(0f)
    return placed.map { it?.let { p -> Offset(p.x, p.y - overflow) } }
}

/** Tracés d'un emblème au trait (chemins et épaisseur dans son repère de 24 × 24), lus une fois. */
private class Glyph(
    val viewport: Float,
    val strokes: List<Pair<Path, Float>>,
)

private fun glyphOf(vector: ImageVector): Glyph {
    val strokes = mutableListOf<Pair<Path, Float>>()

    fun collect(node: VectorNode) {
        when (node) {
            is VectorGroup -> node.forEach(::collect)
            is VectorPath -> strokes += node.pathData.toPath() to node.strokeLineWidth
        }
    }
    collect(vector.root)
    return Glyph(vector.viewportWidth, strokes)
}

/** Emblème (ou initiale) de l'équipe en bout de ligne, blanc sur sa couleur. */
private fun DrawScope.drawEmblem(
    center: Offset,
    line: GameCurveLine,
    glyph: Glyph?,
    measurer: TextMeasurer,
    style: TextStyle,
) {
    val r = 14.dp.toPx()
    val faded =
        line.dots
            .lastOrNull()
            ?.mark
            .let { it == CurveDotMark.ELIMINATED || it == CurveDotMark.LEFT }
    val alpha = if (faded) 0.6f else 1f
    drawCircle(line.color.copy(alpha = alpha), r, center)
    if (glyph != null) {
        val icon = r * 1.2f
        val k = icon / glyph.viewport
        translate(center.x - icon / 2, center.y - icon / 2) {
            scale(k, k, pivot = Offset.Zero) {
                glyph.strokes.forEach { (path, width) ->
                    drawPath(
                        path,
                        Color.White.copy(alpha = alpha),
                        style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    } else {
        val text = measurer.measure(line.initial.take(1).uppercase(), style)
        drawText(text, topLeft = center - Offset(text.size.width / 2f, text.size.height / 2f), alpha = alpha)
    }
}

/** Éclat radial à pointes arrondies et quilles-confettis qui filent hors champ, après l'image d'impact. */
private fun DrawScope.drawBurst(
    timeMs: Float,
    at: Offset,
    team: Color,
    saffron: Color,
    coral: Color,
    confetti: List<Confetti>,
    rounded: PathEffect,
) {
    if (timeMs < GameCurveTimeline.ZOOM_END || timeMs >= GameCurveTimeline.BURST_END + 200f) return
    val frozen = timeMs < GameCurveTimeline.FREEZE_END
    val t =
        ((timeMs - GameCurveTimeline.FREEZE_END) / (GameCurveTimeline.BURST_END - GameCurveTimeline.FREEZE_END))
            .coerceIn(
                0f,
                1f,
            )
    val radius = 34.dp.toPx() * if (frozen) 1f else 1f + 1.6f * ease(t)
    val fade = if (frozen) 1f else (1f - t).coerceIn(0f, 1f)
    // Pointes arrondies : le polygone est rempli puis bordé de sa propre couleur, joints ronds.
    val shape = burst(at, radius)
    val fill = (if (frozen) team else coral).copy(alpha = fade)
    drawPath(shape, fill, style = Fill)
    drawPath(shape, fill, style = Stroke(10.dp.toPx(), join = StrokeJoin.Round))
    drawPath(burst(at, radius * 0.62f), saffron.copy(alpha = fade), style = Stroke(6.dp.toPx(), pathEffect = rounded))
    if (frozen) return
    val distance = size.maxDimension * ease(t)
    confetti.forEach { piece ->
        val p = at + Offset(cos(piece.angle), sin(piece.angle)) * distance * piece.speed
        rotate(piece.spin * t * 360f, p) {
            val w = 7.dp.toPx()
            val h = 16.dp.toPx()
            drawRoundRect(
                when (piece.tint) {
                    0 -> team
                    1 -> saffron
                    else -> coral
                },
                topLeft = p - Offset(w / 2, h / 2),
                size = Size(w, h),
                cornerRadius = CornerRadius(w / 2),
            )
        }
    }
}

private fun burst(center: Offset, radius: Float): Path =
    Path().apply {
        for (i in 0 until BURST_POINTS * 2) {
            val r = if (i % 2 == 0) radius else radius * 0.62f
            val angle = PI * i / BURST_POINTS - PI / 2
            val x = center.x + (r * cos(angle)).toFloat()
            val y = center.y + (r * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

/** Quille-confetti : direction, vitesse, rotation et teinte tirées une fois avec une graine fixe. */
private class Confetti(
    val angle: Float,
    val speed: Float,
    val spin: Float,
    val tint: Int,
) {
    companion object {
        fun create(): List<Confetti> {
            val random = Random(SEED)
            return List(CONFETTI) {
                Confetti(
                    angle = (random.nextFloat() * 2 * PI).toFloat(),
                    speed = 0.5f + random.nextFloat() * 0.7f,
                    spin = random.nextFloat() * 2f - 1f,
                    tint = it % 3,
                )
            }
        }
    }
}

private const val FOLLOW_SCALE = 1.25f
private const val HIGHLIGHT_ZOOM = 0.45f
private const val HIGHLIGHT_MS = 420f
private const val WIN_SCALE = 1.6f
private const val EXAGGERATION = 0.35f
private const val FC_LIMIT = 9f
private const val EMBLEM_SPACE = 40f
private const val SPEED_LINES = 3
private const val BURST_POINTS = 10
private const val CONFETTI = 18
private const val SEED = 50
