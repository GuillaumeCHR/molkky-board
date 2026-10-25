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
