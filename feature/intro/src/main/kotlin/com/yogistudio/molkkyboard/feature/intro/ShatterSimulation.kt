package com.yogistudio.molkkyboard.feature.intro

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Formation de départ officielle, vue du lanceur (rangée du fond en haut). */
internal val FORMATION = listOf(listOf(7, 9, 8), listOf(5, 11, 12, 6), listOf(3, 10, 4), listOf(1, 2))

/**
 * Géométrie de la scène et trajectoires des éclats, entièrement précalculées une fois pour une taille
 * d'écran donnée : la boucle de dessin ne fait que lire des tableaux (aucune allocation, 60 i/s visées).
 * Graine fixe : le résultat est identique à chaque lancement, donc stable pour les captures Roborazzi.
 */
internal class ShatterSimulation(
    val width: Float,
    val height: Float,
    seed: Int = SEED,
) {
    /** Unité d'échelle indépendante de la densité : 1 = 1/400 du plus petit côté. */
    val unit = minOf(width, height) / 400f
    val pinWidth = 44f * unit
    val pinHeight = pinWidth * 1.3f
    val groundY = height * 0.86f

    /** Centre (x, y) de chaque quille, dans l'ordre de [FORMATION] aplati. */
    val pinNumbers: IntArray = FORMATION.flatten().toIntArray()
    val pinX = FloatArray(PIN_COUNT)
    val pinY = FloatArray(PIN_COUNT)
    val pinRow = IntArray(PIN_COUNT)

    /** Point d'impact du mölkky : sous la formation. */
    val impactX: Float
    val impactY: Float

    val shardCount = PIN_COUNT * SHARDS_PER_PIN
    val shardPin = IntArray(shardCount)
    val shardColor = IntArray(shardCount)

    /** Sommets de chaque éclat, relatifs à son centre : [VERTICES] couples (x, y). */
    val shardShape = FloatArray(shardCount * VERTICES * 2)
    private val trajX = FloatArray(shardCount * FRAMES)
    private val trajY = FloatArray(shardCount * FRAMES)
    private val trajRot = FloatArray(shardCount * FRAMES)

    /** Cibles des éclats qui forment le logotype à l'étape 5 (un éclat sur [ASSEMBLE_EVERY]). */
    val targetX = FloatArray(shardCount)
    val targetY = FloatArray(shardCount)

    init {
        val centerX = width / 2
        val top = height * 0.26f
        val stepX = pinWidth * 1.35f
        val stepY = pinHeight * 0.78f
        var index = 0
        FORMATION.forEachIndexed { row, pins ->
            val rowWidth = (pins.size - 1) * stepX
            pins.forEachIndexed { col, _ ->
                pinX[index] = centerX - rowWidth / 2 + col * stepX
                pinY[index] = top + row * stepY
                pinRow[index] = row
                index++
            }
        }
        impactX = centerX
        impactY = top + (FORMATION.size - 1) * stepY + pinHeight * 0.4f
        simulate(Random(seed))
    }

    private fun simulate(random: Random) {
        val gravity = GRAVITY * unit
        val dt = 1f / FPS
        for (s in 0 until shardCount) {
            val pin = s / SHARDS_PER_PIN
            shardPin[s] = pin
            shardColor[s] = random.nextInt(WOOD_TONES)
            // Polygone irrégulier autour du centre.
            val radius = (4f + random.nextFloat() * 6f) * unit
            for (v in 0 until VERTICES) {
                val angle = (v.toFloat() / VERTICES + random.nextFloat() * 0.15f) * 2f * PI.toFloat()
                val r = radius * (0.6f + random.nextFloat() * 0.6f)
                shardShape[(s * VERTICES + v) * 2] = cos(angle) * r
                shardShape[(s * VERTICES + v) * 2 + 1] = sin(angle) * r
            }
            // Départ dans le volume de la quille, projeté à l'opposé du point d'impact et vers le haut.
            var x = pinX[pin] + (random.nextFloat() - 0.5f) * pinWidth * 0.8f
            var y = pinY[pin] + (random.nextFloat() - 0.5f) * pinHeight * 0.8f
            val dx = x - impactX
            val dy = y - impactY
            val distance = hypot(dx, dy).coerceAtLeast(1f)
            val speed = (140f + random.nextFloat() * 260f) * unit
            var vx = dx / distance * speed + (random.nextFloat() - 0.5f) * 220f * unit
            var vy = dy / distance * speed - (120f + random.nextFloat() * 300f) * unit
            var rot = random.nextFloat() * 360f
            var spin = (random.nextFloat() - 0.5f) * 1_200f
            for (f in 0 until FRAMES) {
                val i = s * FRAMES + f
                trajX[i] = x
                trajY[i] = y
                trajRot[i] = rot
                vy += gravity * dt
                x += vx * dt
                y += vy * dt
                rot += spin * dt
                if (y > groundY) {
                    // Rebond amorti au sol.
                    y = groundY
                    vy = -vy * BOUNCE
                    vx *= FRICTION
                    spin *= FRICTION
                }
            }
            // Cible dans le rectangle du logotype (centre de l'écran), en deux rangées.
            val slot = s / ASSEMBLE_EVERY
            val slots = shardCount / ASSEMBLE_EVERY
            val perRow = (slots + 1) / 2
            targetX[s] = width * 0.15f + (slot % perRow + 0.5f) * (width * 0.7f / perRow)
            targetY[s] = height * 0.45f + (slot / perRow - 0.5f) * 18f * unit
        }
    }

    fun assembles(shard: Int): Boolean = shard % ASSEMBLE_EVERY == 0

    /** Position de l'éclat [shard] au temps [time] (ms depuis le début de l'intro), interpolée entre deux images. */
    fun x(shard: Int, time: Long): Float = sample(trajX, shard, time)

    fun y(shard: Int, time: Long): Float = sample(trajY, shard, time)

    fun rotation(shard: Int, time: Long): Float = sample(trajRot, shard, time)

    private fun sample(track: FloatArray, shard: Int, time: Long): Float {
        val frame = ((time - IntroTimeline.IMPACT).coerceAtLeast(0L) * FPS / 1_000f).coerceAtMost(FRAMES - 1f)
        val f0 = frame.toInt()
        val f1 = (f0 + 1).coerceAtMost(FRAMES - 1)
        val t = frame - f0
        val base = shard * FRAMES
        return track[base + f0] + (track[base + f1] - track[base + f0]) * t
    }

    companion object {
        const val SEED = 1977
        const val PIN_COUNT = 12
        const val SHARDS_PER_PIN = 12
        const val VERTICES = 5
        const val WOOD_TONES = 5
        const val ASSEMBLE_EVERY = 4
        private const val FPS = 60
        private val FRAMES = ((IntroTimeline.SIMULATION_END - IntroTimeline.IMPACT) * FPS / 1_000).toInt() + 1
        private const val GRAVITY = 1_700f
        private const val BOUNCE = 0.35f
        private const val FRICTION = 0.7f
    }
}
