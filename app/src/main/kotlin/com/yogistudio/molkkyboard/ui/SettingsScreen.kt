package com.yogistudio.molkkyboard.ui

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.model.InputMode

/** Langues proposées : celle du téléphone, ou une langue imposée à l'application. */
private enum class AppLanguage(
    val tag: String,
    @param:StringRes val label: Int,
) {
    System("", R.string.settings_language_system),
    French("fr", R.string.settings_language_fr),
    English("en", R.string.settings_language_en),
}

@Composable
internal fun SettingsScreen(
    onAbout: () -> Unit,
    onPlayfulRules: () -> Unit,
    onRules: () -> Unit,
    onShowcase: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.nav_settings))
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MBDimens.ScreenPadding)
                    .padding(bottom = MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
        ) {
            SectionTitle(R.string.settings_display)
            SettingsSwitch(
                R.string.settings_sunlight,
                R.string.settings_sunlight_hint,
                settings.sunlight,
                onChange = viewModel::setSunlight,
            )
            LanguageCard()

            SectionTitle(R.string.settings_game)
            SettingsCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(MBDimens.SmallGap)) {
                    Text(stringResource(R.string.settings_input_mode), style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        InputMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = settings.inputMode == mode,
                                onClick = { viewModel.setInputMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, InputMode.entries.size),
                                modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                                icon = {},
                            ) {
                                Text(
                                    stringResource(
                                        if (mode ==
                                            InputMode.PINS
                                        ) {
                                            R.string.settings_input_pins
                                        } else {
                                            R.string.settings_input_quick
                                        },
                                    ),
                                )
                            }
                        }
                    }
                    Hint(R.string.settings_input_hint)
                }
            }
            SettingsLink(R.string.settings_rules, R.string.settings_rules_hint, onRules)
            SettingsLink(R.string.settings_playful_rules, R.string.settings_playful_rules_hint, onPlayfulRules)
            SettingsSwitch(
                R.string.settings_coach,
                R.string.settings_coach_hint,
                settings.coach,
                onChange = viewModel::setCoach,
            )
            SettingsLink(
                R.string.settings_guide,
                if (settings.onboardingDone) R.string.settings_guide_hint else R.string.settings_guide_pending,
                viewModel::replayGuide,
            )
            SettingsSwitch(
                R.string.settings_voice,
                R.string.settings_voice_hint,
                settings.voiceAnnouncements,
                onChange = viewModel::setVoiceAnnouncements,
            )
            if (settings.voiceAnnouncements) {
                SettingsSlider(R.string.settings_voice_rate, settings.voiceRate, VOICE_RATES, viewModel::setVoiceRate)
                SettingsSlider(
                    R.string.settings_voice_volume,
                    settings.voiceVolume,
                    VOICE_VOLUMES,
                    viewModel::setVoiceVolume,
                )
            }
            SettingsSwitch(
                R.string.settings_notification,
                R.string.settings_notification_hint,
                settings.ongoingNotification,
                onChange = viewModel::setOngoingNotification,
            )
            SettingsSwitch(
                R.string.settings_sounds,
                R.string.settings_coming_hint,
                settings.sounds,
                onChange = viewModel::setSounds,
            )

            SectionTitle(R.string.settings_startup)
            SettingsSwitch(
                title = R.string.settings_short_intro,
                hint =
                    if (settings.canChooseShortIntro) {
                        R.string.settings_short_intro_hint
                    } else {
                        R.string.settings_short_intro_locked
                    },
                checked = settings.shortIntro,
                enabled = settings.canChooseShortIntro,
                onChange = viewModel::setShortIntro,
            )

            if (SHOWCASE_AVAILABLE) {
                SettingsLink(R.string.settings_showcase, R.string.settings_showcase_hint, onShowcase)
            }
            SettingsLink(R.string.settings_about, null, onAbout)
        }
    }
}

/** Langue de l'application (Android 13+ : la même que dans les réglages système de l'app). */
@Composable
private fun LanguageCard() {
    var current by remember { mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags()) }
    SettingsCard {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            AppLanguage.entries.forEach { language ->
                val selected = current.substringBefore('-') == language.tag
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected, role = Role.RadioButton) {
                                current = language.tag
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
                            }.heightIn(min = MBDimens.MinTouch)
                            .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Text(
                        stringResource(language.label),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    @StringRes title: Int,
) {
    Text(
        stringResource(title).uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, content = content)
}

@Composable
private fun SettingsSwitch(
    @StringRes title: Int,
    @StringRes hint: Int?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    SettingsCard {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLabel(title, hint, Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}

/** Curseur réglé au relâchement (pas d'écriture DataStore à chaque pixel). */
@Composable
private fun SettingsSlider(
    @StringRes title: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    var current by remember(value) { mutableStateOf(value) }
    SettingsCard {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Slider(
                value = current,
                onValueChange = { current = it },
                onValueChangeFinished = { onChange(current) },
                valueRange = range,
            )
        }
    }
}

private val VOICE_RATES = 0.5f..2f
private val VOICE_VOLUMES = 0.1f..1f

@Composable
private fun SettingsLink(
    @StringRes title: Int,
    @StringRes hint: Int?,
    onClick: () -> Unit,
) {
    SettingsCard {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLabel(title, hint, Modifier.weight(1f))
            Icon(MBIcons.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun SettingsLabel(
    @StringRes title: Int,
    @StringRes hint: Int?,
    modifier: Modifier,
) {
    Column(modifier.padding(vertical = 12.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        if (hint != null) Hint(hint)
    }
}

@Composable
private fun Hint(
    @StringRes hint: Int,
) {
    Text(
        stringResource(hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
