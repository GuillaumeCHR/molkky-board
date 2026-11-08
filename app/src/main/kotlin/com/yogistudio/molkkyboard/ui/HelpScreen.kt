package com.yogistudio.molkkyboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.crash.SUPPORT_EMAIL
import com.yogistudio.molkkyboard.crash.composeEmail
import com.yogistudio.molkkyboard.crash.deviceSummary
import com.yogistudio.molkkyboard.feature.game.RulesContent

/**
 * Aide (accueil, Réglages) : questions sur l'utilisation de l'app, puis les règles et leur FAQ, le tout couvert par la
 * même recherche ; en bas, une question à Yogi Studio par e-mail (l'utilisateur relit avant d'envoyer).
 */
@Composable
internal fun HelpScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.help_title), onBack)
        RulesContent(Modifier.navigationBarsPadding(), appHelp = true, footer = { ContactCard() })
    }
}

@Composable
private fun ContactCard() {
    val context = LocalContext.current
    val subject = stringResource(R.string.help_contact_subject)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.help_contact_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.help_contact, SUPPORT_EMAIL), style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(
                // Seuls la version et le modèle sont joints, pour situer la question ; rien d'autre.
                onClick = { composeEmail(context, subject, "\n\n— ${deviceSummary()}") },
                modifier = Modifier.heightIn(min = MBDimens.MinTouch),
            ) { Text(stringResource(R.string.help_contact_button)) }
        }
    }
}
