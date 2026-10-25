package com.yogistudio.molkkyboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens

@Composable
internal fun SettingsScreen(
    sunlight: Boolean,
    onSunlightChange: (Boolean) -> Unit,
    onAbout: () -> Unit,
    onShowcase: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.nav_settings))
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
        ) {
            Text(
                stringResource(R.string.settings_display).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsCard {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = sunlight, role = Role.Switch, onValueChange = onSunlightChange)
                            .heightIn(min = 72.dp)
                            .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsLabel(R.string.settings_sunlight, R.string.settings_sunlight_hint, Modifier.weight(1f))
                    Switch(checked = sunlight, onCheckedChange = null)
                }
            }
            if (SHOWCASE_AVAILABLE) {
                SettingsLink(R.string.settings_showcase, R.string.settings_showcase_hint, onShowcase)
            }
            SettingsLink(R.string.settings_about, null, onAbout)
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, content = content)
}

@Composable
private fun SettingsLink(title: Int, hint: Int?, onClick: () -> Unit) {
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
private fun SettingsLabel(title: Int, hint: Int?, modifier: Modifier) {
    Column(modifier.padding(vertical = 12.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        if (hint != null) {
            Text(
                stringResource(hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
