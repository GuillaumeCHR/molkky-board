package com.yogistudio.molkkyboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens

// Build release : pas de vitrine ; l'écran de jeu provisoire l'annonce simplement.

internal const val SHOWCASE_AVAILABLE = false

@Composable
internal fun GamePreviewContent() {
    Text(stringResource(R.string.game_preview_release), modifier = Modifier.padding(MBDimens.ScreenPadding))
}

@Composable
internal fun ShowcaseScreen(onBack: () -> Unit) = Unit
