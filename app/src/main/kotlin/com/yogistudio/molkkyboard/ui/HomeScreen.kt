package com.yogistudio.molkkyboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionButton
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionKind
import com.yogistudio.molkkyboard.core.designsystem.component.MolkkyBoardLogotype
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens

/** Accueil : logotype, les deux actions principales, puis la partie en cours (aucune pour l'instant). */
@Composable
internal fun HomeScreen(
    onNewGame: () -> Unit,
    onJoin: () -> Unit,
    logotypeModifier: Modifier = Modifier,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(MBDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
    ) {
        MolkkyBoardLogotype(Modifier.padding(top = 8.dp, bottom = 24.dp).then(logotypeModifier))
        BigActionButton(
            text = stringResource(R.string.home_new_game),
            onClick = onNewGame,
            kind = BigActionKind.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
        BigActionButton(
            text = stringResource(R.string.home_join),
            onClick = onJoin,
            kind = BigActionKind.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.home_no_game), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.home_no_game_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
