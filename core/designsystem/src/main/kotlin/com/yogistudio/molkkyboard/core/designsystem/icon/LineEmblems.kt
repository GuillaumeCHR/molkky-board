package com.yogistudio.molkkyboard.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Emblèmes au trait (proposition C6 : avatars sobres à la place des emoji), dans le style des icônes de l'app :
 * trait épais arrondi, sur une grille de 24. Clés stables, prêtes à être enregistrées.
 */
object LineEmblems {
    val all: Map<String, ImageVector> by lazy {
        linkedMapOf(
            "pin" to
                icon("pin") {
                    moveTo(8f, 7f)
                    lineTo(16f, 4.5f)
                    lineTo(16f, 20f)
                    lineTo(8f, 20f)
                    close()
                },
            "leaf" to
                icon("leaf") {
                    moveTo(5f, 19f)
                    curveTo(5f, 10f, 10f, 5f, 19f, 5f)
                    curveTo(19f, 14f, 14f, 19f, 5f, 19f)
                    close()
                    moveTo(5f, 19f)
                    lineTo(13f, 11f)
                },
            "bolt" to
                icon("bolt") {
                    moveTo(13f, 3f)
                    lineTo(6f, 13.5f)
                    lineTo(11.5f, 13.5f)
                    lineTo(10.5f, 21f)
                    lineTo(18f, 10f)
                    lineTo(12.5f, 10f)
                    close()
                },
            "wave" to
                icon("wave") {
                    moveTo(3f, 9f)
                    curveTo(6f, 6f, 9f, 6f, 12f, 9f)
                    curveTo(15f, 12f, 18f, 12f, 21f, 9f)
                    moveTo(3f, 15f)
                    curveTo(6f, 12f, 9f, 12f, 12f, 15f)
                    curveTo(15f, 18f, 18f, 18f, 21f, 15f)
                },
            "star" to icon("star") { star(12f, 12.5f, 9f, 4f) },
            "sun" to
                icon("sun") {
                    circle(12f, 12f, 4f)
                    repeat(8) { i ->
                        val a = i * PI / 4
                        moveTo(12f + 7f * cos(a).toFloat(), 12f + 7f * sin(a).toFloat())
                        lineTo(12f + 9.5f * cos(a).toFloat(), 12f + 9.5f * sin(a).toFloat())
                    }
                },
            "mountain" to
                icon("mountain") {
                    moveTo(2.5f, 19f)
                    lineTo(9f, 8f)
                    lineTo(13f, 14f)
                    lineTo(16f, 10f)
                    lineTo(21.5f, 19f)
                    close()
                    moveTo(7.2f, 11f)
                    lineTo(9f, 12.5f)
                    lineTo(10.8f, 11f)
                },
            "flame" to
                icon("flame") {
                    moveTo(12f, 3f)
                    curveTo(16f, 8f, 18f, 11f, 18f, 14.5f)
                    curveTo(18f, 18f, 15.5f, 21f, 12f, 21f)
                    curveTo(8.5f, 21f, 6f, 18f, 6f, 14.5f)
                    curveTo(6f, 11.5f, 8f, 9.5f, 9.5f, 8f)
                    curveTo(9.5f, 10.5f, 10.5f, 12f, 12f, 12.5f)
                    curveTo(11.5f, 9f, 12f, 6f, 12f, 3f)
                    close()
                },
            "tree" to
                icon("tree") {
                    moveTo(12f, 3f)
                    lineTo(6.5f, 10f)
                    lineTo(9f, 10f)
                    lineTo(5f, 15.5f)
                    lineTo(19f, 15.5f)
                    lineTo(15f, 10f)
                    lineTo(17.5f, 10f)
                    close()
                    moveTo(12f, 15.5f)
                    lineTo(12f, 20.5f)
                },
            "crown" to
                icon("crown") {
                    moveTo(4f, 8f)
                    lineTo(7.5f, 15.5f)
                    lineTo(16.5f, 15.5f)
                    lineTo(20f, 8f)
                    lineTo(15f, 11.5f)
                    lineTo(12f, 5.5f)
                    lineTo(9f, 11.5f)
                    close()
                    moveTo(7.5f, 19f)
                    lineTo(16.5f, 19f)
                },
            "anchor" to
                icon("anchor") {
                    circle(12f, 5.5f, 2f)
                    moveTo(12f, 7.5f)
                    lineTo(12f, 20.5f)
                    moveTo(8f, 11f)
                    lineTo(16f, 11f)
                    moveTo(4.5f, 13.5f)
                    curveTo(5f, 18f, 8.5f, 20.5f, 12f, 20.5f)
                    curveTo(15.5f, 20.5f, 19f, 18f, 19.5f, 13.5f)
                },
            "heart" to
                icon("heart") {
                    moveTo(12f, 20f)
                    curveTo(6f, 15.5f, 3.5f, 12.5f, 3.5f, 9f)
                    curveTo(3.5f, 6.5f, 5.5f, 4.5f, 8f, 4.5f)
                    curveTo(9.8f, 4.5f, 11.2f, 5.5f, 12f, 7f)
                    curveTo(12.8f, 5.5f, 14.2f, 4.5f, 16f, 4.5f)
                    curveTo(18.5f, 4.5f, 20.5f, 6.5f, 20.5f, 9f)
                    curveTo(20.5f, 12.5f, 18f, 15.5f, 12f, 20f)
                    close()
                },
            "moon" to
                icon("moon") {
                    moveTo(15.5f, 3.8f)
                    curveTo(10.5f, 4.8f, 7.8f, 9.5f, 9f, 14f)
                    curveTo(10.2f, 18.2f, 14.5f, 20.6f, 19f, 19.4f)
                    curveTo(17.3f, 20.9f, 15f, 21.6f, 12.5f, 21.4f)
                    curveTo(7.5f, 21f, 3.8f, 16.6f, 4.2f, 11.6f)
                    curveTo(4.6f, 7f, 9f, 3.4f, 15.5f, 3.8f)
                    close()
                },
            "snowflake" to
                icon("snowflake") {
                    repeat(3) { i ->
                        val a = PI / 2 + i * PI / 3
                        val dx = (8.5 * cos(a)).toFloat()
                        val dy = (8.5 * sin(a)).toFloat()
                        moveTo(12f - dx, 12f - dy)
                        lineTo(12f + dx, 12f + dy)
                    }
                    moveTo(9.5f, 4.5f)
                    lineTo(12f, 6.5f)
                    lineTo(14.5f, 4.5f)
                    moveTo(9.5f, 19.5f)
                    lineTo(12f, 17.5f)
                    lineTo(14.5f, 19.5f)
                },
            "fish" to
                icon("fish") {
                    moveTo(3.5f, 12f)
                    curveTo(7f, 7f, 13f, 6.5f, 17f, 12f)
                    curveTo(13f, 17.5f, 7f, 17f, 3.5f, 12f)
                    close()
                    moveTo(17f, 12f)
                    lineTo(21f, 8.5f)
                    lineTo(21f, 15.5f)
                    close()
                    circle(8f, 11f, 0.6f)
                },
            "bird" to
                icon("bird") {
                    moveTo(3f, 13f)
                    curveTo(6f, 9f, 9f, 9f, 12f, 12.5f)
                    curveTo(15f, 9f, 18f, 9f, 21f, 13f)
                },
        )
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = cx + r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = cx - r, y1 = cy)
        close()
    }

    private fun PathBuilder.star(cx: Float, cy: Float, outer: Float, inner: Float) {
        repeat(STAR_POINTS * 2) { i ->
            val r = if (i % 2 == 0) outer else inner
            val a = -PI / 2 + i * PI / STAR_POINTS
            val x = cx + r * cos(a).toFloat()
            val y = cy + r * sin(a).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private fun icon(name: String, block: PathBuilder.() -> Unit) =
        ImageVector
            .Builder(name, 24.dp, 24.dp, 24f, 24f)
            .path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            ).build()

    private const val STAR_POINTS = 5
}
