package com.yogistudio.molkkyboard.feature.setup

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.EndCondition
import com.yogistudio.molkkyboard.core.model.GameConfig

/** Éditeur des exceptions du mode Ludique, partagé par les Réglages et l'écran de création de partie. */
@Composable
fun PlayfulRulesEditor(
    config: GameConfig,
    onChange: (GameConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val half = config.targetScore / 2
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MBDimens.Gap)) {
        RulesCard {
            CardTitle(stringResource(R.string.playful_misses_title, config.missLimit))
            MissChoice.entries.forEach { choice ->
                ChoiceRow(
                    label =
                        when (choice) {
                            MissChoice.ELIMINATE -> stringResource(R.string.playful_miss_eliminate)
                            MissChoice.NONE -> stringResource(R.string.playful_miss_none)
                            MissChoice.RESET_ZERO -> stringResource(R.string.playful_miss_reset_zero)
                            MissChoice.RESET_HALF -> stringResource(R.string.playful_miss_reset_half, half)
                        },
                    official = choice == MissChoice.ELIMINATE,
                    selected = config.missChoice == choice,
                    onSelect = { onChange(config.withMissChoice(choice)) },
                )
            }
            Stepper(
                label = R.string.playful_miss_limit,
                value = config.missLimit,
                canDecrease = config.missLimit > PlayfulRules.MIN_MISS_LIMIT,
                canIncrease = config.missLimit < PlayfulRules.MAX_MISS_LIMIT,
                onDecrease = { onChange(config.withMissLimit(config.missLimit - 1)) },
                onIncrease = { onChange(config.withMissLimit(config.missLimit + 1)) },
            )
        }
        RulesCard {
            CardTitle(stringResource(R.string.playful_overshoot_title, config.targetScore))
            OvershootChoice.entries.forEach { choice ->
                ChoiceRow(
                    label =
                        when (choice) {
                            OvershootChoice.RESET_HALF -> stringResource(R.string.playful_overshoot_half, half)
                            OvershootChoice.RESET_ZERO -> stringResource(R.string.playful_overshoot_zero)
                            OvershootChoice.STAY -> stringResource(R.string.playful_overshoot_stay)
                            OvershootChoice.BOUNCE -> stringResource(R.string.playful_overshoot_bounce)
                        },
                    official = choice == OvershootChoice.RESET_HALF && config.targetScore == GameConfig.STANDARD_TARGET,
                    selected = config.overshootChoice == choice,
                    onSelect = { onChange(config.withOvershootChoice(choice)) },
                )
            }
        }
        RulesCard {
            Stepper(
                label = R.string.playful_target,
                value = config.targetScore,
                canDecrease = config.targetScore > PlayfulRules.MIN_TARGET,
                canIncrease = config.targetScore < PlayfulRules.MAX_TARGET,
                onDecrease = { onChange(config.withTarget(config.targetScore - PlayfulRules.TARGET_STEP)) },
                onIncrease = { onChange(config.withTarget(config.targetScore + PlayfulRules.TARGET_STEP)) },
            )
        }
        SwitchCard(
            title = stringResource(R.string.playful_faults),
            hint = stringResource(R.string.playful_faults_hint),
            checked = config.faultsCountAsMiss,
            onChange = { onChange(config.copy(faultsCountAsMiss = it)) },
        )
        SwitchCard(
            title = stringResource(R.string.playful_places),
            hint = stringResource(R.string.playful_places_hint),
            checked = config.endCondition == EndCondition.PlayForPlaces,
            onChange = {
                onChange(config.copy(endCondition = if (it) EndCondition.PlayForPlaces else EndCondition.FirstToTarget))
            },
        )
        SwitchCard(
            title = stringResource(R.string.playful_catch_up),
            hint = stringResource(R.string.playful_catch_up_hint, half),
            checked = config.catchUpReset,
            onChange = { onChange(config.copy(catchUpReset = it)) },
        )
    }
}

/** Résumé des exceptions actives, ou rappel qu'aucune n'est active (la partie est alors Officielle). */
@Composable
fun playfulSummary(config: GameConfig): String {
    val exceptions = config.exceptions
    if (exceptions.isEmpty()) return stringResource(R.string.playful_summary_none)
    val labels = exceptions.map { exceptionLabel(it, config.targetScore / 2) }
    return stringResource(R.string.playful_summary, labels.joinToString(" · "))
}

@Composable
private fun exceptionLabel(
    exception: RuleException,
    half: Int,
): String =
    when (exception) {
        is RuleException.Miss -> {
            when (exception.choice) {
                MissChoice.NONE -> stringResource(R.string.playful_ex_no_elimination)
                MissChoice.RESET_ZERO -> stringResource(R.string.playful_ex_miss_zero)
                else -> stringResource(R.string.playful_ex_miss_half, half)
            }
        }

        is RuleException.MissLimit -> {
            stringResource(R.string.playful_ex_miss_limit, exception.limit)
        }

        is RuleException.Overshoot -> {
            when (exception.choice) {
                OvershootChoice.RESET_ZERO -> stringResource(R.string.playful_ex_overshoot_zero)
                OvershootChoice.STAY -> stringResource(R.string.playful_ex_overshoot_stay)
                else -> stringResource(R.string.playful_ex_overshoot_bounce)
            }
        }

        is RuleException.Target -> {
            stringResource(R.string.playful_ex_target, exception.target)
        }

        RuleException.FaultsIgnored -> {
            stringResource(R.string.playful_ex_faults)
        }

        RuleException.PlayForPlaces -> {
            stringResource(R.string.playful_ex_places)
        }

        RuleException.CatchUpReset -> {
            stringResource(R.string.playful_ex_catch_up)
        }
    }

@Composable
private fun RulesCard(content: @Composable () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(vertical = 8.dp)) { content() }
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun ChoiceRow(
    label: String,
    official: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .heightIn(min = MBDimens.MinTouch)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (official) {
                Text(
                    stringResource(R.string.playful_official),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun Stepper(
    @StringRes label: Int,
    value: Int,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val title = stringResource(label)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        val less = stringResource(R.string.playful_decrease, title)
        val more = stringResource(R.string.playful_increase, title)
        FilledTonalButton(
            onClick = onDecrease,
            enabled = canDecrease,
            modifier = Modifier.heightIn(min = MBDimens.MinTouch).semantics { contentDescription = less },
        ) { Text("−") }
        Text(
            value.toString(),
            style = MolkkyBoardTheme.typography.scoreSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 48.dp),
        )
        FilledTonalButton(
            onClick = onIncrease,
            enabled = canIncrease,
            modifier = Modifier.heightIn(min = MBDimens.MinTouch).semantics { contentDescription = more },
        ) { Text("+") }
    }
}

@Composable
internal fun SwitchCard(
    title: String,
    hint: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (hint != null) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}
