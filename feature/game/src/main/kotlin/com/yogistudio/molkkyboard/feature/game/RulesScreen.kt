package com.yogistudio.molkkyboard.feature.game

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.component.PinGrid
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

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
        Part(R.string.rules_part_prepare)
        Rule(R.string.rules_gear_title, R.string.rules_gear)
        Rule(R.string.rules_setup_title, R.string.rules_setup, key = "3,5 m") {
            // Formation officielle, vue depuis la ligne de lancer : 7-9-8 au fond, 1-2 devant.
            Box(Modifier.fillMaxWidth().height(FORMATION_HEIGHT)) {
                PinGrid(knocked = emptySet(), onToggle = { _, _ -> }, modifier = Modifier.fillMaxSize())
            }
            Text(
                stringResource(R.string.rules_setup_caption),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Part(R.string.rules_part_play)
        Rule(R.string.rules_throw_title, R.string.rules_throw)
        Rule(R.string.rules_points_title, R.string.rules_points, key = "1 = n°")
        Rule(R.string.rules_raise_title, R.string.rules_raise)
        Part(R.string.rules_part_win)
        Rule(R.string.rules_win_title, R.string.rules_win, key = "50")
        Rule(R.string.rules_misses_title, R.string.rules_misses, key = "3 ✗")
        Part(R.string.rules_part_together)
        Rule(R.string.rules_teams_title, R.string.rules_teams)
        Rule(R.string.rules_variants_title, R.string.rules_variants)
        Part(R.string.rules_faq_title)
        Rule(R.string.rules_faq_leaning_q, R.string.rules_faq_leaning_a)
        Rule(R.string.rules_faq_out_q, R.string.rules_faq_out_a)
        Rule(R.string.rules_faq_foot_q, R.string.rules_faq_foot_a)
        Rule(R.string.rules_faq_moved_q, R.string.rules_faq_moved_a)
    }
}

/** Intertitre d'une grande partie (« Préparer », « Jouer »…). */
@Composable
private fun Part(
    @StringRes title: Int,
) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp).semantics { heading() },
    )
}

/** Une règle dans sa carte : titre, chiffre clé éventuel, texte et illustration. */
@Composable
private fun Rule(
    @StringRes title: Int,
    @StringRes body: Int,
    key: String? = null,
    illustration: @Composable () -> Unit = {},
) {
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
                    stringResource(title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (key != null) {
                    Surface(shape = CircleShape, color = MolkkyBoardTheme.colors.saffron.copy(alpha = KEY_ALPHA)) {
                        Text(
                            key,
                            style = MolkkyBoardTheme.typography.scoreSmall,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
            illustration()
        }
    }
}

private val FORMATION_HEIGHT = 280.dp
private const val KEY_ALPHA = 0.3f
