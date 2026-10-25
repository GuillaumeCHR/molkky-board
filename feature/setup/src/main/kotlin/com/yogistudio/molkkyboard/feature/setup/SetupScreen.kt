package com.yogistudio.molkkyboard.feature.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionButton
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionKind
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.component.PlayerChip
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/** Création de partie : joueurs, ordre de passage, préréglage de règles. */
@Composable
fun SetupScreen(
    onBack: () -> Unit,
    onGameCreated: (String) -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SetupContent(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onAdd = viewModel::addPlayer,
        onRemove = { viewModel.removePlayer(it.id) },
        onShuffle = viewModel::shuffle,
        onPreset = viewModel::setPreset,
        onStart = { viewModel.start(onGameCreated) },
    )
}

@Composable
internal fun SetupContent(
    state: SetupUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (SetupPlayer) -> Unit,
    onShuffle: () -> Unit,
    onPreset: (RulesPreset) -> Unit,
    onStart: () -> Unit,
) {
    val colors = MolkkyBoardTheme.colors.teams
    Column(Modifier.fillMaxSize().imePadding()) {
        MBTopBar(stringResource(R.string.setup_title), onBack)
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
        ) {
            Label(R.string.setup_players)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
            ) {
                OutlinedTextField(
                    value = state.draftName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.setup_player_name)) },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions = KeyboardActions(onDone = { onAdd() }),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onAdd,
                    enabled = state.canAdd,
                    modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                ) {
                    Text(stringResource(R.string.setup_add))
                }
            }
            state.players.forEachIndexed { index, player ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
                ) {
                    Text(
                        stringResource(R.string.setup_position, index + 1),
                        style = MolkkyBoardTheme.typography.scoreSmall,
                    )
                    PlayerChip(
                        name = player.name,
                        color = colors[player.colorIndex % colors.size],
                        active = index == 0,
                        onRemove = { onRemove(player) },
                    )
                }
            }
            if (state.players.size >= 2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.setup_order_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onShuffle, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
                        Text(stringResource(R.string.setup_shuffle))
                    }
                }
            }
            Label(R.string.setup_rules)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                RulesPreset.entries.forEachIndexed { index, preset ->
                    SegmentedButton(
                        selected = state.preset == preset,
                        onClick = { onPreset(preset) },
                        shape = SegmentedButtonDefaults.itemShape(index, RulesPreset.entries.size),
                        modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                        icon = {},
                    ) {
                        Text(
                            stringResource(
                                if (preset ==
                                    RulesPreset.Classic
                                ) {
                                    R.string.setup_rules_classic
                                } else {
                                    R.string.setup_rules_tournament
                                },
                            ),
                        )
                    }
                }
            }
            Text(
                stringResource(
                    if (state.preset ==
                        RulesPreset.Classic
                    ) {
                        R.string.setup_rules_classic_hint
                    } else {
                        R.string.setup_rules_tournament_hint
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Le bouton de lancement reste visible en bas et devient évident dès deux joueurs.
        Column(
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .padding(MBDimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!state.canStart) {
                Text(
                    stringResource(R.string.setup_need_two),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BigActionButton(
                text = stringResource(R.string.setup_start),
                onClick = onStart,
                kind = BigActionKind.Primary,
                enabled = state.canStart,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Label(text: Int) {
    Text(
        stringResource(text).uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}
