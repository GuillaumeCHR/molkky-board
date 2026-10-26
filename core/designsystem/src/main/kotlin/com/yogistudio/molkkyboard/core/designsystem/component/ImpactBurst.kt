package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogistudio.molkkyboard.core.designsystem.theme.BricolageGrotesque
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bulle éclatée de l'onomatopée « 12 ! » : étoile à pointes arrondies safran cerclée de corail, en aplats
 * (ni trame ni encrage). Réutilisée par l'intro (le strike) et par l'animation « 12 ! » en jeu.
 *
 * @param caption petit bandeau sous la bulle (« Strike ! »), ou rien.
 */
@Composable
fun ImpactBurst(
    text: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    size: Dp = 220.dp,
) {
    val colors = MolkkyBoardTheme.colors
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((-18).dp),
    ) {
        Box(
            Modifier
                .size(size)
                .drawWithCache {
                    val path =
                        starPath(Offset(this.size.width / 2f, this.size.height / 2f), this.size.minDimension / 2f)
                    val rounded = PathEffect.cornerPathEffect(this.size.minDimension * CORNER_RATIO)
                    val stroke =
                        Stroke(
                            width = this.size.minDimension * STROKE_RATIO,
                            join = StrokeJoin.Round,
                            pathEffect = rounded,
                        )
                    onDrawBehind {
                        drawPath(path, colors.coral, style = stroke)
                        drawPath(path, colors.saffron, style = Fill)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * TEXT_RATIO).sp,
                color = colors.onSaffron,
                maxLines = 1,
            )
        }
        if (caption != null) {
            Surface(shape = CircleShape, color = colors.coral, contentColor = colors.onCoral) {
                Text(
                    caption,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** Étoile à [POINTS] branches, irrégulière (une branche sur deux plus courte) pour l'effet « éclaté ». */
private fun starPath(center: Offset, radius: Float): Path {
    val outer = radius * OUTER_RATIO
    val inner = radius * INNER_RATIO
    return Path().apply {
        for (i in 0 until POINTS * 2) {
            val angle = PI * i / POINTS - PI / 2
            val r =
                when {
                    i % 2 == 1 -> inner
                    i % 4 == 0 -> outer
                    else -> outer * SHORT_POINT_RATIO
                }
            val x = center.x + (r * cos(angle)).toFloat()
            val y = center.y + (r * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

private const val POINTS = 12
private const val OUTER_RATIO = 0.92f
private const val INNER_RATIO = 0.68f
private const val SHORT_POINT_RATIO = 0.88f
private const val CORNER_RATIO = 0.04f
private const val STROKE_RATIO = 0.05f
private const val TEXT_RATIO = 0.3f
