package com.yogistudio.molkkyboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionButton
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionKind
import com.yogistudio.molkkyboard.core.designsystem.component.MolkkyBoardLogotype
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/** Accueil : logotype, les deux actions principales, puis la partie en cours (aucune pour l'instant). */
@Composable
internal fun HomeScreen(
    onNewGame: () -> Unit,
    onJoin: () -> Unit,
    onResume: (String) -> Unit,
    logotypeModifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ongoing by viewModel.ongoing.collectAsStateWithLifecycle()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(MBDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
    ) {
        // Logotype compact, à la hauteur d'un titre de barre d'application : il ne domine pas l'écran.
        MolkkyBoardLogotype(
            Modifier.padding(top = 4.dp, bottom = 12.dp).then(logotypeModifier),
            fontSize = HOME_LOGOTYPE_SIZE,
        )
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
        val game = ongoing
        if (game != null) OngoingCard(game, onResume) else NoGameCard()
    }
}

/** S'il existe une partie en cours, l'accueil propose de la reprendre en premier. */
@Composable
private fun OngoingCard(game: OngoingGame, onResume: (String) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.home_ongoing), style = MaterialTheme.typography.titleLarge)
            game.scores.forEach { (name, score) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(score.toString(), style = MolkkyBoardTheme.typography.scoreSmall)
                }
            }
            game.nextPlayer?.let {
                Text(
                    stringResource(R.string.home_ongoing_turn, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BigActionButton(
                text = stringResource(R.string.home_resume),
                onClick = { onResume(game.id) },
                kind = BigActionKind.Validate,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NoGameCard() {
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

private val HOME_LOGOTYPE_SIZE = 22.sp
