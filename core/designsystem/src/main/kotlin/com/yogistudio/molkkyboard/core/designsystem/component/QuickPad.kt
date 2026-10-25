package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogistudio.molkkyboard.core.designsystem.R
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MBMotion
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/** Saisie rapide : la valeur touchée est celle d'une quille seule, ou un nombre de quilles tombées. */
enum class QuickPadMode { SinglePin, PinCount }

/** Touches plus larges que hautes : le pavé laisse la place aux boutons Raté et Valider. */
private const val KEY_RATIO = 1.4f

private val PAD_ROWS = listOf(listOf(1, 2, 3, 4), listOf(5, 6, 7, 8), listOf(9, 10, 11, 12))

/**
 * Pavé de saisie rapide : 13 gros boutons (0 à 12) dans le style des quilles, et un sélecteur
 * « quille seule / nombre de quilles ». Rien n'est enregistré ici : la sélection attend « Valider ».
 * En mode « nombre de quilles », 1 n'a pas de sens (une quille seule vaut sa valeur) et est désactivé.
 */
@Composable
fun QuickPad(
    selected: Int?,
    onSelect: (Int?) -> Unit,
    mode: QuickPadMode,
    onModeChange: (QuickPadMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MBDimens.SmallGap)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            QuickPadMode.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = mode == entry,
                    onClick = { onModeChange(entry) },
                    shape = SegmentedButtonDefaults.itemShape(index, QuickPadMode.entries.size),
                    modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                    colors =
                        SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    icon = {},
                ) {
                    val label =
                        if (entry ==
                            QuickPadMode.SinglePin
                        ) {
                            R.string.ds_quickpad_single
                        } else {
                            R.string.ds_quickpad_count
                        }
                    Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        PAD_ROWS.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap)) {
                row.forEach { value ->
                    PadKey(value, selected == value, mode, onSelect, Modifier.weight(1f))
                }
            }
        }
        PadKey(0, selected == 0, mode, onSelect, Modifier.fillMaxWidth(), wide = true)
    }
}

@Composable
private fun PadKey(
    value: Int,
    isSelected: Boolean,
    mode: QuickPadMode,
    onSelect: (Int?) -> Unit,
    modifier: Modifier,
    wide: Boolean = false,
) {
    val colors = MolkkyBoardTheme.colors
    val haptic = LocalHapticFeedback.current
    val enabled = !(mode == QuickPadMode.PinCount && value == 1)
    val container by animateColorAsState(
        if (isSelected) colors.pinKnocked else colors.pinWood,
        MBMotion.gentle(),
        label = "key",
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1f,
        animationSpec = if (MolkkyBoardTheme.reducedMotion) snap() else MBMotion.bouncy(),
        label = "keyScale",
    )
    val description =
        when {
            value == 0 -> stringResource(R.string.ds_quickpad_value_miss)
            mode == QuickPadMode.SinglePin -> stringResource(R.string.ds_quickpad_value_single, value)
            else -> stringResource(R.string.ds_quickpad_value_count, value)
        }
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onSelect(if (isSelected) null else value)
        },
        enabled = enabled,
        modifier =
            modifier
                .heightIn(min = MBDimens.MinTouch)
                .then(if (wide) Modifier else Modifier.aspectRatio(KEY_RATIO))
                .scale(scale)
                .semantics {
                    contentDescription = description
                    selected = isSelected
                    role = Role.RadioButton
                },
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) container else colors.pinWood.copy(alpha = 0.35f),
        contentColor = colors.pinNumber,
        border = if (colors.outlineWidth > 1.dp) BorderStroke(colors.outlineWidth, colors.outline) else null,
    ) {
        BasicText(
            text = value.toString(),
            modifier = Modifier.wrapContentSize(Alignment.Center),
            style = MolkkyBoardTheme.typography.scoreSmall.copy(color = colors.pinNumber, textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 30.sp),
        )
    }
}
