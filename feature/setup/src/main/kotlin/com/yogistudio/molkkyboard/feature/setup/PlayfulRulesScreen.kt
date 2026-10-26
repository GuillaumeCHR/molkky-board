package com.yogistudio.molkkyboard.feature.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.model.GameConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayfulRulesViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) : ViewModel() {
        val rules: StateFlow<GameConfig> =
            settings.settings
                .map { it.playfulRules }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GameConfig.Playful)

        fun update(config: GameConfig) {
            viewModelScope.launch { settings.setPlayfulRules(config) }
        }

        fun reset() = update(GameConfig.Playful)

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

/** Réglages › Règles du mode Ludique : exceptions proposées par défaut aux nouvelles parties Ludique. */
@Composable
fun PlayfulRulesScreen(
    onBack: () -> Unit,
    viewModel: PlayfulRulesViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    PlayfulRulesContent(rules, onBack, viewModel::update, viewModel::reset)
}

@Composable
internal fun PlayfulRulesContent(
    rules: GameConfig,
    onBack: () -> Unit,
    onChange: (GameConfig) -> Unit,
    onReset: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.playful_title), onBack)
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = MBDimens.ScreenPadding)
                    .padding(bottom = MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
        ) {
            Text(
                stringResource(R.string.playful_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(playfulSummary(rules), style = MaterialTheme.typography.titleSmall)
            PlayfulRulesEditor(rules, onChange)
            TextButton(onClick = onReset, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
                Text(stringResource(R.string.playful_reset))
            }
        }
    }
}
