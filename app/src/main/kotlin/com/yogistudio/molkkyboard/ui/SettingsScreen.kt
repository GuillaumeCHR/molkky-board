package com.yogistudio.molkkyboard.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.data.RestoreError
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.model.InputMode
import java.time.LocalDate
import java.util.Locale

/**
 * Langues de l'application. Sans choix explicite, l'app suit le téléphone s'il est en anglais, et reste en
 * français sinon (les textes par défaut sont en français) : la case cochée montre toujours la langue affichée.
 */
private enum class AppLanguage(
    val tag: String,
    val flag: String,
    @param:StringRes val label: Int,
) {
    French("fr", "🇫🇷", R.string.settings_language_fr),
    English("en", "🇬🇧", R.string.settings_language_en),
    ;

    companion object {
        fun current(): AppLanguage {
            val chosen = AppCompatDelegate.getApplicationLocales()
            val tag = if (chosen.isEmpty) Locale.getDefault().language else chosen[0]?.language
            return entries.firstOrNull { it.tag == tag } ?: French
        }
    }
}

@Composable
internal fun SettingsScreen(
    onAbout: () -> Unit,
    onPlayfulRules: () -> Unit,
    onRules: () -> Unit,
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
            SettingsSwitch(
                R.string.settings_ready_card,
                R.string.settings_ready_card_hint,
                settings.showReadyCard,
                onChange = viewModel::setReadyCard,
            )
            SettingsSwitch(
                R.string.settings_beginner,
                R.string.settings_beginner_hint,
                settings.beginnerMode,
                onChange = viewModel::setBeginnerMode,
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

            SectionTitle(R.string.settings_data)
            DataSection(viewModel)

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

            SettingsLink(R.string.settings_about, null, onAbout)
        }
    }
}

/**
 * Sauvegarde et restauration par fichier (sélecteur de documents Android : aucune permission de stockage), et
 * suppression de toutes les données, avec confirmation.
 */
@Composable
private fun DataSection(viewModel: SettingsViewModel) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val fileName = stringResource(R.string.settings_backup_file, LocalDate.now().toString())
    val create =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
            uri?.let(viewModel::backupTo)
        }
    val open =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::restoreFrom)
        }
    SettingsLink(
        R.string.settings_backup,
        R.string.settings_backup_hint,
        { create.launch(fileName) },
        enabled = !data.busy,
    )
    SettingsLink(
        R.string.settings_restore,
        R.string.settings_restore_hint,
        { open.launch(arrayOf(BACKUP_MIME, "text/plain", "application/octet-stream")) },
        enabled = !data.busy,
    )
    SettingsLink(
        R.string.settings_delete_all,
        R.string.settings_delete_all_hint,
        { confirmDelete = true },
        enabled = !data.busy,
        danger = true,
    )
    if (data.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    data.message?.let { message ->
        Text(
            dataMessage(message),
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (message is DataMessage.BackupFailed || message is DataMessage.RestoreFailed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.settings_delete_all_title)) },
            text = { Text(stringResource(R.string.settings_delete_all_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteAll()
                }) {
                    Text(
                        stringResource(R.string.settings_delete_all_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.settings_cancel)) }
            },
        )
    }
}

@Composable
private fun dataMessage(message: DataMessage): String =
    when (message) {
        DataMessage.BackupDone -> {
            stringResource(R.string.settings_backup_done)
        }

        DataMessage.BackupFailed -> {
            stringResource(R.string.settings_backup_failed)
        }

        DataMessage.Deleted -> {
            stringResource(R.string.settings_delete_all_done)
        }

        is DataMessage.RestoreFailed -> {
            stringResource(
                when (message.error) {
                    RestoreError.NOT_A_BACKUP -> R.string.settings_restore_not_backup
                    RestoreError.NEWER_VERSION -> R.string.settings_restore_newer
                    RestoreError.TOO_LARGE -> R.string.settings_restore_too_large
                    RestoreError.UNREADABLE -> R.string.settings_restore_unreadable
                },
            )
        }

        is DataMessage.Restored -> {
            val report = message.report
            buildList {
                add(pluralStringResource(R.plurals.settings_restore_added, report.gamesAdded, report.gamesAdded))
                if (report.gamesAlreadyThere > 0) {
                    add(
                        pluralStringResource(
                            R.plurals.settings_restore_present,
                            report.gamesAlreadyThere,
                            report.gamesAlreadyThere,
                        ),
                    )
                }
                if (report.gamesInvalid > 0) {
                    add(
                        pluralStringResource(
                            R.plurals.settings_restore_invalid,
                            report.gamesInvalid,
                            report.gamesInvalid,
                        ),
                    )
                }
            }.joinToString(" · ")
        }
    }

private const val BACKUP_MIME = "application/json"

/** Langue de l'application (Android 13+ : la même que dans les réglages système de l'app). */
@Composable
private fun LanguageCard() {
    var current by remember { mutableStateOf(AppLanguage.current()) }
    SettingsCard {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            AppLanguage.entries.forEach { language ->
                val selected = current == language
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected, role = Role.RadioButton) {
                                current = language
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
                            }.heightIn(min = MBDimens.MinTouch)
                            .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Text(
                        "${language.flag}  ${stringResource(language.label)}",
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
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    SettingsCard {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled, onClick = onClick)
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLabel(
                title,
                hint,
                Modifier.weight(1f),
                if (danger) MaterialTheme.colorScheme.error else Color.Unspecified,
            )
            Icon(MBIcons.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun SettingsLabel(
    @StringRes title: Int,
    @StringRes hint: Int?,
    modifier: Modifier,
    color: Color = Color.Unspecified,
) {
    Column(modifier.padding(vertical = 12.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = color)
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
