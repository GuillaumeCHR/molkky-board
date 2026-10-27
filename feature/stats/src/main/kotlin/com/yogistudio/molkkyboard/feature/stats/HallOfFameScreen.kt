package com.yogistudio.molkkyboard.feature.stats

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.stats.HallTab
import kotlin.math.roundToInt

@Composable
fun HallOfFameScreen(
    onOpenGame: (String) -> Unit,
    onOpenStats: () -> Unit,
    viewModel: HallOfFameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HallOfFameContent(
        state = state,
        onTab = viewModel::selectTab,
        onPeriod = viewModel::selectPeriod,
        onTeamSize = viewModel::selectTeamSize,
        onOpenGame = onOpenGame,
        onOpenStats = onOpenStats,
    )
}

/** Onglets Joueurs / Équipes, filtres de période et de taille, puis les deux classements. */
@Composable
fun HallOfFameContent(
    state: HallUiState,
    onTab: (HallTab) -> Unit = {},
    onPeriod: (Period) -> Unit = {},
    onTeamSize: (Int?) -> Unit = {},
    onOpenGame: (String) -> Unit = {},
    onOpenStats: () -> Unit = {},
) {
    val filters = state.filters
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.hall_title)) {
            IconButton(onClick = onOpenStats, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
                Icon(MBIcons.Chart, contentDescription = stringResource(R.string.hall_stats))
            }
        }
        PrimaryTabRow(selectedTabIndex = filters.tab.ordinal, containerColor = MaterialTheme.colorScheme.background) {
            Tab(filters.tab == HallTab.PLAYERS, {
                onTab(HallTab.PLAYERS)
            }, text = { Text(stringResource(R.string.hall_tab_players)) })
            Tab(
                filters.tab == HallTab.TEAMS,
                { onTab(HallTab.TEAMS) },
                text = { Text(stringResource(R.string.hall_tab_teams)) },
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
        ) {
            item {
                ChipRow {
                    Period.entries.forEach { period ->
                        FilterChip(
                            filters.period == period,
                            { onPeriod(period) },
                            label = { Text(stringResource(period.label())) },
                        )
                    }
                }
            }
            if (filters.tab == HallTab.TEAMS && state.teamSizes.size > 1) {
                item {
                    ChipRow {
                        FilterChip(
                            filters.teamSize == null,
                            { onTeamSize(null) },
                            label = { Text(stringResource(R.string.hall_size_all)) },
                        )
                        state.teamSizes.forEach { size ->
                            FilterChip(
                                filters.teamSize == size,
                                { onTeamSize(size) },
                                label = { Text(stringResource(R.string.hall_size, size)) },
                            )
                        }
                    }
                }
            }
            if (!state.loading && state.wins.isEmpty()) {
                item {
                    Text(
                        stringResource(
                            if (filters.tab ==
                                HallTab.PLAYERS
                            ) {
                                R.string.hall_empty_players
                            } else {
                                R.string.hall_empty_teams
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            if (state.wins.isNotEmpty()) item { SectionTitle(stringResource(R.string.hall_wins)) }
            itemsIndexed(state.wins, key = { _, entry -> "w" + entry.memberKey }) { index, entry ->
                val teamSize = entry.size.takeIf { filters.tab == HallTab.TEAMS }
                RecordRow(
                    rank = index + 1,
                    name = entry.name,
                    teamSize = teamSize,
                    detail =
                        stringResource(
                            R.string.hall_wins_line,
                            pluralStringResource(R.plurals.hall_wins_count, entry.wins, entry.wins),
                            pluralStringResource(R.plurals.hall_played_count, entry.played, entry.played),
                            (entry.rate * PERCENT).roundToInt(),
                        ),
                    onOpen = entry.lastWinGameId?.let { id -> { onOpenGame(id) } },
                )
            }
            if (state.fastest.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.hall_fastest)) }
                item {
                    Text(
                        stringResource(R.string.hall_fastest_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(state.fastest, key = { _, entry -> "f" + entry.memberKey }) { index, entry ->
                RecordRow(
                    rank = index + 1,
                    name = entry.name,
                    teamSize = entry.size.takeIf { filters.tab == HallTab.TEAMS },
                    detail = stringResource(R.string.hall_fastest_line, entry.throws, formatDate(entry.playedAt)),
                    badge = if (entry.perfect) stringResource(R.string.hall_perfect) else null,
                    onOpen = { onOpenGame(entry.gameId) },
                )
            }
        }
    }
}

private const val PERCENT = 100

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 12.dp).semantics { heading() },
    )
}

@Composable
private fun formatDate(at: Long): String =
    DateUtils.formatDateTime(LocalContext.current, at, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)

/** Une ligne de classement : rang, nom, détail ; un tap ouvre le déroulé de la partie du record. */
@Composable
private fun RecordRow(
    rank: Int,
    name: String,
    teamSize: Int?,
    detail: String,
    badge: String? = null,
    onOpen: (() -> Unit)?,
) {
    val first = rank == 1
    val openLabel = stringResource(R.string.hall_open)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (first) MolkkyBoardTheme.colors.saffron else MaterialTheme.colorScheme.surface,
        contentColor = if (first) MolkkyBoardTheme.colors.onSaffron else MaterialTheme.colorScheme.onSurface,
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (onOpen != null) Modifier.clickable(onClickLabel = openLabel, onClick = onOpen) else Modifier),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                rank.toString(),
                style = MolkkyBoardTheme.typography.scoreSmall,
                modifier = Modifier.widthIn(min = 28.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    name + (teamSize?.let { " · " + stringResource(R.string.hall_team_size, it) } ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(detail, style = MaterialTheme.typography.bodyMedium)
                if (badge != null) Text("✨ $badge", style = MaterialTheme.typography.labelLarge)
            }
            if (onOpen != null) Icon(MBIcons.ChevronRight, contentDescription = null)
        }
    }
}

private fun Period.label(): Int =
    when (this) {
        Period.ALL -> R.string.hall_period_all
        Period.YEAR -> R.string.hall_period_year
        Period.MONTH -> R.string.hall_period_month
    }
