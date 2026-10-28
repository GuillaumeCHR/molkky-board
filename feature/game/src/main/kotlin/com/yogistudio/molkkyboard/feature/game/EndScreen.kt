package com.yogistudio.molkkyboard.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionButton
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionKind
import com.yogistudio.molkkyboard.core.designsystem.component.PinGrid
import com.yogistudio.molkkyboard.core.designsystem.component.PinView
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.scoring.MOLKKOUT_PINS
import com.yogistudio.molkkyboard.core.scoring.NextStarter
import java.util.Locale

/**
 * Fin de manche : vainqueur et chiffre clé, classement, statistiques de la manche, match et Mölkkout, série des
 * mêmes équipes, puis « Revanche » (ou « Manche suivante » dans un match). Défile si la place manque.
 */
@Composable
internal fun EndPanel(
    state: GameUiState,
    actions: GameActions,
    onOpenLog: () -> Unit,
    onOpenPlacement: () -> Unit = {},
) {
    val end = state.end
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EndHeadline(state, end)
            end?.match?.let { MatchCard(it) }
            end?.match?.molkkout?.let { MolkkoutCard(it, actions) }
            RankingCard(state, end)
            end?.series?.let { SeriesCard(it) }
            if (!state.abandoned) ReplacePins(onOpenPlacement)
            EndActions(state, end, actions)
            Row(horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap)) {
                // Pendant un Mölkkout, seule son annulation est proposée (deux « Annuler » prêteraient à confusion).
                val molkkoutStarted = (end?.match?.molkkout?.throws ?: 0) > 0
                if (state.finished && !molkkoutStarted) {
                    TextButton(onClick = actions.onUndo) { Text(stringResource(R.string.game_undo)) }
                }
                TextButton(onClick = onOpenLog) { Text(stringResource(R.string.game_menu_log)) }
            }
        }
    }
}

@Composable
private fun EndHeadline(state: GameUiState, end: EndUi?) {
    val colors = MolkkyBoardTheme.colors.teams
    val winner = end?.winner
    val match = end?.match
    val title =
        when {
            state.abandoned -> stringResource(R.string.game_abandoned)
            match?.winner != null -> stringResource(R.string.end_match_winner, match.winner.name)
            match != null -> stringResource(R.string.end_set_winner, winner?.name.orEmpty())
            else -> stringResource(R.string.game_winner, state.winnerName.orEmpty())
        }
    Text(
        title,
        style = MaterialTheme.typography.headlineLarge,
        color = winner?.let { colors[it.colorIndex % colors.size].fill } ?: MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
    if (winner != null && end.winnerThrows > 0) {
        Text(
            stringResource(R.string.end_key_figure_target, state.target, end.winnerThrows, state.round),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    if (end?.perfect == true) Pill(stringResource(R.string.victory_perfect))
    if (end?.newRecord == true) Pill(stringResource(R.string.victory_new_record))
}

@Composable
private fun Pill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MolkkyBoardTheme.colors.saffron,
        contentColor = MolkkyBoardTheme.colors.onSaffron,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun EndCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

@Composable
private fun TeamDot(colorIndex: Int) {
    val colors = MolkkyBoardTheme.colors.teams
    Box(Modifier.size(14.dp).background(colors[colorIndex % colors.size].fill, CircleShape))
}

/** Classement et statistiques de chaque équipe dans la manche. */
@Composable
private fun RankingCard(state: GameUiState, end: EndUi?) {
    EndCard(stringResource(R.string.end_ranking)) {
        val stats = end?.teamStats.orEmpty().associateBy { it.team.id }
        val ranking = state.ranking.ifEmpty { state.scoreboard }
        ranking.forEachIndexed { index, team ->
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TeamDot(team.colorIndex)
                    Text(
                        stringResource(R.string.game_ranking_line, index + 1, team.name),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (team.out) {
                        Text(
                            stringResource(
                                if (team.eliminated) R.string.end_status_eliminated else R.string.end_status_left,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MolkkyBoardTheme.colors.coralText,
                        )
                    }
                    Text(team.score.toString(), style = MolkkyBoardTheme.typography.scoreSmall)
                }
                stats[team.id]?.let { line ->
                    Text(
                        stringResource(
                            R.string.end_stats_line,
                            pluralStringResource(R.plurals.end_throws, line.throws, line.throws),
                            pluralStringResource(R.plurals.end_misses, line.misses, line.misses),
                            line.average.oneDecimal(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 22.dp),
                    )
                }
            }
        }
    }
}

private fun Float.oneDecimal(): String = String.format(Locale.getDefault(), "%.1f", this)

@Composable
private fun MatchCard(match: MatchUi) {
    EndCard(stringResource(R.string.end_match_title, match.setsToWin)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            match.wins.forEach { (team, wins) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TeamDot(team.colorIndex)
                    Text(
                        stringResource(R.string.end_match_score, team.name, wins),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

/** Mölkkout : chaque équipe lance à tour de rôle sur les cinq quilles alignées. */
@Composable
private fun MolkkoutCard(molkkout: MolkkoutUi, actions: GameActions) {
    var selected by remember(molkkout.throws) { mutableStateOf(emptySet<Int>()) }
    EndCard(stringResource(R.string.molkkout_title)) {
        Text(stringResource(R.string.molkkout_explain), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            molkkout.totals.forEach { (team, total) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TeamDot(team.colorIndex)
                    Text(
                        stringResource(R.string.end_match_score, team.name, total),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        if (molkkout.winner != null) {
            Text(
                stringResource(R.string.molkkout_winner, molkkout.winner),
                style = MaterialTheme.typography.titleMedium,
            )
        } else {
            val player = molkkout.nextPlayer.orEmpty()
            val team = molkkout.nextTeam.orEmpty()
            Text(
                if (player == team) {
                    stringResource(R.string.molkkout_turn_solo, player)
                } else {
                    stringResource(R.string.molkkout_turn, player, team)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            // Alignement réglementaire, vu du lanceur : 6, 4, 12, 10, 8.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MOLKKOUT_ORDER.forEach { pin ->
                    PinView(
                        number = pin,
                        knocked = pin in selected,
                        onToggle = { knocked -> selected = if (knocked) selected + pin else selected - pin },
                        size = 52.dp,
                    )
                }
            }
            BigActionButton(
                stringResource(R.string.molkkout_validate),
                { actions.onMolkkout(selected) },
                BigActionKind.Validate,
                Modifier.fillMaxWidth(),
            )
        }
        if (molkkout.throws > 0) {
            TextButton(onClick = actions.onMolkkoutUndo) { Text(stringResource(R.string.molkkout_undo)) }
        }
    }
}

private val MOLKKOUT_ORDER = listOf(6, 4, 12, 10, 8).also { order -> check(order.toSet() == MOLKKOUT_PINS) }

@Composable
private fun SeriesCard(series: SeriesUi) {
    EndCard(stringResource(R.string.end_series_title, series.sets)) {
        series.rows.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TeamDot(row.colorIndex)
                Column(Modifier.weight(1f)) {
                    Text(row.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        row.scores.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.end_series_wins, row.wins),
                        style = MolkkyBoardTheme.typography.scoreSmall,
                    )
                    Text(
                        stringResource(R.string.end_series_total, row.total),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun EndActions(state: GameUiState, end: EndUi?, actions: GameActions) {
    val match = end?.match
    when {
        match?.nextSet != null -> {
            BigActionButton(
                stringResource(R.string.end_next_set, match.nextSet),
                actions.onNextSet,
                BigActionKind.Primary,
                Modifier.fillMaxWidth(),
            )
        }

        // Mölkkout à jouer : la manche suivante attend son résultat.
        match?.molkkout != null && match.winner == null -> {
            Unit
        }

        state.abandoned -> {
            BigActionButton(
                stringResource(R.string.game_new_game),
                actions.onNewGame,
                BigActionKind.Primary,
                Modifier.fillMaxWidth(),
            )
        }

        else -> {
            // Le partage est proposé, jamais imposé : il ne passe pas devant la revanche.
            BigActionButton(
                stringResource(R.string.end_share),
                actions.onShare,
                BigActionKind.Secondary,
                Modifier.fillMaxWidth(),
            )
            RematchButton(actions)
            BigActionButton(
                stringResource(R.string.game_new_game),
                actions.onNewGame,
                BigActionKind.Secondary,
                Modifier.fillMaxWidth(),
            )
        }
    }
    // Une partie finie se termine par un vrai bouton ; un match en cours reste ouvert et se reprend depuis l'accueil.
    if (state.finished && match?.nextSet == null && (match?.molkkout == null || match.winner != null)) {
        BigActionButton(
            stringResource(R.string.end_finish),
            actions.onHome,
            BigActionKind.Secondary,
            Modifier.fillMaxWidth(),
        )
    } else {
        TextButton(onClick = actions.onHome) { Text(stringResource(R.string.game_home)) }
    }
}

/** « Revanche » en un tap, avec l'ordre de passage choisi juste au-dessus (rotation par défaut). */
@Composable
private fun RematchButton(actions: GameActions) {
    var order by rememberSaveable { mutableStateOf(NextStarter.Rotate) }
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text(
                stringResource(R.string.end_rematch_order) + " : " + stringResource(order.label()),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            REMATCH_ORDERS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label())) },
                    onClick = {
                        order = option
                        open = false
                    },
                )
            }
        }
    }
    BigActionButton(stringResource(R.string.end_rematch), {
        actions.onRematch(order)
    }, BigActionKind.Primary, Modifier.fillMaxWidth())
}

private val REMATCH_ORDERS =
    listOf(NextStarter.Rotate, NextStarter.Same, NextStarter.LoserFirst, NextStarter.WinnerLast)

private fun NextStarter.label(): Int =
    when (this) {
        NextStarter.Same -> R.string.end_order_same
        NextStarter.Rotate, NextStarter.DecisiveByPoints -> R.string.end_order_rotate
        NextStarter.LoserFirst -> R.string.end_order_loser_first
        NextStarter.WinnerLast -> R.string.end_order_winner_last
    }

/** Entre deux manches : rappel de la formation de départ, avec un schéma miniature et l'accès au placement. */
@Composable
private fun ReplacePins(onOpenPlacement: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.end_replace_pins),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.fillMaxWidth().height(MINI_FORMATION).clearAndSetSemantics {}) {
            PinGrid(knocked = emptySet(), onToggle = { _, _ -> }, enabled = true, modifier = Modifier.fillMaxSize())
        }
        TextButton(onClick = onOpenPlacement) { Text(stringResource(R.string.placement_title)) }
    }
}

private val MINI_FORMATION = 170.dp
