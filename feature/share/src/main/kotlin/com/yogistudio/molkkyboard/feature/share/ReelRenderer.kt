package com.yogistudio.molkkyboard.feature.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlin.math.cos
import kotlin.math.exp
import kotlin.random.Random

/**
 * Dessine une image du mini-clip à l'instant [timeMs], dans un [Canvas] Android (aucune dépendance à Compose :
 * le même code sert à la vidéo MP4 et au GIF). Tout est préparé une fois : pinceaux, trajectoires, QR code ;
 * la boucle de dessin n'alloue rien.
 *
 * Déroulé (≈ 8,8 s) : 1. le strike, les quilles s'envolent ; 2. la course vers 50, tour après tour, avec les
 * retours à 25 et les éliminations ; 3. le lancer décisif et la pluie de quilles ; 4. carton final avec le
 * score, le logotype et le QR code.
 */
class ReelRenderer(
    private val data: ReelData,
    val width: Int,
    val height: Int,
    display: Typeface,
    body: Typeface,
) {
    private val unit = width / REFERENCE_WIDTH
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val big = textPaint(display, 0f)
    private val text = textPaint(body, 0f)
    private val rect = RectF()
    private val random = Random(SEED)
    private val pinStartX = FloatArray(PINS) { (0.3f + (it % 4) * 0.13f) }
    private val pinStartY = FloatArray(PINS) { 0.3f + (it / 4) * 0.08f }
    private val pinVx = FloatArray(PINS) { (random.nextFloat() - 0.5f) * 1.6f }
    private val pinVy = FloatArray(PINS) { -0.6f - random.nextFloat() * 0.8f }
    private val pinSpin = FloatArray(PINS) { (random.nextFloat() - 0.5f) * 900f }
    private val confettiX = FloatArray(CONFETTI) { random.nextFloat() }
    private val confettiSpeed = FloatArray(CONFETTI) { 0.35f + random.nextFloat() * 0.4f }
    private val confettiSpin = FloatArray(CONFETTI) { (random.nextFloat() - 0.5f) * 540f }
    private val confettiDelay = FloatArray(CONFETTI) { random.nextFloat() * 0.5f }

    /** Score affiché de chaque équipe, recalculé à chaque image (tableau réutilisé). */
    private val scores = IntArray(data.teams.size)
    private val previousScores = IntArray(data.teams.size)
    private val out = BooleanArray(data.teams.size)

    /** Libellés préparés une fois (pas de chaîne construite pendant le rendu). */
    private val rowLabels = data.teams.map { listOfNotNull(it.avatar, it.name).joinToString(" ").take(NAME_MAX) }

    /** Carton final : classement par score final (le vainqueur d'abord). */
    private val outroOrder =
        data.teams.indices.sortedWith(
            compareByDescending<Int> { it == data.winner }.thenByDescending { finalScore(it) },
        )
    private val rankLabels =
        outroOrder.mapIndexed {
            rank,
            team,
            ->
            "${rank + 1}. ${data.teams[team].name}".take(NAME_MAX)
        }
    private val roundLabels = (0..data.steps.maxOfOrNull { it.round }.let { it ?: 1 }).map(data.labels.round)
    private val winnerName = data.winner?.let { data.teams[it].name.take(NAME_MAX) }
    private val qr = QRCodeWriter().encode(STORE_URL, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 1))

    val durationMs: Long get() = END

    fun draw(canvas: Canvas, timeMs: Long) {
        when {
            timeMs < RACE_START -> drawStrike(canvas, timeMs)
            timeMs < WIN_START -> drawRace(canvas, timeMs)
            timeMs < OUTRO_START -> drawWin(canvas, timeMs)
            else -> drawOutro(canvas)
        }
    }

    /** Variante pratique pour le GIF et les tests. */
    fun render(bitmap: Bitmap, timeMs: Long) = draw(Canvas(bitmap), timeMs)

    private fun drawStrike(canvas: Canvas, t: Long) {
        canvas.drawColor(BIRCH)
        // Les 12 quilles, intactes, s'envolent en tournoyant ; un aplat safran bref à l'impact.
        if (t in IMPACT_MS until IMPACT_MS + FREEZE_MS) canvas.drawColor(SAFFRON)
        val flight = ((t - IMPACT_MS).coerceAtLeast(0L)) / 1_000f
        for (i in 0 until PINS) {
            val x = width * (pinStartX[i] + pinVx[i] * flight)
            val y = height * (pinStartY[i] + pinVy[i] * flight + GRAVITY * flight * flight)
            canvas.save()
            canvas.rotate(pinSpin[i] * flight, x, y)
            fill.color = if (t in IMPACT_MS until IMPACT_MS + FREEZE_MS) FOREST else WOOD
            rect.set(x - PIN_W * unit, y - PIN_H * unit, x + PIN_W * unit, y + PIN_H * unit)
            canvas.drawRoundRect(rect, PIN_W * unit * 0.4f, PIN_W * unit * 0.4f, fill)
            canvas.restore()
        }
        if (t >= IMPACT_MS + FREEZE_MS) {
            val pop = spring(((t - IMPACT_MS - FREEZE_MS) / BUBBLE_MS.toFloat()).coerceIn(0f, 1f))
            big.textSize = 150f * unit * pop
            big.color = FOREST
            canvas.drawText(data.title, width / 2f, height * 0.62f, big)
        }
    }

    private fun drawRace(canvas: Canvas, t: Long) {
        canvas.drawColor(FOREST)
        val p = (t - RACE_START) / (WIN_START - RACE_START).toFloat()
        val steps = data.steps
        val exact = p * steps.size
        val done = exact.toInt().coerceAtMost(steps.size)
        val within = exact - done
        scores.fill(0)
        out.fill(false)
        var round = 1
        var last: ReelStep? = null
        for (s in 0 until done) {
            val step = steps[s]
            scores[step.team] = step.score
            if (step.kind == StepKind.ELIMINATED) out[step.team] = true
            round = step.round
            last = step
        }
        previousScores.fill(0)
        for (s in 0 until (done - 1).coerceAtLeast(0)) previousScores[steps[s].team] = steps[s].score
        big.textSize = 64f * unit
        big.color = SAFFRON
        canvas.drawText(roundLabels[round.coerceIn(0, roundLabels.lastIndex)], width / 2f, height * 0.12f, big)
        val rows = data.teams.size.coerceAtMost(MAX_ROWS)
        val rowH = height * 0.62f / rows
        for (i in 0 until rows) {
            val team = data.teams[i]
            val top = height * 0.2f + i * rowH
            // La barre de l'équipe qui vient de lancer glisse vers son nouveau score.
            val shown =
                if (last != null && last.team == i) {
                    previousScores[i] + (scores[i] - previousScores[i]) * spring(within.coerceIn(0f, 1f))
                } else {
                    scores[i].toFloat()
                }
            text.textSize = 40f * unit
            text.color = BIRCH
            text.textAlign = Paint.Align.LEFT
            canvas.drawText(rowLabels[i], MARGIN * unit, top + 44f * unit, text)
            rect.set(MARGIN * unit, top + 64f * unit, width - MARGIN * unit, top + 104f * unit)
            fill.color = TRACK
            canvas.drawRoundRect(rect, 20f * unit, 20f * unit, fill)
            rect.right = rect.left + (rect.width() * (shown / data.target)).coerceIn(0f, rect.width())
            fill.color = if (out[i]) GREY else team.color
            canvas.drawRoundRect(rect, 20f * unit, 20f * unit, fill)
            big.textSize = 56f * unit
            big.color = if (out[i]) GREY else BIRCH
            big.textAlign = Paint.Align.RIGHT
            canvas.drawText(shown.toInt().toString(), width - MARGIN * unit, top + 44f * unit, big)
            big.textAlign = Paint.Align.CENTER
            if (out[i]) stamp(canvas, data.labels.eliminated, width * 0.62f, top + 84f * unit, CORAL)
            val justReset = last?.team == i && last.kind == StepKind.RESET
            if (justReset && within < RESET_FLASH) {
                stamp(canvas, data.labels.reset, width * 0.5f, top + 84f * unit, CORAL)
            }
        }
    }

    private fun drawWin(canvas: Canvas, t: Long) {
        val winner = data.winner?.let(data.teams::get)
        val color = winner?.color ?: SAFFRON
        canvas.drawColor(BIRCH)
        val p = (t - WIN_START) / (OUTRO_START - WIN_START).toFloat()
        // Pluie de petites quilles aux couleurs du vainqueur.
        for (c in 0 until CONFETTI) {
            val local = p - confettiDelay[c]
            if (local <= 0f) continue
            val x = width * confettiX[c]
            val y = -40f * unit + height * confettiSpeed[c] * local * 2f
            canvas.save()
            canvas.rotate(confettiSpin[c] * local, x, y)
            fill.color =
                if (c % 3 == 0) {
                    SAFFRON
                } else if (c % 3 == 1) {
                    CORAL
                } else {
                    color
                }
            rect.set(x - 14f * unit, y - 28f * unit, x + 14f * unit, y + 28f * unit)
            canvas.drawRoundRect(rect, 8f * unit, 8f * unit, fill)
            canvas.restore()
        }
        val pop = spring((p * 2f).coerceIn(0f, 1f))
        big.textSize = 300f * unit * pop
        big.color = color
        canvas.drawText(data.target.toString(), width / 2f, height * 0.45f, big)
        if (winnerName != null) {
            big.textSize = 90f * unit
            big.color = FOREST
            canvas.drawText(winnerName, width / 2f, height * 0.6f, big)
        }
        text.textSize = 44f * unit
        text.color = INK
        text.textAlign = Paint.Align.CENTER
        canvas.drawText(data.keyFigure, width / 2f, height * 0.68f, text)
    }

    private fun drawOutro(canvas: Canvas) {
        canvas.drawColor(BIRCH)
        big.textSize = 110f * unit
        big.color = FOREST
        canvas.drawText(data.title, width / 2f, height * 0.16f, big)
        val rows = data.teams.size.coerceAtMost(MAX_ROWS)
        for (i in 0 until rows) {
            val index = outroOrder[i]
            val team = data.teams[index]
            val y = height * 0.28f + i * 90f * unit
            text.textSize = 48f * unit
            text.color = INK
            text.textAlign = Paint.Align.LEFT
            canvas.drawText(rankLabels[i], MARGIN * unit, y, text)
            big.textSize = 56f * unit
            big.color = team.color
            big.textAlign = Paint.Align.RIGHT
            canvas.drawText(finalScores[index], width - MARGIN * unit, y, big)
            big.textAlign = Paint.Align.CENTER
        }
        text.textAlign = Paint.Align.CENTER
        text.textSize = 40f * unit
        text.color = INK
        canvas.drawText(data.labels.scan, width / 2f, height * 0.72f, text)
        // Logotype : « Molkky » vert forêt, « Board » safran, même ligne de base.
        big.textSize = 96f * unit
        val molkky = big.measureText("Molkky ")
        val board = big.measureText("Board")
        val start = (width - molkky - board) / 2f
        big.textAlign = Paint.Align.LEFT
        big.color = FOREST
        canvas.drawText("Molkky ", start, height * 0.8f, big)
        big.color = SAFFRON
        canvas.drawText("Board", start + molkky, height * 0.8f, big)
        big.textAlign = Paint.Align.CENTER
        drawQr(canvas, width / 2f, height * 0.88f, 200f * unit)
        text.textSize = 36f * unit
        canvas.drawText(data.labels.hashtag, width / 2f, height * 0.97f, text)
    }

    private fun finalScore(team: Int): Int = data.steps.lastOrNull { it.team == team }?.score ?: 0

    private val finalScores = data.teams.indices.map { finalScore(it).toString() }

    private fun drawQr(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        fill.color = WHITE
        rect.set(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)
        canvas.drawRect(rect, fill)
        val cell = size / qr.width
        fill.color = INK
        for (y in 0 until qr.height) {
            for (x in 0 until qr.width) {
                if (!qr[x, y]) continue
                val left = cx - size / 2 + x * cell
                val top = cy - size / 2 + y * cell
                canvas.drawRect(left, top, left + cell + 0.5f, top + cell + 0.5f, fill)
            }
        }
    }

    /** Tampon penché (« Éliminé », « Retour à 25 ! »). */
    private fun stamp(canvas: Canvas, label: String, cx: Float, cy: Float, color: Int) {
        canvas.save()
        canvas.rotate(STAMP_ANGLE, cx, cy)
        text.textSize = 40f * unit
        text.textAlign = Paint.Align.CENTER
        val w = text.measureText(label) / 2 + 24f * unit
        rect.set(cx - w, cy - 34f * unit, cx + w, cy + 22f * unit)
        fill.color = color
        canvas.drawRoundRect(rect, 18f * unit, 18f * unit, fill)
        text.color = WHITE
        canvas.drawText(label, cx, cy + 6f * unit, text)
        canvas.restore()
    }

    private fun textPaint(typeface: Typeface, size: Float) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = size
            textAlign = Paint.Align.CENTER
        }

    private fun spring(p: Float): Float = if (p <= 0f) 0f else 1f - exp(-6f * p) * cos(10f * p)

    companion object {
        const val RACE_START = 1_400L
        const val WIN_START = 5_800L
        const val OUTRO_START = 7_400L
        const val END = 8_800L
        private const val IMPACT_MS = 300L
        private const val FREEZE_MS = 80L
        private const val BUBBLE_MS = 350L
        private const val REFERENCE_WIDTH = 1_080f
        private const val SEED = 12
        private const val PINS = 12
        private const val CONFETTI = 40
        private const val MAX_ROWS = 6
        private const val NAME_MAX = 22
        private const val MARGIN = 80f
        private const val PIN_W = 34f
        private const val PIN_H = 46f
        private const val GRAVITY = 0.9f
        private const val RESET_FLASH = 0.9f
        private const val STAMP_ANGLE = -8f
        private const val BIRCH = 0xFFF4EBDD.toInt()
        private const val FOREST = 0xFF1F4D3A.toInt()
        private const val SAFFRON = 0xFFF2A93B.toInt()
        private const val CORAL = 0xFFE8604C.toInt()
        private const val INK = 0xFF1B1B18.toInt()
        private const val WOOD = 0xFFE9D3AE.toInt()
        private const val TRACK = 0x40F4EBDD
        private const val GREY = 0xFF8A8F86.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()
    }
}
