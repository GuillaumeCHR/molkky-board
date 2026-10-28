package com.yogistudio.molkkyboard.feature.game

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.component.MBDragHandle
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.component.PIN_FORMATION
import com.yogistudio.molkkyboard.core.designsystem.component.PinGrid
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

private val ROW_NAMES =
    intArrayOf(R.string.placement_row_1, R.string.placement_row_2, R.string.placement_row_3, R.string.placement_row_4)

/** Rangée (0 = devant) et position dans la rangée, pour la description d'une quille. */
@StringRes
private fun rowName(pin: Int): Int = ROW_NAMES[PIN_FORMATION.size - 1 - PIN_FORMATION.indexOfFirst { pin in it }]

@StringRes
private fun sideName(pin: Int): Int {
    val row = PIN_FORMATION.first { pin in it }
    val index = row.indexOf(pin)
    return when {
        index == 0 -> R.string.placement_left
        index == row.lastIndex -> R.string.placement_right
        row.size % 2 == 1 -> R.string.placement_center
        index == 1 -> R.string.placement_center_left
        else -> R.string.placement_center_right
    }
}

/**
 * Placement des quilles : la formation complète d'emblée, vue depuis la ligne de lancer (retour du test : le pas à
 * pas était trop long), puis la distance de lancer. Chaque quille annonce sa position à TalkBack.
 */
@Composable
fun PlacementGuide(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MBDimens.SmallGap)) {
        Text(stringResource(R.string.placement_caption), style = MaterialTheme.typography.bodyLarge)
        Box(Modifier.fillMaxWidth().height(FORMATION_HEIGHT)) {
            PinGrid(
                knocked = emptySet(),
                onToggle = { _, _ -> },
                pinModifier = { pin ->
                    Modifier.clearAndSetSemantics { contentDescription = context.placementLabel(pin) }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(stringResource(R.string.placement_distance), style = MaterialTheme.typography.bodyLarge)
    }
}

private fun android.content.Context.placementLabel(pin: Int): String {
    val row = getString(rowName(pin))
    val side = getString(sideName(pin))
    return getString(R.string.placement_pin, pin, row, side)
}

/** Placement en surimpression pendant une partie (bouton « Quilles »), sans quitter la saisie. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlacementSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        dragHandle = { MBDragHandle() },
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MBDimens.ScreenPadding)
                .padding(bottom = MBDimens.ScreenPadding)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
        ) {
            Text(
                stringResource(R.string.placement_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            PlacementGuide()
        }
    }
}

/** Placement en écran plein (raccourci de l'icône « Placer les quilles »). */
@Composable
fun PlacementScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.placement_title), onBack)
        PlacementGuide(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MBDimens.ScreenPadding)
                .padding(bottom = MBDimens.ScreenPadding)
                .navigationBarsPadding(),
        )
    }
}

private val FORMATION_HEIGHT = 300.dp
