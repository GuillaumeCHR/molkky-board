package com.yogistudio.molkkyboard.feature.history

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

@Composable
fun HistoryScreen(onOpenGame: (String) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryContent(state, onOpenGame, viewModel::delete)
}

/** Historique complet : chaque partie close, la plus récente d'abord ; un tap ouvre son déroulé. */
@Composable
fun HistoryContent(state: HistoryUiState, onOpenGame: (String) -> Unit, onDelete: (String) -> Unit) {
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.history_title))
        if (!state.loading && state.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(MBDimens.ScreenPadding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
        ) {
            items(state.items, key = { it.gameId }) { item ->
                HistoryCard(item, onOpen = { onOpenGame(item.gameId) }, onDelete = { confirm = item.gameId })
            }
        }
    }
    confirm?.let { id ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(id)
                    confirm = null
                }) { Text(stringResource(R.string.history_delete_confirm), color = MolkkyBoardTheme.colors.coralText) }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirm = null },
                ) { Text(stringResource(R.string.history_cancel)) }
            },
        )
    }
}

@Composable
private fun HistoryCard(item: HistoryItem, onOpen: () -> Unit, onDelete: () -> Unit) {
    val colors = MolkkyBoardTheme.colors.teams
    val context = LocalContext.current
    val openLabel = stringResource(R.string.history_open)
    val date =
        DateUtils.formatDateTime(
            context,
            item.playedAt,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
        )
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = openLabel, onClick = onOpen),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    date + if (item.sets > 1) " · " + stringResource(R.string.history_sets, item.sets) else "",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(MBDimens.MinTouch)) {
                    Icon(MBIcons.Delete, contentDescription = stringResource(R.string.history_delete))
                }
            }
            item.teams.forEach { team ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(14.dp).background(colors[team.colorIndex % colors.size].fill, CircleShape))
                    Text(
                        team.name + if (team.winner) " 🏆" else "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (team.winner) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    Text(team.score.toString(), style = MolkkyBoardTheme.typography.scoreSmall)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.abandoned) Badge(stringResource(R.string.history_abandoned))
                if (item.modifiedAfterFinish) Badge(stringResource(R.string.history_modified))
                if (item.corrected && !item.modifiedAfterFinish) Badge(stringResource(R.string.history_corrected))
            }
        }
    }
}

@Composable
private fun Badge(text: String) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
