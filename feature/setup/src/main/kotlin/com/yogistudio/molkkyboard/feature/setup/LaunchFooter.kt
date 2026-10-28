package com.yogistudio.molkkyboard.feature.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionButton
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionKind
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/** Bouton de lancement, toujours visible en bas : évident dès deux joueurs, avec la raison s'il est désactivé. */
@Composable
internal fun LaunchFooter(state: SetupUiState, actions: SetupActions) {
    Column(
        modifier = Modifier.navigationBarsPadding().padding(MBDimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val duplicate = state.duplicateTeamName
        if (duplicate != null) {
            Text(
                stringResource(R.string.setup_duplicate_team, duplicate),
                style = MaterialTheme.typography.bodyMedium,
                color = MolkkyBoardTheme.colors.coralText,
            )
        } else if (!state.canStart) {
            Hint(if (state.playMode == PlayMode.SOLO) R.string.setup_need_two else R.string.setup_need_two_teams)
        }
        BigActionButton(
            text = stringResource(R.string.setup_start),
            onClick = actions.onStart,
            kind = BigActionKind.Primary,
            enabled = state.canStart,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
