package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MBMotion
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/** Rôle visuel d'un gros bouton d'action. */
enum class BigActionKind {
    /** Valider le lancer : safran. */
    Validate,

    /** Raté : corail. */
    Miss,

    /** Action principale hors partie (« Nouvelle partie ») : vert forêt. */
    Primary,

    /** Action secondaire (« Rejoindre ») : contour. */
    Secondary,
}

/** Gros bouton en pilule (72 dp), pensé pour être pressé au pouce en extérieur. */
@Composable
fun BigActionButton(
    text: String,
    onClick: () -> Unit,
    kind: BigActionKind,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = kind.colors()
    val border = kind.border()
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = if (MolkkyBoardTheme.reducedMotion) snap() else MBMotion.snappy(),
        label = "press",
    )
    Surface(
        onClick = {
            haptic.performHapticFeedback(kind.haptic)
            onClick()
        },
        modifier =
            modifier
                .defaultMinSize(minHeight = MBDimens.ActionButtonHeight)
                .scale(scale),
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) container else scheme.surfaceVariant,
        contentColor = if (enabled) content else scheme.onSurfaceVariant,
        border = border,
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            val style = MolkkyBoardTheme.typography.action
            // Une seule ligne : avec une grande police système, le libellé rétrécit au lieu de passer à la ligne.
            BasicText(
                text = text,
                modifier = Modifier.weight(1f, fill = false),
                style = style.copy(color = LocalContentColor.current, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = style.fontSize),
            )
        }
    }
}

@Composable
private fun BigActionKind.colors(): Pair<Color, Color> {
    val colors = MolkkyBoardTheme.colors
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        BigActionKind.Validate -> colors.saffron to colors.onSaffron
        BigActionKind.Miss -> colors.coral to colors.onCoral
        BigActionKind.Primary -> scheme.primary to scheme.onPrimary
        BigActionKind.Secondary -> Color.Transparent to scheme.primary
    }
}

/** Contour : toujours pour l'action secondaire, et sur tous les boutons en « Plein soleil ». */
@Composable
private fun BigActionKind.border(): BorderStroke? {
    val colors = MolkkyBoardTheme.colors
    return when {
        this == BigActionKind.Secondary -> {
            BorderStroke(
                maxOf(colors.outlineWidth, 2.dp),
                MaterialTheme.colorScheme.primary,
            )
        }

        colors.outlineWidth > 1.dp -> {
            BorderStroke(colors.outlineWidth, colors.outline)
        }

        else -> {
            null
        }
    }
}

private val BigActionKind.haptic: HapticFeedbackType
    get() =
        when (this) {
            BigActionKind.Validate -> HapticFeedbackType.Confirm
            BigActionKind.Miss -> HapticFeedbackType.Reject
            else -> HapticFeedbackType.ContextClick
        }
