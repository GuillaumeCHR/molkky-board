package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogistudio.molkkyboard.core.designsystem.R
import com.yogistudio.molkkyboard.core.designsystem.theme.MBMotion
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/** Inclinaison d'une quille couchée. */
private const val KNOCKED_TILT = 16f

/** Proportions de la quille dans sa zone (largeur du corps, hauteurs du biseau). */
private const val BODY_WIDTH = 0.62f
private const val BEVEL_LOW = 0.24f
private const val BEVEL_HIGH = 0.10f
private const val BEVEL_THICKNESS = 0.07f
private const val BOTTOM = 0.90f

/**
 * Une quille stylisée : cylindre au sommet biseauté, numérotée. Un tap la couche (elle bascule et
 * passe au safran), un second tap la relève.
 *
 * @param onToggle `null` pour une quille en lecture seule.
 * @param highlighted mise en avant (quille gagnante suggérée par le coach).
 */
@Composable
fun PinView(
    number: Int,
    knocked: Boolean,
    onToggle: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    size: Dp = 64.dp,
) {
    val colors = MolkkyBoardTheme.colors
    val reduced = MolkkyBoardTheme.reducedMotion
    val haptic = LocalHapticFeedback.current
    val tilt by animateFloatAsState(
        targetValue = if (knocked) KNOCKED_TILT else 0f,
        animationSpec = if (reduced) snap() else MBMotion.bouncy(),
        label = "tilt",
    )
    val body by animateColorAsState(
        if (knocked) colors.pinKnocked else colors.pinWood,
        MBMotion.gentle(),
        label = "body",
    )
    val top by animateColorAsState(
        if (knocked) colors.pinKnockedTop else colors.pinWoodTop,
        MBMotion.gentle(),
        label = "top",
    )
    val measurer = rememberTextMeasurer()
    val numberStyle =
        MolkkyBoardTheme.typography.pinNumber.copy(
            color = colors.pinNumber,
            fontSize =
                (
                    size.value *
                        0.34f
                ).sp,
        )
    val highlight = MaterialTheme.colorScheme.primary
    val description = stringResource(if (knocked) R.string.ds_pin_knocked else R.string.ds_pin_standing, number)
    val state = stringResource(if (knocked) R.string.ds_pin_state_knocked else R.string.ds_pin_state_standing)

    val interaction =
        if (onToggle != null) {
            Modifier.toggleable(value = knocked, enabled = enabled, role = Role.Checkbox) { value ->
                haptic.performHapticFeedback(if (value) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onToggle(value)
            }
        } else {
            Modifier
        }
    Box(
        modifier =
            modifier
                .size(width = size, height = size * 1.3f)
                .then(interaction)
                .semantics {
                    contentDescription = description
                    stateDescription = state
                }.alpha(if (enabled) 1f else 0.35f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawShadow(knocked)
            rotate(degrees = tilt, pivot = Offset(this.size.width / 2, this.size.height * BOTTOM)) {
                drawPin(body, top, colors.pinWoodShade, colors.outline, colors.outlineWidth.toPx())
                if (highlighted) drawHighlight(highlight)
                val label = measurer.measure(number.toString(), numberStyle)
                val center = Offset(this.size.width / 2, this.size.height * 0.58f)
                drawText(label, topLeft = center - Offset(label.size.width / 2f, label.size.height / 2f))
            }
        }
    }
}

private fun DrawScope.bodyPath(): Path {
    val w = size.width * BODY_WIDTH
    val left = (size.width - w) / 2
    val right = left + w
    val bottom = size.height * BOTTOM
    val radius = w * 0.22f
    return Path().apply {
        moveTo(left, size.height * BEVEL_LOW)
        lineTo(right, size.height * BEVEL_HIGH)
        lineTo(right, bottom - radius)
        quadraticTo(right, bottom, right - radius, bottom)
        lineTo(left + radius, bottom)
        quadraticTo(left, bottom, left, bottom - radius)
        close()
    }
}

private fun DrawScope.drawPin(body: Color, top: Color, shade: Color, outline: Color, outlineWidth: Float) {
    val w = size.width * BODY_WIDTH
    val left = (size.width - w) / 2
    val right = left + w
    val path = bodyPath()
    drawPath(path, body)
    // Ombre propre sur le flanc droit : volume du cylindre, en aplat.
    clipPath(path) {
        drawRect(
            color = shade.copy(alpha = 0.45f),
            topLeft = Offset(right - w * 0.18f, 0f),
            size = Size(w * 0.18f, size.height),
        )
    }
    // Face du biseau (bois de bout), plus claire.
    val bevel =
        Path().apply {
            moveTo(left, size.height * BEVEL_LOW)
            lineTo(right, size.height * BEVEL_HIGH)
            lineTo(right, size.height * (BEVEL_HIGH + BEVEL_THICKNESS))
            lineTo(left, size.height * (BEVEL_LOW + BEVEL_THICKNESS))
            close()
        }
    drawPath(bevel, top)
    drawPath(path, outline, style = Stroke(width = outlineWidth))
}

private fun DrawScope.drawHighlight(color: Color) {
    val inset = 2.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - 2 * inset, size.height - 2 * inset),
        cornerRadius = CornerRadius(16.dp.toPx()),
        style = Stroke(width = 3.dp.toPx()),
    )
}

private fun DrawScope.drawShadow(knocked: Boolean) {
    val w = size.width * BODY_WIDTH * if (knocked) 1.15f else 1f
    val y = size.height * BOTTOM
    drawOval(
        color = Color.Black.copy(alpha = 0.12f),
        topLeft = Offset((size.width - w) / 2, y - 3.dp.toPx()),
        size = Size(w, 6.dp.toPx()),
    )
}
