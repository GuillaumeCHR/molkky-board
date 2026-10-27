package com.yogistudio.molkkyboard.feature.stats

import android.text.format.DateUtils
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.yogistudio.molkkyboard.core.data.PlayerRepository
import com.yogistudio.molkkyboard.core.data.StatsRepository
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.stats.Badge
import com.yogistudio.molkkyboard.core.stats.EarnedBadge
import com.yogistudio.molkkyboard.core.stats.PlayerStats
import com.yogistudio.molkkyboard.core.stats.badges
import com.yogistudio.molkkyboard.core.stats.playerStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

data class StatsPlayer(
    val id: PlayerId,
    val name: String,
    val avatar: String?,
)

data class StatsUiState(
    val loading: Boolean = true,
    val players: List<StatsPlayer> = emptyList(),
    val selected: PlayerId? = null,
    val stats: PlayerStats? = null,
    val badges: List<EarnedBadge> = emptyList(),
)

/** Statistiques simples des joueurs habituels (profils locaux), calculées depuis les journaux. */
@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        stats: StatsRepository,
        profiles: PlayerRepository,
    ) : ViewModel() {
        private val selected = MutableStateFlow<PlayerId?>(null)

        val state: StateFlow<StatsUiState> =
            combine(stats.summaries, profiles.profiles, selected) { summaries, people, chosen ->
                val played = summaries.flatMap { s -> s.teams.flatMap { it.players } }.toSet()
                val players =
                    people
                        .filter { PlayerId(it.id) in played }
                        .map { StatsPlayer(PlayerId(it.id), it.name, it.avatar) }
                val current = chosen?.takeIf { id -> players.any { it.id == id } } ?: players.firstOrNull()?.id
                StatsUiState(
                    loading = false,
                    players = players,
                    selected = current,
                    stats = current?.let { playerStats(summaries, it) },
                    badges = current?.let { badges(summaries, it) }.orEmpty(),
                )
            }.flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatsUiState())

        fun select(player: PlayerId) {
            selected.value = player
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatsContent(state, onBack, viewModel::select)
}

@Composable
fun StatsContent(state: StatsUiState, onBack: () -> Unit = {}, onSelect: (PlayerId) -> Unit = {}) {
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.stats_title), onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
        ) {
            if (!state.loading && state.players.isEmpty()) {
                Text(
                    stringResource(R.string.stats_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.players.forEach { player ->
                    FilterChip(
                        selected = player.id == state.selected,
                        onClick = { onSelect(player.id) },
                        label = { Text(listOfNotNull(player.avatar, player.name).joinToString(" ")) },
                    )
                }
            }
            state.stats?.let { PlayerStatsCards(it) }
            if (state.stats != null) BadgesSection(state.badges)
        }
    }
}

@Composable
private fun PlayerStatsCards(stats: PlayerStats) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
        verticalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
    ) {
        StatTile(stringResource(R.string.stats_average), stats.average.oneDecimal())
        StatTile(stringResource(R.string.stats_miss_rate), "${(stats.missRate * PERCENT).roundToInt()} %")
        StatTile(stringResource(R.string.stats_wins), stringResource(R.string.stats_wins_value, stats.wins, stats.sets))
        StatTile(stringResource(R.string.stats_throws), stats.throws.toString())
    }
    if (stats.averages.size >= 2) AverageChart(stats)
    Text(
        stringResource(
            R.string.stats_records,
        ),
        style = MaterialTheme.typography.titleLarge,
        modifier =
            Modifier.semantics {
                heading()
            },
    )
    Text(stringResource(R.string.stats_best_streak, stats.bestStreak), style = MaterialTheme.typography.bodyLarge)
    Text(stringResource(R.string.stats_twelves, stats.twelves), style = MaterialTheme.typography.bodyLarge)
    Text(stringResource(R.string.stats_resets, stats.resets), style = MaterialTheme.typography.bodyLarge)
    Text(
        stats.fastestWin?.let { stringResource(R.string.stats_fastest, it) }
            ?: stringResource(R.string.stats_fastest_none),
        style = MaterialTheme.typography.bodyLarge,
    )
}

/** Badges obtenus (avec la date de la première fois), puis ceux qui restent à décrocher, grisés. */
@Composable
private fun BadgesSection(earned: List<EarnedBadge>) {
    Text(
        stringResource(R.string.stats_badges),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
    val context = LocalContext.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
        verticalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
    ) {
        Badge.entries.forEach { badge ->
            val got = earned.firstOrNull { it.badge == badge }
            val (emoji, name, hint) = badge.texts()
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (got != null) MolkkyBoardTheme.colors.saffron else MaterialTheme.colorScheme.surface,
                contentColor =
                    if (got !=
                        null
                    ) {
                        MolkkyBoardTheme.colors.onSaffron
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.alpha(if (got != null) 1f else LOCKED_ALPHA),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text("$emoji ${stringResource(name)}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        got?.let {
                            DateUtils.formatDateTime(
                                context,
                                it.playedAt,
                                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH,
                            )
                        } ?: stringResource(hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun Badge.texts(): Triple<String, Int, Int> =
    when (this) {
        Badge.FIRST_WIN -> Triple("🥇", R.string.badge_first_win, R.string.badge_first_win_hint)
        Badge.PERFECT_GAME -> Triple("✨", R.string.badge_perfect, R.string.badge_perfect_hint)
        Badge.PHOENIX -> Triple("🐦‍🔥", R.string.badge_phoenix, R.string.badge_phoenix_hint)
        Badge.SNIPER -> Triple("🎯", R.string.badge_sniper, R.string.badge_sniper_hint)
    }

private const val LOCKED_ALPHA = 0.6f

@Composable
private fun StatTile(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.widthIn(min = 150.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MolkkyBoardTheme.typography.scoreLarge)
        }
    }
}

/** Moyenne par lancer des dernières manches (Vico), décrite en une phrase pour TalkBack. */
@Composable
private fun AverageChart(stats: PlayerStats) {
    val recent = stats.averages.takeLast(CHART_SETS)
    val color = MaterialTheme.colorScheme.primary
    val column = remember(color) { LineComponent(Fill(SolidColor(color)), COLUMN_WIDTH.dp, RoundedCornerShape(50)) }
    val model =
        remember(recent) {
            CartesianChartModel(ColumnCartesianLayerModel.build { series(recent.map { it.average }) })
        }
    val context = LocalContext.current
    val description =
        stringResource(
            R.string.stats_chart_description,
            recent.size,
            DateUtils.formatDateTime(context, recent.first().playedAt, DateUtils.FORMAT_SHOW_DATE),
            DateUtils.formatDateTime(context, recent.last().playedAt, DateUtils.FORMAT_SHOW_DATE),
        )
    Text(
        stringResource(
            R.string.stats_chart,
        ),
        style = MaterialTheme.typography.titleLarge,
        modifier =
            Modifier.semantics {
                heading()
            },
    )
    CartesianChartHost(
        chart =
            rememberCartesianChart(
                rememberColumnCartesianLayer(ColumnCartesianLayer.ColumnProvider.series(column)),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(),
            ),
        model = model,
        modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT.dp).semantics { contentDescription = description },
    )
}

private fun Float.oneDecimal(): String = String.format(Locale.getDefault(), "%.1f", this)

private const val PERCENT = 100
private const val CHART_SETS = 20
private const val CHART_HEIGHT = 200
private const val COLUMN_WIDTH = 12
