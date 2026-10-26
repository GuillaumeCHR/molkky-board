package com.yogistudio.molkkyboard.feature.game

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.component.PinGrid
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens

/** Règles du jeu (Réglages) : écran plein, avec barre de titre. */
@Composable
fun RulesScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.rules_title), onBack)
        RulesContent(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding())
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
        RulesContent(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding())
    }
}

/**
 * Les règles reformulées simplement (`docs/RULES.md`), en sections courtes : de quoi lancer une partie correcte
 * avec l'app seule, même sans avoir jamais joué. La formation de départ est dessinée avec la grille de quilles.
 */
@Composable
fun RulesContent(modifier: Modifier = Modifier) {
    Column(
        modifier.padding(horizontal = MBDimens.ScreenPadding).padding(bottom = MBDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
    ) {
        Section(R.string.rules_gear_title, R.string.rules_gear)
        Section(R.string.rules_setup_title, R.string.rules_setup)
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(12.dp)) {
                // Formation officielle, vue depuis la ligne de lancer : 7-9-8 au fond, 1-2 devant.
                Box(Modifier.fillMaxWidth().height(FORMATION_HEIGHT)) {
                    PinGrid(knocked = emptySet(), onToggle = {
                        _,
                        _,
                        ->
                    }, enabled = false, modifier = Modifier.fillMaxSize())
                }
                Text(
                    stringResource(R.string.rules_setup_caption),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Section(R.string.rules_throw_title, R.string.rules_throw)
        Section(R.string.rules_points_title, R.string.rules_points)
        Section(R.string.rules_raise_title, R.string.rules_raise)
        Section(R.string.rules_win_title, R.string.rules_win)
        Section(R.string.rules_misses_title, R.string.rules_misses)
        Section(R.string.rules_teams_title, R.string.rules_teams)
        Section(R.string.rules_variants_title, R.string.rules_variants)
        Text(
            stringResource(R.string.rules_faq_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
        )
        Section(R.string.rules_faq_leaning_q, R.string.rules_faq_leaning_a)
        Section(R.string.rules_faq_out_q, R.string.rules_faq_out_a)
        Section(R.string.rules_faq_foot_q, R.string.rules_faq_foot_a)
        Section(R.string.rules_faq_moved_q, R.string.rules_faq_moved_a)
    }
}

@Composable
private fun Section(
    @StringRes title: Int,
    @StringRes body: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
    }
}

private val FORMATION_HEIGHT = 300.dp
