package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.R
import com.yogistudio.molkkyboard.core.designsystem.theme.MBMotion
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/**
 * Ratés consécutifs : des points qui se remplissent en corail (0/3 → 3/3).
 *
 * @param warning un raté de plus serait sanctionné : les points pulsent trois fois en corail (avertissement).
 */
@Composable
fun MissIndicator(
    misses: Int,
    modifier: Modifier = Modifier,
    limit: Int = 3,
    dotSize: Dp = 14.dp,
    warning: Boolean = false,
) {
    val description =
        if (warning) {
            stringResource(R.string.ds_misses_warning, misses, limit)
        } else {
            stringResource(R.string.ds_misses, misses, limit)
        }
    val pulse = remember { Animatable(1f) }
    val reduced = MolkkyBoardTheme.reducedMotion
    LaunchedEffect(warning, misses) {
        if (!warning || reduced) return@LaunchedEffect
        // Trois pulsations puis repos : une animation infinie coûterait de la batterie.
        repeat(WARNING_PULSES) {
            pulse.animateTo(WARNING_SCALE, MBMotion.snappy())
            pulse.animateTo(1f, MBMotion.bouncy())
        }
    }
    Row(
        modifier = modifier.scale(pulse.value).clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(dotSize / 2),
    ) {
        repeat(limit) { index -> MissDot(filled = index < misses, size = dotSize) }
    }
}

@Composable
private fun MissDot(filled: Boolean, size: Dp) {
    val colors = MolkkyBoardTheme.colors
    val reduced = MolkkyBoardTheme.reducedMotion
    val fill by animateColorAsState(if (filled) colors.coral else Color.Transparent, MBMotion.gentle(), label = "fill")
    val scale by animateFloatAsState(
        targetValue = if (filled) 1f else 0.85f,
        animationSpec = if (reduced) snap() else MBMotion.bouncy(),
        label = "scale",
    )
    val border = if (filled) colors.coral else MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .size(size)
            .scale(scale)
            .background(fill, CircleShape)
            .border(maxOf(colors.outlineWidth, 2.dp), border, CircleShape),
    )
}

private const val WARNING_PULSES = 3
private const val WARNING_SCALE = 1.25f
