package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.sqrt

/**
 * Tracé d'une ligne de la courbe de la partie : spline monotone entre les points (jamais de bosse entre deux tours).
 * Un retour en arrière ([resets]) passe d'abord en pic à la hauteur [peakY], au-dessus de la ligne de la cible (le
 * dépassement), puis plonge un peu sous sa valeur avant de s'y poser : la forme est exagérée, jamais les valeurs.
 */
internal fun curvePath(points: List<Offset>, resets: List<Boolean>, peakY: Float): Path {
    val tangents = monotoneTangents(points)
    // Avant un dépassement, la montée continue sur sa lancée vers le pic, sans palier.
    for (i in 2 until points.size) {
        val dx = points[i - 1].x - points[i - 2].x
        if (resets[i] && dx > 0f) tangents[i - 1] = (points[i - 1].y - points[i - 2].y) / dx
    }
    val path = Path()
    path.moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        val dx = b.x - a.x
        when {
            dx <= 0f -> {
                path.lineTo(b.x, b.y)
            }

            resets[i] -> {
                overshoot(path, a, tangents[i - 1], b, peakY)
            }

            else -> {
                path.cubicTo(
                    a.x + dx / 3f,
                    a.y + tangents[i - 1] * dx / 3f,
                    b.x - dx / 3f,
                    b.y - tangents[i] * dx / 3f,
                    b.x,
                    b.y,
                )
            }
        }
    }
    return path
}

/**
 * Dépassement de la cible : montée souple de [a] (pente [slope]) jusqu'au pic [peakY], puis chute presque verticale
 * qui plonge sous [b] avant de s'y poser.
 */
private fun overshoot(path: Path, a: Offset, slope: Float, b: Offset, peakY: Float) {
    val dx = b.x - a.x
    val peak = Offset(a.x + dx * PEAK_AT, peakY)
    val lead = dx * 0.2f
    path.cubicTo(a.x + lead, (a.y + slope * lead).coerceAtLeast(peakY), peak.x - dx * 0.2f, peak.y, peak.x, peak.y)
    val dip = (b.y - peak.y) * EXAGGERATION
    path.cubicTo(peak.x + dx * 0.16f, peak.y, b.x - dx * 0.18f, b.y + dip, b.x, b.y)
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
            val k = 3f / sqrt(h)
            t[i] = k * a * slopes[i]
            t[i + 1] = k * b * slopes[i]
        }
    }
    return t
}

/** Plongée sous la valeur d'arrivée après un dépassement, en part de la hauteur de la chute. */
private const val EXAGGERATION = 0.12f

/** Place du pic de dépassement dans le tour (0 à 1). */
private const val PEAK_AT = 0.5f
private const val FC_LIMIT = 9f
