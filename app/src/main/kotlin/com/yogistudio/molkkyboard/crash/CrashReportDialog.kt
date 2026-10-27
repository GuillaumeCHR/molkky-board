package com.yogistudio.molkkyboard.crash

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.R

/**
 * Après un plantage : l'app propose d'envoyer le rapport par e-mail et en montre **tout** le contenu avant. Les deux
 * choix effacent le rapport du téléphone.
 */
@Composable
internal fun CrashReportDialog(report: String, onHandled: () -> Unit) {
    val context = LocalContext.current
    val subject = stringResource(R.string.crash_subject)
    AlertDialog(
        onDismissRequest = onHandled,
        title = { Text(stringResource(R.string.crash_title)) },
        text = {
            Column {
                Text(stringResource(R.string.crash_text))
                Text(
                    report,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                composeEmail(context, subject, report)
                onHandled()
            }) { Text(stringResource(R.string.crash_send)) }
        },
        dismissButton = { TextButton(onClick = onHandled) { Text(stringResource(R.string.crash_ignore)) } },
    )
}
