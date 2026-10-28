package com.yogistudio.molkkyboard.feature.game

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

/** Règles du jeu (Réglages, accueil, raccourci) : écran plein, avec barre de titre. */
@Composable
fun RulesScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.rules_title), onBack)
        RulesContent(Modifier.navigationBarsPadding())
    }
}

/** Règles du jeu en pleine partie : une feuille qui ne fait pas quitter la partie. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RulesSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            stringResource(R.string.rules_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = MBDimens.ScreenPadding).semantics { heading() },
        )
        RulesContent(Modifier.navigationBarsPadding())
    }
}

/** Une règle : titre, texte, chiffre clé éventuel et illustration (schéma animé). */
private class RuleItem(
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    val key: String? = null,
    val illustration: (@Composable () -> Unit)? = null,
)

private class RulePart(
    @param:StringRes val title: Int,
    val rules: List<RuleItem>,
)

private val PARTS =
    listOf(
        RulePart(
            R.string.rules_part_prepare,
            listOf(
                RuleItem(R.string.rules_gear_title, R.string.rules_gear),
                // Formation officielle : 1-2 devant, 7-9-8 au fond.
                RuleItem(
                    R.string.rules_setup_title,
                    R.string.rules_setup,
                    key = "3,5 m",
                ) { PlacementGuide() },
            ),
        ),
        RulePart(
            R.string.rules_part_play,
            listOf(
                RuleItem(R.string.rules_throw_title, R.string.rules_throw),
                RuleItem(R.string.rules_points_title, R.string.rules_points, key = "1 = n°") { PointsDemo() },
                RuleItem(R.string.rules_raise_title, R.string.rules_raise),
            ),
        ),
        RulePart(
            R.string.rules_part_win,
            listOf(
                RuleItem(R.string.rules_win_title, R.string.rules_win, key = "50") { OvershootDemo() },
                RuleItem(R.string.rules_misses_title, R.string.rules_misses, key = "3 ✗") { MissesDemo() },
            ),
        ),
        RulePart(
            R.string.rules_part_together,
            listOf(
                RuleItem(R.string.rules_teams_title, R.string.rules_teams),
                RuleItem(
                    R.string.rules_molkkout_title,
                    R.string.rules_molkkout,
                    key = "6 4 12 10 8",
                ) { MolkkoutDemo() },
                RuleItem(R.string.rules_variants_title, R.string.rules_variants),
            ),
        ),
        RulePart(
            R.string.rules_faq_title,
            listOf(
                RuleItem(R.string.rules_faq_leaning_q, R.string.rules_faq_leaning_a),
                RuleItem(R.string.rules_faq_out_q, R.string.rules_faq_out_a),
                RuleItem(R.string.rules_faq_foot_q, R.string.rules_faq_foot_a),
                RuleItem(R.string.rules_faq_moved_q, R.string.rules_faq_moved_a),
            ),
        ),
    )

/** Minuscules sans accents : « reglés » trouve « Règles ». */
private fun String.searchable(): String =
    Normalizer.normalize(lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(DIACRITICS, "")

private val DIACRITICS = Regex("\\p{Mn}+")

/**
 * Les règles reformulées simplement (`docs/RULES.md`), en sections courtes et illustrées : de quoi lancer une partie
 * correcte avec l'app seule, même sans avoir jamais joué. Un sommaire mène à chaque partie ; la recherche filtre les
 * règles (titre et texte, sans tenir compte des accents).
 */
@Composable
fun RulesContent(modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val resources = LocalResources.current
    val needle = query.trim().searchable()
    val parts =
        remember(needle, resources) {
            if (needle.isEmpty()) {
                PARTS
            } else {
                PARTS.mapNotNull { part ->
                    val rules =
                        part.rules.filter { rule ->
                            (
                                resources.getString(
                                    rule.title,
                                ) + " " + resources.getString(rule.body)
                            ).searchable().contains(needle)
                        }
                    if (rules.isEmpty()) null else RulePart(part.title, rules)
                }
            }
        }
    val requesters = remember { PARTS.associate { it.title to BringIntoViewRequester() } }
    val scope = rememberCoroutineScope()
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MBDimens.ScreenPadding)
            .padding(bottom = MBDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(MAX_QUERY) },
            label = { Text(stringResource(R.string.rules_search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (needle.isEmpty()) {
            // Sommaire : un tap fait défiler jusqu'à la partie.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PARTS.forEach { part ->
                    AssistChip(
                        onClick = { scope.launch { requesters.getValue(part.title).bringIntoView() } },
                        label = { Text(stringResource(part.title)) },
                    )
                }
            }
        }
        if (parts.isEmpty()) {
            Text(stringResource(R.string.rules_search_empty), style = MaterialTheme.typography.bodyLarge)
        }
        parts.forEach { part ->
            Part(part.title, Modifier.bringIntoViewRequester(requesters.getValue(part.title)))
            part.rules.forEach { Rule(it) }
        }
    }
}

/** Intertitre d'une grande partie (« Préparer », « Jouer »…). */
@Composable
private fun Part(
    @StringRes title: Int,
    modifier: Modifier,
) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 20.dp).semantics { heading() },
    )
}

/** Une règle dans sa carte : titre, chiffre clé éventuel, texte et illustration. */
@Composable
private fun Rule(rule: RuleItem) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MBDimens.SmallGap),
            ) {
                Text(
                    stringResource(rule.title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (rule.key != null) {
                    Surface(shape = CircleShape, color = MolkkyBoardTheme.colors.saffron.copy(alpha = KEY_ALPHA)) {
                        Text(
                            rule.key,
                            style = MolkkyBoardTheme.typography.scoreSmall,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(stringResource(rule.body), style = MaterialTheme.typography.bodyLarge)
            rule.illustration?.invoke()
        }
    }
}

private const val KEY_ALPHA = 0.3f
private const val MAX_QUERY = 40
