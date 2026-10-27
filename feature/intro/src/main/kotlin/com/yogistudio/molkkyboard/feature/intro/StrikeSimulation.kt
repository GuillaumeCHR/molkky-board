package com.yogistudio.molkkyboard.feature.intro

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Formation de départ officielle, vue du lanceur (rangée du fond en haut). */
internal val FORMATION = listOf(listOf(7, 9, 8), listOf(5, 11, 12, 6), listOf(3, 10, 4), listOf(1, 2))

/**
 * Géométrie de la scène, trajectoires des 12 quilles et lignes de vitesse, entièrement précalculées une fois pour
 * une taille d'écran donnée : la boucle de dessin ne fait que lire des tableaux (aucune allocation, 60 i/s visées).
 * Les quilles restent **entières** : elles s'envolent en tournoyant ; quelques-unes filent vers le fond en
 * rétrécissant puis disparaissent dans un scintillement. Graine fixe : images stables pour les captures.
 */
internal class StrikeSimulation(
    val width: Float,
    val height: Float,
    seed: Int = SEED,
) {
    /** Unité d'échelle indépendante de la densité : 1 = 1/400 du plus petit côté. */
    val unit = minOf(width, height) / 400f
    val pinWidth = 58f * unit
    val pinHeight = pinWidth * 1.3f

    /** Centre (x, y) de chaque quille, dans l'ordre de [FORMATION] aplati. */
    val pinNumbers: IntArray = FORMATION.flatten().toIntArray()
    val pinX = FloatArray(PIN_COUNT)
    val pinY = FloatArray(PIN_COUNT)
    val pinRow = IntArray(PIN_COUNT)

    /** Quille qui file vers le fond (rétrécit, puis scintille). */
    val pinFar = BooleanArray(PIN_COUNT)

    /** Point d'impact du mölkky : sous la formation ; centre de la formation (cible des lignes de vitesse). */
    val impactX: Float
    val impactY: Float
    val centerY: Float

    private val trajX = FloatArray(PIN_COUNT * FRAMES)
    private val trajY = FloatArray(PIN_COUNT * FRAMES)
    private val trajRot = FloatArray(PIN_COUNT * FRAMES)
    private val trajScale = FloatArray(PIN_COUNT * FRAMES)

    /** Lignes de vitesse : angle autour de la cible, distance de départ, longueur, décalage d'apparition. */
    val lineAngle = FloatArray(SPEED_LINES)
    val lineReach = FloatArray(SPEED_LINES)
    val lineLength = FloatArray(SPEED_LINES)
    val lineDelay = FloatArray(SPEED_LINES)
    val lineCos = FloatArray(SPEED_LINES)
    val lineSin = FloatArray(SPEED_LINES)

    init {
        val centerX = width / 2
        val top = height * 0.24f
        val stepX = pinWidth * 1.35f
        val stepY = pinHeight * 0.78f
        var index = 0
        FORMATION.forEachIndexed { row, pins ->
            val rowWidth = (pins.size - 1) * stepX
            pins.forEachIndexed { col, number ->
                pinX[index] = centerX - rowWidth / 2 + col * stepX
                pinY[index] = top + row * stepY
                pinRow[index] = row
                pinFar[index] = number in FAR_PINS
                index++
            }
        }
        impactX = centerX
        impactY = top + (FORMATION.size - 1) * stepY + pinHeight * 0.4f
        centerY = top + (FORMATION.size - 1) * stepY / 2
        val random = Random(seed)
        simulatePins(random)
        prepareSpeedLines(random)
    }

    private fun simulatePins(random: Random) {
        val gravity = GRAVITY * unit
        val dt = 1f / FPS
        for (p in 0 until PIN_COUNT) {
            var x = pinX[p]
            var y = pinY[p]
            var vx: Float
            var vy: Float
            if (pinFar[p]) {
                // Vers le fond : presque sur place, léger glissement vers le haut, la quille rétrécit.
                vx = (random.nextFloat() - 0.5f) * 80f * unit
                vy = -(140f + random.nextFloat() * 60f) * unit
            } else {
                // Projetée à l'opposé du point d'impact, vers le haut et les côtés : sort de l'écran en moins d'1 s.
                val dx = x - impactX
                val dy = y - impactY
                val distance = hypot(dx, dy).coerceAtLeast(1f)
                // Assez lentes pour qu'on les voie s'envoler (≈ 0,6 à 1 s avant de sortir du cadre).
                val speed = (420f + random.nextFloat() * 320f) * unit
                vx = dx / distance * speed * 1.3f + (random.nextFloat() - 0.5f) * 160f * unit
                vy = dy / distance * speed - (380f + random.nextFloat() * 260f) * unit
            }
            var rot = 0f
            val spin = (random.nextFloat() - 0.5f) * 1_400f
            var scale = 1f
            for (f in 0 until FRAMES) {
                val i = p * FRAMES + f
                trajX[i] = x
                trajY[i] = y
                trajRot[i] = rot
                trajScale[i] = scale
                if (!pinFar[p]) vy += gravity * dt
                x += vx * dt
                y += vy * dt
                rot += spin * dt
                if (pinFar[p]) scale = (scale - FAR_SHRINK * dt).coerceAtLeast(0f)
            }
        }
    }

    private fun prepareSpeedLines(random: Random) {
        for (l in 0 until SPEED_LINES) {
            lineAngle[l] = (l.toFloat() / SPEED_LINES + random.nextFloat() * 0.02f) * 2f * PI.toFloat()
            lineReach[l] = (70f + random.nextFloat() * 70f) * unit
            lineLength[l] = (40f + random.nextFloat() * 50f) * unit
            lineDelay[l] = random.nextFloat() * 0.35f
            lineCos[l] = cos(lineAngle[l])
            lineSin[l] = sin(lineAngle[l])
        }
    }

    /** Pose de la quille [pin] au temps [time] (ms depuis le début de l'intro), interpolée entre deux images. */
    fun x(pin: Int, time: Long): Float = sample(trajX, pin, time)

    fun y(pin: Int, time: Long): Float = sample(trajY, pin, time)

    fun rotation(pin: Int, time: Long): Float = sample(trajRot, pin, time)

    fun scale(pin: Int, time: Long): Float = sample(trajScale, pin, time)

    private fun sample(track: FloatArray, pin: Int, time: Long): Float {
        val frame = ((time - IntroTimeline.FREEZE_END).coerceAtLeast(0L) * FPS / 1_000f).coerceAtMost(FRAMES - 1f)
        val f0 = frame.toInt()
        val f1 = (f0 + 1).coerceAtMost(FRAMES - 1)
        val t = frame - f0
        val base = pin * FRAMES
        return track[base + f0] + (track[base + f1] - track[base + f0]) * t
    }

    companion object {
        const val SEED = 1977
        const val PIN_COUNT = 12

        /** Au plus 40 lignes de vitesse (contrainte de performance du storyboard). */
        const val SPEED_LINES = 36
        private const val FPS = 60
        private val FRAMES = ((IntroTimeline.SIMULATION_END - IntroTimeline.FREEZE_END) * FPS / 1_000).toInt() + 1
        private const val GRAVITY = 900f

        /** Les quilles qui filent vers le fond (clin d'œil au gag du manga). */
        private val FAR_PINS = setOf(9, 11, 12)
        private const val FAR_SHRINK = 1.6f
    }
}
