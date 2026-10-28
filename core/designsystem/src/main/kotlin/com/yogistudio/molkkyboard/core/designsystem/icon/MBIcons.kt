package com.yogistudio.molkkyboard.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Quelques icônes au trait épais, dessinées pour l'app (pas de dépendance aux icônes Material). */
object MBIcons {
    val Check: ImageVector by lazy {
        stroke("check") {
            moveTo(5f, 12.5f)
            lineTo(10f, 17.5f)
            lineTo(19f, 7f)
        }
    }

    val Cross: ImageVector by lazy {
        stroke("cross") {
            moveTo(6.5f, 6.5f)
            lineTo(17.5f, 17.5f)
            moveTo(17.5f, 6.5f)
            lineTo(6.5f, 17.5f)
        }
    }

    val Undo: ImageVector by lazy {
        stroke("undo") {
            moveTo(9f, 6f)
            lineTo(4.5f, 10.5f)
            lineTo(9f, 15f)
            moveTo(4.5f, 10.5f)
            lineTo(14f, 10.5f)
            curveTo(17.5f, 10.5f, 19.5f, 12.8f, 19.5f, 15.5f)
            curveTo(19.5f, 18f, 17.5f, 19.5f, 15f, 19.5f)
        }
    }

    val Home: ImageVector by lazy {
        stroke("home") {
            moveTo(3.5f, 11f)
            lineTo(12f, 4f)
            lineTo(20.5f, 11f)
            moveTo(6f, 9.5f)
            lineTo(6f, 20f)
            lineTo(18f, 20f)
            lineTo(18f, 9.5f)
            moveTo(10f, 20f)
            lineTo(10f, 14.5f)
            lineTo(14f, 14.5f)
            lineTo(14f, 20f)
        }
    }

    val History: ImageVector by lazy {
        stroke("history") {
            circle(12f, 12f, 8.5f)
            moveTo(12f, 7.5f)
            lineTo(12f, 12f)
            lineTo(15f, 14f)
        }
    }

    val Trophy: ImageVector by lazy {
        stroke("trophy") {
            moveTo(7f, 4f)
            lineTo(17f, 4f)
            lineTo(17f, 9f)
            curveTo(17f, 12f, 15f, 14f, 12f, 14f)
            curveTo(9f, 14f, 7f, 12f, 7f, 9f)
            close()
            moveTo(7f, 6f)
            lineTo(4.5f, 6f)
            curveTo(4.5f, 8.5f, 5.5f, 10f, 7.5f, 10.5f)
            moveTo(17f, 6f)
            lineTo(19.5f, 6f)
            curveTo(19.5f, 8.5f, 18.5f, 10f, 16.5f, 10.5f)
            moveTo(12f, 14f)
            lineTo(12f, 17.5f)
            moveTo(8f, 20f)
            lineTo(16f, 20f)
            moveTo(9.5f, 17.5f)
            lineTo(14.5f, 17.5f)
        }
    }

    /** Réglages : trois curseurs. */
    val Settings: ImageVector by lazy {
        stroke("settings") {
            listOf(6f to 9f, 12f to 15f, 18f to 8f).forEach { (y, x) ->
                moveTo(4f, y)
                lineTo(x - 2f, y)
                moveTo(x + 2f, y)
                lineTo(20f, y)
                circle(x, y, 2f)
            }
        }
    }

    val More: ImageVector by lazy {
        stroke("more") {
            listOf(5.5f, 12f, 18.5f).forEach { circle(12f, it, 0.05f) }
        }
    }

    val Back: ImageVector by lazy {
        stroke("back") {
            moveTo(19f, 12f)
            lineTo(5f, 12f)
            moveTo(11f, 6f)
            lineTo(5f, 12f)
            lineTo(11f, 18f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        stroke("chevron") {
            moveTo(9f, 5.5f)
            lineTo(15.5f, 12f)
            lineTo(9f, 18.5f)
        }
    }

    /** Corbeille : suppression d'une partie. */
    val Delete: ImageVector by lazy {
        stroke("delete") {
            moveTo(4.5f, 7f)
            lineTo(19.5f, 7f)
            moveTo(9.5f, 7f)
            lineTo(10f, 4.5f)
            lineTo(14f, 4.5f)
            lineTo(14.5f, 7f)
            moveTo(6.5f, 7f)
            lineTo(7.5f, 19.5f)
            lineTo(16.5f, 19.5f)
            lineTo(17.5f, 7f)
            moveTo(10.5f, 11f)
            lineTo(10.5f, 16f)
            moveTo(13.5f, 11f)
            lineTo(13.5f, 16f)
        }
    }

    /** Trois quilles biseautées : placement des quilles. */
    val Pins: ImageVector by lazy {
        stroke("pins") {
            moveTo(4f, 9f)
            lineTo(8f, 7.5f)
            lineTo(8f, 19f)
            lineTo(4f, 19f)
            close()
            moveTo(10f, 6f)
            lineTo(14f, 4.5f)
            lineTo(14f, 19f)
            lineTo(10f, 19f)
            close()
            moveTo(16f, 9f)
            lineTo(20f, 7.5f)
            lineTo(20f, 19f)
            lineTo(16f, 19f)
            close()
        }
    }

    /** Podium à trois marches : ouvre le tableau des scores. */
    val Podium: ImageVector by lazy {
        stroke("podium") {
            moveTo(3.5f, 20f)
            lineTo(20.5f, 20f)
            moveTo(4f, 20f)
            lineTo(4f, 13f)
            lineTo(9f, 13f)
            lineTo(9f, 8f)
            lineTo(15f, 8f)
            lineTo(15f, 15f)
            lineTo(20f, 15f)
            lineTo(20f, 20f)
            moveTo(9f, 13f)
            lineTo(9f, 20f)
            moveTo(15f, 15f)
            lineTo(15f, 20f)
            moveTo(12f, 3.5f)
            lineTo(12f, 5f)
        }
    }

    /** Livre ouvert : règles du jeu. */
    val Book: ImageVector by lazy {
        stroke("book") {
            moveTo(12f, 7f)
            curveTo(10f, 5.5f, 7f, 5f, 4f, 5.5f)
            lineTo(4f, 18f)
            curveTo(7f, 17.5f, 10f, 18f, 12f, 19.5f)
            curveTo(14f, 18f, 17f, 17.5f, 20f, 18f)
            lineTo(20f, 5.5f)
            curveTo(17f, 5f, 14f, 5.5f, 12f, 7f)
            close()
            moveTo(12f, 7f)
            lineTo(12f, 19.5f)
        }
    }

    /** Barres : statistiques. */
    val Chart: ImageVector by lazy {
        stroke("chart") {
            moveTo(5f, 19.5f)
            lineTo(19.5f, 19.5f)
            moveTo(7.5f, 16.5f)
            lineTo(7.5f, 12f)
            moveTo(12f, 16.5f)
            lineTo(12f, 6.5f)
            moveTo(16.5f, 16.5f)
            lineTo(16.5f, 9.5f)
        }
    }

    private fun androidx.compose.ui.graphics.vector.PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = cx + r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = cx - r, y1 = cy)
        close()
    }

    private fun stroke(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        ImageVector
            .Builder(name, 24.dp, 24.dp, 24f, 24f)
            .path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            ).build()
}
