package com.yogistudio.molkkyboard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.designsystem.catalog.DesignSystemCatalog
import com.yogistudio.molkkyboard.core.designsystem.catalog.GameScreenMockup
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar

// Build debug : la maquette de jeu et la vitrine du design system (source set debug du design system).

internal const val SHOWCASE_AVAILABLE = true

@Composable
internal fun ShowcaseScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.settings_showcase), onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            DesignSystemCatalog()
            GameScreenMockup(quickMode = true)
        }
    }
}
