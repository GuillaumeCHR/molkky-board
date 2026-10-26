package com.yogistudio.molkkyboard.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionButton
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionKind
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId

/**
 * Gestion des joueurs en cours de partie : changer l'ordre de passage (entre deux tours, avec confirmation),
 * ajouter un joueur en retard (il démarre à 0, en fin d'ordre) et retirer un joueur qui part.
 *
 * @param message retour de la dernière action (« Dana rejoint la partie… » ou la raison d'un refus).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayersSheet(
    state: GameUiState,
    actions: GameActions,
    message: String?,
    onDismiss: () -> Unit,
) {
    val original = state.teams.map { it.id }
    var order by remember(original) { mutableStateOf(original) }
    var confirmOrder by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<Pair<PlayerId, String>?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    val editable = !state.finished && !state.abandoned
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = MBDimens.ScreenPadding)
                .padding(bottom = MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
        ) {
            Text(
                stringResource(R.string.players_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (message != null) MessageBanner(message)
            SectionLabel(R.string.players_order)
            Text(
                if (state.betweenRounds) {
                    stringResource(R.string.players_order_hint)
                } else {
                    stringResource(R.string.players_order_locked, state.round)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            order.forEachIndexed { index, id ->
                val team = state.teams.firstOrNull { it.id == id } ?: return@forEachIndexed
                OrderRow(
                    index = index,
                    team = team,
                    canMove = editable && state.betweenRounds,
                    isLast = index == order.lastIndex,
                    onMove = { delta -> order = order.moved(index, index + delta) },
                    onRemove = { player, name -> removing = player to name },
                    canRemove = editable && !team.out,
                )
            }
            if (order != original) {
                BigActionButton(
                    text = stringResource(R.string.players_order_save),
                    onClick = { confirmOrder = true },
                    kind = BigActionKind.Validate,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { order = original }) { Text(stringResource(R.string.log_cancel)) }
            }
            if (editable) {
                SectionLabel(R.string.players_add)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it.take(MAX_NAME) },
                        label = { Text(stringResource(R.string.players_add_name)) },
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions =
                            KeyboardActions(onDone = {
                                actions.onAddPlayer(newName)
                                newName = ""
                            }),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            actions.onAddPlayer(newName)
                            newName = ""
                        },
                        enabled = newName.isNotBlank(),
                        modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                    ) {
                        Text(stringResource(R.string.players_add_button))
                    }
                }
                Text(
                    stringResource(R.string.players_add_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (confirmOrder) {
        AlertDialog(
            onDismissRequest = { confirmOrder = false },
            title = { Text(stringResource(R.string.players_order_confirm_title)) },
            text = {
                val names = order.mapNotNull { id -> state.teams.firstOrNull { it.id == id }?.name }
                Text(stringResource(R.string.players_order_confirm_text, names.joinToString(", ")))
            },
            confirmButton = {
                TextButton(onClick = {
                    actions.onReorder(order)
                    confirmOrder = false
                }) { Text(stringResource(R.string.players_confirm)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmOrder = false },
                ) { Text(stringResource(R.string.log_cancel)) }
            },
        )
    }
    removing?.let { (player, name) ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.players_remove_title, name)) },
            text = { Text(stringResource(R.string.players_remove_text)) },
            confirmButton = {
                TextButton(onClick = {
                    actions.onRemovePlayer(player)
                    removing = null
                }) { Text(stringResource(R.string.players_remove_confirm), color = MolkkyBoardTheme.colors.coralText) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.log_cancel)) } },
        )
    }
}

@Composable
private fun OrderRow(
    index: Int,
    team: TeamUi,
    canMove: Boolean,
    isLast: Boolean,
    onMove: (Int) -> Unit,
    onRemove: (PlayerId, String) -> Unit,
    canRemove: Boolean,
) {
    val colors = MolkkyBoardTheme.colors.teams
    val color = colors[team.colorIndex % colors.size]
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).alpha(if (team.out) OUT_ALPHA else 1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
            ) {
                Text(
                    "${index + 1}.",
                    style = MolkkyBoardTheme.typography.scoreSmall,
                    modifier = Modifier.widthIn(min = 32.dp),
                )
                Box(Modifier.size(36.dp).background(color.fill, CircleShape), contentAlignment = Alignment.Center) {
                    Text(
                        team.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = color.onFill,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(team.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    if (team.out) {
                        Text(
                            stringResource(if (team.eliminated) R.string.game_eliminated_stamp else R.string.game_left),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MolkkyBoardTheme.colors.coralText,
                        )
                    }
                }
                MoveButton(
                    "↑",
                    stringResource(R.string.players_move_up, team.name),
                    enabled = canMove && index > 0,
                ) { onMove(-1) }
                MoveButton(
                    "↓",
                    stringResource(R.string.players_move_down, team.name),
                    enabled = canMove && !isLast,
                ) { onMove(1) }
            }
            if (canRemove) {
                team.memberIds.zip(team.members).forEach { (id, name) ->
                    TextButton(
                        onClick = { onRemove(id, name) },
                        modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                    ) {
                        Text(stringResource(R.string.players_remove, name), color = MolkkyBoardTheme.colors.coralText)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(MBDimens.MinTouch).semantics { contentDescription = description },
    ) {
        Text(symbol, style = MolkkyBoardTheme.typography.scoreSmall)
    }
}

@Composable
private fun SectionLabel(text: Int) {
    Text(
        stringResource(text).uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp).semantics { heading() },
    )
}

@Composable
internal fun MessageBanner(message: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MolkkyBoardTheme.colors.saffron.copy(alpha = BANNER_ALPHA),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp))
    }
}

/** Déplace l'élément [from] à la position [to]. */
internal fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

private const val MAX_NAME = 20
private const val OUT_ALPHA = 0.55f
private const val BANNER_ALPHA = 0.3f
