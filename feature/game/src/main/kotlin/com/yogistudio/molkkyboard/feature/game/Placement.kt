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
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.component.PIN_FORMATION
import com.yogistudio.molkkyboard.core.designsystem.component.PinGrid
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/** Ordre de pose : de la rangée de devant (1, 2) à celle du fond (7, 9, 8), de gauche à droite. */
internal val PLACEMENT_ORDER: List<Int> = PIN_FORMATION.reversed().flatten()

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

/** « Quille 12, troisième rangée, au centre droit ». */
@Composable
internal fun pinPosition(pin: Int): String =
    stringResource(R.string.placement_pin, pin, stringResource(rowName(pin)), stringResource(sideName(pin)))

/**
 * Placement des quilles pas à pas : les quilles apparaissent une à une à leur place, avec leur position en clair
 * (lue par TalkBack, et à voix haute sur demande). « Tout afficher » montre la formation complète ; la vue de côté
 * aide à aligner les rangées et à placer la ligne de lancer. [startWithAll] : la formation complète d'abord (règles).
 */
@Composable
fun PlacementGuide(modifier: Modifier = Modifier, startWithAll: Boolean = false) {
    var step by rememberSaveable { mutableIntStateOf(if (startWithAll) PLACEMENT_ORDER.size else 0) }
    var side by rememberSaveable { mutableStateOf(false) }
    var speak by rememberSaveable { mutableStateOf(false) }
    val all = step >= PLACEMENT_ORDER.size
    val current = PLACEMENT_ORDER.getOrNull(step)
    val visible = if (all) PLACEMENT_ORDER.toSet() else PLACEMENT_ORDER.take(step + 1).toSet()
    val announcer = rememberVoiceAnnouncer(speak)
    val context = LocalContext.current
    val spoken = current?.let { pinPosition(it) }
    LaunchedEffect(spoken, announcer) {
        if (spoken != null) announcer?.speak(spoken, rate = 1f, volume = 1f)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MBDimens.SmallGap)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !side,
                onClick = { side = false },
                label = { Text(stringResource(R.string.placement_from_line)) },
            )
            FilterChip(
                selected = side,
                onClick = { side = true },
                label = { Text(stringResource(R.string.placement_from_side)) },
            )
            FilterChip(selected = speak, onClick = {
                speak = !speak
            }, label = { Text(stringResource(R.string.placement_read_aloud)) })
        }
        if (side) {
            SideView(Modifier.fillMaxWidth().height(SIDE_HEIGHT))
            Text(stringResource(R.string.placement_side_caption), style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                if (all) stringResource(R.string.placement_all_shown) else spoken.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Box(Modifier.fillMaxWidth().height(FORMATION_HEIGHT)) {
                PinGrid(
                    knocked = emptySet(),
                    onToggle = { _, _ -> },
                    visible = visible,
                    highlighted = setOfNotNull(current),
                    pinModifier = { pin ->
                        if (pin in visible) {
                            Modifier.clearAndSetSemantics { contentDescription = context.placementLabel(pin) }
                        } else {
                            Modifier.clearAndSetSemantics {}
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                stringResource(R.string.placement_step, minOf(step + 1, PLACEMENT_ORDER.size), PLACEMENT_ORDER.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { step = (step - 1).coerceAtLeast(0) },
                    enabled = step > 0 && !all,
                    modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                ) { Text(stringResource(R.string.placement_previous)) }
                OutlinedButton(
                    onClick = { step = if (all) 0 else step + 1 },
                    modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                ) {
                    Text(
                        stringResource(
                            when {
                                all -> R.string.placement_restart
                                step == PLACEMENT_ORDER.lastIndex -> R.string.placement_done
                                else -> R.string.placement_next
                            },
                        ),
                    )
                }
                if (!all) {
                    TextButton(
                        onClick = { step = PLACEMENT_ORDER.size },
                        modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                    ) { Text(stringResource(R.string.placement_show_all)) }
                }
            }
        }
        Text(stringResource(R.string.placement_distance), style = MaterialTheme.typography.bodyLarge)
    }
}

private fun android.content.Context.placementLabel(pin: Int): String {
    val row = getString(rowName(pin))
    val side = getString(sideName(pin))
    return getString(R.string.placement_pin, pin, row, side)
}

/**
 * Vue de côté : la ligne de lancer à gauche, les 4 rangées serrées à droite (devant, puis jusqu'au fond), et la
 * distance entre les deux. Schéma fixe, sans animation.
 */
@Composable
private fun SideView(modifier: Modifier) {
    val wood = MolkkyBoardTheme.colors.pinWood
    val outline = MolkkyBoardTheme.colors.outline
    val line = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.placement_side_description)
    Canvas(modifier.semantics { contentDescription = description }) {
        val ground = size.height * GROUND
        drawLine(outline, Offset(0f, ground), Offset(size.width, ground), strokeWidth = 2.dp.toPx())
        // Ligne de lancer (mölkkaari) vue de côté : un petit repère au sol.
        drawRoundRect(
            line,
            Offset(size.width * LINE_X, ground - 6.dp.toPx()),
            Size(10.dp.toPx(), 12.dp.toPx()),
            CornerRadius(4.dp.toPx()),
        )
        val pinWidth = size.width * PIN_WIDTH
        val pinHeight = pinWidth * PIN_RATIO
        repeat(PIN_FORMATION.size) { row ->
            val x = size.width * FIRST_ROW_X + row * pinWidth * ROW_GAP
            drawRoundRect(wood, Offset(x, ground - pinHeight), Size(pinWidth, pinHeight), CornerRadius(pinWidth / 4))
            drawRoundRect(
                outline,
                Offset(x, ground - pinHeight),
                Size(pinWidth, pinHeight),
                CornerRadius(pinWidth / 4),
                style = Stroke(1.5.dp.toPx()),
            )
        }
        // Flèche de distance, de la ligne à la première rangée.
        val arrowY = ground + 14.dp.toPx()
        val start = size.width * LINE_X + 5.dp.toPx()
        val end = size.width * FIRST_ROW_X
        drawLine(line, Offset(start, arrowY), Offset(end, arrowY), strokeWidth = 2.dp.toPx())
        drawLine(line, Offset(end, arrowY), Offset(end - 8.dp.toPx(), arrowY - 6.dp.toPx()), strokeWidth = 2.dp.toPx())
        drawLine(line, Offset(end, arrowY), Offset(end - 8.dp.toPx(), arrowY + 6.dp.toPx()), strokeWidth = 2.dp.toPx())
    }
}

/** Placement en surimpression pendant une partie (bouton « Quilles »), sans quitter la saisie. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlacementSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
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
private val SIDE_HEIGHT = 140.dp
private const val GROUND = 0.7f
private const val LINE_X = 0.06f
private const val FIRST_ROW_X = 0.58f
private const val PIN_WIDTH = 0.07f
private const val PIN_RATIO = 1.6f
private const val ROW_GAP = 1.15f
