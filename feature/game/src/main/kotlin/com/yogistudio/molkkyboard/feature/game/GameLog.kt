package com.yogistudio.molkkyboard.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionButton
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionKind
import com.yogistudio.molkkyboard.core.designsystem.component.FitToScreen
import com.yogistudio.molkkyboard.core.designsystem.component.MBDragHandle
import com.yogistudio.molkkyboard.core.designsystem.component.PinGrid
import com.yogistudio.molkkyboard.core.designsystem.component.QuickPad
import com.yogistudio.molkkyboard.core.designsystem.component.QuickPadMode
import com.yogistudio.molkkyboard.core.designsystem.component.RoundBadge
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.scoring.FaultKind
import com.yogistudio.molkkyboard.core.scoring.PointsKind
import com.yogistudio.molkkyboard.core.scoring.ThrowResult

/**
 * Déroulé de la partie : liste chronologique des événements, groupée par tour. Toucher un lancer ouvre son
 * éditeur (correction ou suppression) ; tout est alors recalculé et les animations suivent (victoire qui apparaît
 * ou disparaît, élimination défaite).
 *
 * @param message retour de la dernière modification (« Lancer corrigé… » ou la raison d'un refus).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogSheet(
    state: GameUiState,
    actions: GameActions,
    message: String?,
    onDismiss: () -> Unit,
    initialEditing: LogItemUi? = null,
) {
    var editing by remember { mutableStateOf(initialEditing) }
    var deleting by remember { mutableStateOf<LogItemUi?>(null) }
    ModalBottomSheet(
        dragHandle = { MBDragHandle() },
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        val item = editing
        if (item == null) {
            LogList(state, message, onEdit = { if (state.canEditLog) editing = it })
        } else {
            ThrowEditor(
                item = item,
                onSave = {
                    actions.onCorrect(item.id, it)
                    editing = null
                },
                onCancel = { editing = null },
                onDelete = { deleting = item },
            )
        }
    }
    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.log_delete_title)) },
            text = { Text(stringResource(R.string.log_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    actions.onDelete(item.id)
                    deleting = null
                    editing = null
                }) {
                    Text(stringResource(R.string.log_delete_confirm), color = MolkkyBoardTheme.colors.coralText)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.log_cancel)) } },
        )
    }
}

@Composable
private fun LogList(state: GameUiState, message: String?, onEdit: (LogItemUi) -> Unit) {
    // On ouvre le déroulé sur les derniers lancers.
    val lines = state.log.sumOf { it.items.size + 1 }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (lines - 1).coerceAtLeast(0))
    Column(Modifier.padding(horizontal = MBDimens.ScreenPadding)) {
        Text(
            stringResource(R.string.log_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(if (state.canEditLog) R.string.log_hint else R.string.log_hint_readonly),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (message != null) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MolkkyBoardTheme.colors.saffron.copy(alpha = MESSAGE_ALPHA),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp))
            }
        }
        if (state.log.isEmpty()) {
            Text(
                stringResource(R.string.log_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 32.dp),
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.log.forEach { round ->
                item(key = "round-${round.round}") {
                    Row(
                        Modifier.padding(top = 16.dp, bottom = 4.dp).semantics { heading() },
                    ) { RoundBadge(round.round) }
                }
                items(round.items, key = { it.id.value }) { item ->
                    LogRow(item, onClick = if (item.deletable && state.canEditLog) ({ onEdit(item) }) else null)
                }
                if (round.standings.isNotEmpty()) {
                    item(key = "standings-${round.round}") { Standings(round) }
                }
            }
        }
    }
}

@Composable
private fun LogRow(item: LogItemUi, onClick: (() -> Unit)?) {
    val colors = MolkkyBoardTheme.colors.teams
    val clickLabel = stringResource(R.string.log_edit)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (onClick !=
                        null
                    ) {
                        Modifier.clickable(onClickLabel = clickLabel, onClick = onClick)
                    } else {
                        Modifier
                    },
                ).heightIn(min = MBDimens.MinTouch)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MBDimens.Gap),
    ) {
        val name = item.teamName ?: item.playerName
        val color = colors[item.colorIndex % colors.size]
        Box(
            Modifier.size(36.dp).background(
                if (item.teamName !=
                    null
                ) {
                    color.fill
                } else {
                    MaterialTheme.colorScheme.outline
                },
                CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name?.take(1)?.uppercase().orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = color.onFill,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(lineTitle(item), style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text(
                lineDetail(item),
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if ((item.kind == LogKind.THROW && item.points == 0) || item.kind == LogKind.FAULT) {
                        MolkkyBoardTheme.colors.coralText
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
        val after = item.scoreAfter
        if (after != null && item.kind != LogKind.WRONG_TEAM) {
            val before = item.scoreBefore
            Text(
                if (before != null && item.kind == LogKind.THROW) {
                    stringResource(R.string.log_score_change, before, after)
                } else {
                    stringResource(R.string.log_score_after, after)
                },
                style = MolkkyBoardTheme.typography.scoreSmall,
                softWrap = false,
            )
        }
        if (onClick != null) Icon(MBIcons.ChevronRight, contentDescription = null)
    }
}

/** Récapitulatif des scores à la fin d'un tour, façon feuille de match. */
@Composable
private fun Standings(round: LogRound) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = STANDINGS_ALPHA),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        Text(
            stringResource(
                R.string.log_standings,
                round.round,
                round.standings.joinToString(" · ") { (name, score) -> "$name $score" },
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** « Bruno » ou « Les Bouleaux · Léa » ; pour un événement sans équipe, sa description. */
@Composable
private fun lineTitle(item: LogItemUi): String {
    val team = item.teamName
    val player = item.playerName
    return when (item.kind) {
        LogKind.JOINED -> stringResource(R.string.log_joined, player.orEmpty())
        LogKind.LEFT -> stringResource(R.string.log_left, player.orEmpty())
        LogKind.REORDER -> stringResource(R.string.log_reorder)
        LogKind.ABANDON -> stringResource(R.string.log_abandon)
        else -> if (player != null && team != null && player != team) "$team · $player" else team ?: player.orEmpty()
    }
}

@Composable
private fun lineDetail(item: LogItemUi): String =
    when (item.kind) {
        LogKind.THROW -> throwDetail(item)
        LogKind.FAULT -> stringResource(R.string.log_fault, faultLabel(item.faultKind))
        LogKind.WRONG_TEAM -> stringResource(R.string.log_wrong_team)
        else -> ""
    }

@Composable
private fun throwDetail(item: LogItemUi): String {
    val result = item.result ?: return ""
    if (item.points == 0) return stringResource(R.string.log_miss)
    val what =
        when (result) {
            is ThrowResult.Pins -> {
                if (result.pins.size == 1) {
                    stringResource(R.string.log_single_pin, result.pins.first())
                } else {
                    stringResource(R.string.log_pin_count, result.pins.size, result.pins.sorted().joinToString(", "))
                }
            }

            is ThrowResult.Points -> {
                if (result.kind == PointsKind.SINGLE_PIN) {
                    stringResource(R.string.log_quick_single, result.value)
                } else {
                    stringResource(R.string.log_quick_count, result.value)
                }
            }
        }
    val before = item.scoreBefore
    val after = item.scoreAfter
    // Le score n'a pas progressé d'autant : c'est un dépassement (retour à 25, rebond…).
    return if (before != null && after != null && after != before + item.points) {
        stringResource(R.string.log_points_overshoot, item.points, what, after)
    } else {
        stringResource(R.string.log_points, item.points, what)
    }
}

@Composable
private fun faultLabel(kind: FaultKind?): String =
    stringResource(
        when (kind) {
            FaultKind.TRESPASS -> R.string.log_fault_trespass
            FaultKind.DROPPED -> R.string.log_fault_dropped
            FaultKind.TIMEOUT -> R.string.log_fault_timeout
            FaultKind.ABSENT -> R.string.log_fault_absent
            FaultKind.WRONG_PLAYER -> R.string.log_fault_wrong_player
            null -> R.string.log_fault_other
        },
    )

/**
 * Saisie en cours dans l'éditeur d'un lancer passé, avec les mêmes règles que l'écran de jeu : « Raté » exclut
 * les quilles, et une seule quille ne se saisit pas en « nombre de quilles ».
 */
internal data class ThrowDraft(
    val mode: InputMode,
    val pins: Set<Int> = emptySet(),
    val quick: Int? = null,
    val quickMode: QuickPadMode = QuickPadMode.SinglePin,
    val miss: Boolean = false,
) {
    /** Le lancer saisi, ou `null` tant que rien n'est choisi. */
    val result: ThrowResult?
        get() =
            when {
                miss -> {
                    ThrowResult.Pins(emptyList())
                }

                mode == InputMode.PINS -> {
                    if (pins.isEmpty()) null else ThrowResult.Pins(pins.sorted())
                }

                quick == null -> {
                    null
                }

                quick == 0 -> {
                    ThrowResult.Pins(emptyList())
                }

                else -> {
                    ThrowResult.Points(
                        quick,
                        if (quickMode ==
                            QuickPadMode.SinglePin
                        ) {
                            PointsKind.SINGLE_PIN
                        } else {
                            PointsKind.PIN_COUNT
                        },
                    )
                }
            }

    fun withMode(mode: InputMode) = copy(mode = mode, miss = false)

    fun withPin(pin: Int, knocked: Boolean) = copy(pins = if (knocked) pins + pin else pins - pin, miss = false)

    fun withQuick(value: Int?) = copy(quick = value, miss = false)

    fun withQuickMode(mode: QuickPadMode) =
        copy(quickMode = mode, quick = if (mode == QuickPadMode.PinCount && quick == 1) null else quick)

    fun toggleMiss() = copy(miss = !miss)

    companion object {
        /** Saisie de départ : le lancer tel qu'il a été enregistré. */
        fun of(result: ThrowResult?): ThrowDraft =
            when (result) {
                is ThrowResult.Pins -> {
                    ThrowDraft(InputMode.PINS, pins = result.pins.toSet(), miss = result.pins.isEmpty())
                }

                is ThrowResult.Points -> {
                    ThrowDraft(
                        InputMode.QUICK,
                        quick = result.value,
                        quickMode =
                            if (result.kind ==
                                PointsKind.PIN_COUNT
                            ) {
                                QuickPadMode.PinCount
                            } else {
                                QuickPadMode.SinglePin
                            },
                    )
                }

                null -> {
                    ThrowDraft(InputMode.PINS)
                }
            }
    }
}

/** Éditeur d'un lancer passé : mêmes saisies que l'écran de jeu, rien n'est enregistré sans « Enregistrer ». */
@Composable
private fun ThrowEditor(
    item: LogItemUi,
    onSave: (ThrowResult) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    var draft by remember(item.id) { mutableStateOf(ThrowDraft.of(item.result)) }
    val chosen = draft.result
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = MBDimens.ScreenPadding)
            .padding(bottom = MBDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
    ) {
        Text(
            stringResource(R.string.log_editor_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.log_editor_subtitle, item.round, lineTitle(item)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EditorInput(draft, onChange = { draft = it })
        Row(horizontalArrangement = Arrangement.spacedBy(MBDimens.Gap)) {
            BigActionButton(
                text = stringResource(R.string.game_miss),
                onClick = { draft = draft.toggleMiss() },
                kind = if (draft.miss) BigActionKind.Miss else BigActionKind.Secondary,
                icon = MBIcons.Cross,
                modifier = Modifier.weight(1f),
            )
            BigActionButton(
                text = stringResource(R.string.log_save),
                onClick = { chosen?.let(onSave) },
                kind = BigActionKind.Validate,
                icon = MBIcons.Check,
                enabled = chosen != null && chosen != item.result,
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
                Text(stringResource(R.string.log_cancel))
            }
            TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
                Text(stringResource(R.string.log_delete), color = MolkkyBoardTheme.colors.coralText)
            }
        }
    }
}

/** Bascule Quilles / Rapide, puis la grille ou le pavé. */
@Composable
private fun EditorInput(draft: ThrowDraft, onChange: (ThrowDraft) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        InputMode.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = draft.mode == entry,
                onClick = { onChange(draft.withMode(entry)) },
                shape = SegmentedButtonDefaults.itemShape(index, InputMode.entries.size),
                modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                icon = {},
            ) {
                Text(
                    stringResource(
                        if (entry ==
                            InputMode.PINS
                        ) {
                            R.string.game_switch_to_pins
                        } else {
                            R.string.game_switch_to_quick
                        },
                    ),
                )
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(EDITOR_INPUT_HEIGHT), contentAlignment = Alignment.Center) {
        when (draft.mode) {
            InputMode.PINS -> {
                PinGrid(
                    knocked = if (draft.miss) emptySet() else draft.pins,
                    onToggle = { pin, knocked -> onChange(draft.withPin(pin, knocked)) },
                    modifier = Modifier.matchParentSize(),
                )
            }

            InputMode.QUICK -> {
                FitToScreen(Modifier.matchParentSize()) {
                    QuickPad(
                        selected = if (draft.miss) null else draft.quick,
                        onSelect = { onChange(draft.withQuick(it)) },
                        mode = draft.quickMode,
                        onModeChange = { onChange(draft.withQuickMode(it)) },
                    )
                }
            }
        }
    }
}

private const val MESSAGE_ALPHA = 0.3f
private const val STANDINGS_ALPHA = 0.6f
private val EDITOR_INPUT_HEIGHT = 340.dp
