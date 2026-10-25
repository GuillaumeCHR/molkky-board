package com.yogistudio.molkkyboard.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.designsystem.component.FitToScreen
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/**
 * Écran de jeu en plein écran (sans barre du bas). Le menu « ⋮ » regroupe le déroulé, la gestion des
 * joueurs, les règles et l'abandon ; ces entrées seront branchées au J4.
 */
@Composable
internal fun GameScreen(onBack: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.game_title), onBack) {
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(MBDimens.MinTouch)) {
                    Icon(MBIcons.More, contentDescription = stringResource(R.string.game_menu))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    listOf(
                        R.string.game_menu_log,
                        R.string.game_menu_players,
                        R.string.game_menu_rules,
                        R.string.game_menu_abandon,
                    ).forEach { label ->
                        DropdownMenuItem(
                            text = {
                                val color =
                                    if (label == R.string.game_menu_abandon) {
                                        MolkkyBoardTheme.colors.coralText
                                    } else {
                                        androidx.compose.ui.graphics.Color.Unspecified
                                    }
                                Text(stringResource(label), color = color)
                            },
                            onClick = { menu = false },
                        )
                    }
                }
            }
        }
        // L'écran de jeu tient toujours sur un seul écran : réduction homothétique si la place manque.
        FitToScreen(
            Modifier
                .weight(1f)
                .navigationBarsPadding(),
        ) {
            GamePreviewContent()
        }
    }
}
