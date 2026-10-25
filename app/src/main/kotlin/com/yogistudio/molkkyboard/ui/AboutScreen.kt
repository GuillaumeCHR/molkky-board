package com.yogistudio.molkkyboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.yogistudio.molkkyboard.BuildConfig
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.designsystem.component.MolkkyBoardLogotype
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens

/** « À propos » : logotype, version, éditeur. Le logo Yogi Studio y sera ajouté au J5. */
@Composable
internal fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.settings_about), onBack)
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MolkkyBoardLogotype(fontSize = 44.sp)
            Text(
                stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.about_publisher), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.about_privacy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
