package com.yogistudio.molkkyboard.core.designsystem.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.yogistudio.molkkyboard.core.designsystem.theme.Inter

// Logo de l'éditeur Yogi Studio, reproduit à partir de docs/brand/yogi-studio-icon.svg (repère 512 × 512).
// Il est dessiné en code plutôt qu'importé pour pouvoir l'animer : le cercle safran se trace, puis le
// personnage apparaît avec un rebond (intro). Nom de marque : non traduit.

private const val VIEWBOX = 512f
private const val RING_START = -62f
private const val RING_SWEEP = 314.2f

private val Forest = Color(0xFF1F4D3A)
private val Saffron = Color(0xFFF2A93B)
private val Birch = Color(0xFFF4EBDD)
private val Ink = Color(0xFF1B1B18)
private val WoodLight = Color(0xFFE9D3AE)
private val WoodGrain = Color(0xFFE2C99E)
private val Coral = Color(0xFFE8604C)

private fun path(data: String): Path = PathParser().parsePathString(data).toPath()

private object YogiPaths {
    val sparks =
        listOf(
            path("M372,104 L392,96 L398,114 L380,120 Z") to 1f,
            path("M404,146 L418,142 L420,156 Z") to 0.85f,
            path("M350,74 L362,70 L364,82 L354,84 Z") to 0.7f,
        )
    val arms = listOf(path("M212,252 Q170,292 188,334"), path("M300,252 Q342,292 324,334"))
    val body = path("M206,172 L306,142 L306,322 Q306,344 284,344 L228,344 Q206,344 206,322 Z")
    val bevel = path("M206,172 L306,142 L306,160 L206,190 Z")
    val grain = path("M220,290 Q256,284 292,290 M224,306 Q256,300 288,306")
    val face =
        listOf(path("M228,226 Q238,235 248,226"), path("M264,226 Q274,235 284,226"), path("M243,252 Q256,265 269,252"))
}

/**
 * Dessine le logo Yogi Studio dans la zone courante.
 *
 * @param ringProgress tracé du cercle safran, de 0 à 1.
 * @param characterScale échelle du personnage (0 = absent, 1 = taille normale, > 1 pendant le rebond).
 * @param side côté du logo en pixels ; [topLeft] son coin haut gauche (par défaut, centré dans la zone).
 */
fun DrawScope.drawYogiStudioMark(
    ringProgress: Float = 1f,
    characterScale: Float = 1f,
    alpha: Float = 1f,
    side: Float = minOf(size.width, size.height),
    topLeft: Offset = Offset((size.width - side) / 2, (size.height - side) / 2),
) {
    val unit = side / VIEWBOX
    val origin = topLeft
    withTransform({
        translate(origin.x, origin.y)
        scale(unit, unit, pivot = Offset.Zero)
    }) {
        drawRoundRect(
            Forest.copy(alpha = alpha),
            size = Size(VIEWBOX, VIEWBOX),
            cornerRadius =
                androidx.compose.ui.geometry
                    .CornerRadius(112f),
        )
        if (ringProgress > 0f) {
            drawArc(
                color = Saffron.copy(alpha = alpha),
                startAngle = RING_START,
                sweepAngle = RING_SWEEP * ringProgress.coerceAtMost(1f),
                useCenter = false,
                topLeft = Offset(256f - 186f, 262f - 186f),
                size = Size(372f, 372f),
                style = Stroke(width = 20f, cap = StrokeCap.Round),
            )
        }
        if (characterScale > 0f) {
            scale(characterScale, pivot = Offset(256f, 300f)) { drawCharacter(alpha) }
        }
    }
}

private fun DrawScope.drawCharacter(alpha: Float) {
    YogiPaths.sparks.forEach { (p, a) -> drawPath(p, Birch.copy(alpha = a * alpha)) }
    drawOval(Color.Black.copy(alpha = 0.22f * alpha), topLeft = Offset(138f, 376f), size = Size(236f, 24f))
    YogiPaths.arms.forEach { drawPath(it, WoodLight.copy(alpha = alpha), style = Stroke(22f, cap = StrokeCap.Round)) }
    drawPath(YogiPaths.body, Birch.copy(alpha = alpha))
    drawPath(YogiPaths.bevel, WoodGrain.copy(alpha = alpha))
    drawPath(YogiPaths.grain, WoodGrain.copy(alpha = alpha), style = Stroke(3f, cap = StrokeCap.Round))
    YogiPaths.face.forEach { drawPath(it, Ink.copy(alpha = alpha), style = Stroke(6f, cap = StrokeCap.Round)) }
    listOf(226f, 286f).forEach { drawCircle(Coral.copy(alpha = 0.55f * alpha), radius = 9f, center = Offset(it, 246f)) }
    // Jambes croisées : deux pilules inclinées, puis les pieds safran.
    listOf(-7f to Color(0xFFD9B98C), 7f to Color(0xFFCFAE7E)).forEach { (angle, color) ->
        rotate(angle, pivot = Offset(256f, 348f)) {
            drawRoundRect(
                color.copy(alpha = alpha),
                topLeft = Offset(160f, 326f),
                size = Size(192f, 44f),
                cornerRadius =
                    androidx.compose.ui.geometry
                        .CornerRadius(22f),
            )
        }
    }
    listOf(188f, 324f).forEach { drawCircle(Saffron.copy(alpha = alpha), radius = 14f, center = Offset(it, 336f)) }
}

/** Logo carré Yogi Studio (statique). */
@Composable
fun YogiStudioMark(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Canvas(modifier.size(size).semantics { contentDescription = "Yogi Studio" }) { drawYogiStudioMark() }
}

/** Logo horizontal : le logo carré, « Yogi » en vert forêt et « STUDIO » en safran espacé. */
@Composable
fun YogiStudioLogo(modifier: Modifier = Modifier, markSize: Dp = 64.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        YogiStudioMark(size = markSize)
        Column {
            Text(
                "Yogi",
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = (markSize.value * 0.55f).sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-0.03).em,
            )
            Text(
                "STUDIO",
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = (markSize.value * 0.19f).sp,
                color = Saffron,
                letterSpacing = 0.5.em,
            )
        }
    }
}
