package com.yogistudio.molkkyboard.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.yogistudio.molkkyboard.R

/**
 * Android 13+ : propose une fois, en arrivant sur une partie, d'afficher la notification de partie en cours,
 * avec une explication avant la demande système. Refuser ne change rien au jeu.
 */
@Composable
internal fun NotificationPermissionPrompt(
    prompted: Boolean?,
    enabled: Boolean,
    onPrompted: () -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || prompted != false || !enabled) return
    val context = LocalContext.current
    val granted =
        remember {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
    if (granted) return
    var visible by remember { mutableStateOf(true) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onPrompted() }
    if (!visible) return
    AlertDialog(
        onDismissRequest = {
            visible = false
            onPrompted()
        },
        title = { Text(stringResource(R.string.notification_permission_title)) },
        text = { Text(stringResource(R.string.notification_permission_text)) },
        confirmButton = {
            TextButton(onClick = {
                visible = false
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text(stringResource(R.string.notification_permission_allow)) }
        },
        dismissButton = {
            TextButton(onClick = {
                visible = false
                onPrompted()
            }) { Text(stringResource(R.string.notification_permission_later)) }
        },
    )
}
