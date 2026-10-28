package com.yogistudio.molkkyboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.yogistudio.molkkyboard.BuildConfig
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.designsystem.brand.YogiStudioLogo
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.component.MolkkyBoardLogotype
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.crash.SUPPORT_EMAIL
import com.yogistudio.molkkyboard.crash.composeEmail
import com.yogistudio.molkkyboard.crash.deviceSummary

/**
 * « À propos » : logotype, version, éditeur, encart « Vie privée » (chaque phrase est vraie et vérifiable), crédits
 * (bloc « Crédits », la liste des bibliothèques sur son propre écran) et, dans les versions de test, « Envoyer un
 * retour ».
 */
@Composable
internal fun AboutScreen(onBack: () -> Unit, onLicenses: () -> Unit) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.settings_about), onBack)
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .navigationBarsPadding()
                    .padding(MBDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MolkkyBoardLogotype(fontSize = 44.sp)
            Text(
                stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.about_publisher), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.about_contact, SUPPORT_EMAIL), style = MaterialTheme.typography.bodyMedium)
            YogiStudioLogo(markSize = 56.dp)
            AboutCard(R.string.about_privacy_title) {
                // L'essentiel en tête, en gras : rien n'est collecté, et l'app ne peut de toute façon rien envoyer.
                Text(
                    stringResource(R.string.about_privacy_headline),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                listOf(
                    R.string.about_privacy_no_ads,
                    R.string.about_privacy_local,
                    R.string.about_privacy_control,
                    R.string.about_privacy_open_source,
                    R.string.about_license,
                ).forEach {
                    Text(stringResource(it), style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (BuildConfig.FEEDBACK) {
                val subject = stringResource(R.string.about_feedback_subject)
                val body = stringResource(R.string.about_feedback_body, deviceSummary())
                AboutCard(R.string.about_feedback_title) {
                    Text(stringResource(R.string.about_feedback_hint), style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(
                        onClick = { composeEmail(context, subject, body) },
                        modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                    ) { Text(stringResource(R.string.about_feedback)) }
                }
            }
            CreditsSection(scroll, onLicenses)
        }
    }
}

@Composable
private fun AboutCard(title: Int, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(MBDimens.SmallGap)) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.semantics { heading() },
            )
            content()
        }
    }
}

/** Licences des bibliothèques open source, collectées au build (AboutLibraries, sans accès réseau). */
@Composable
internal fun LicensesScreen(onBack: () -> Unit) {
    val libraries by produceLibraries(R.raw.aboutlibraries)
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.about_licenses), onBack)
        LibrariesContainer(libraries, Modifier.fillMaxSize())
    }
}
