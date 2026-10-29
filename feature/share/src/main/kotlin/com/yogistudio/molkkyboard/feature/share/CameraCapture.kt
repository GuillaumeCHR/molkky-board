package com.yogistudio.molkkyboard.feature.share

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Prise de vue par l'appareil photo du téléphone. L'app déclare la permission caméra (scan du QR code de la partie à
 * côté) : Android exige alors qu'elle soit accordée avant de lancer l'appareil photo, sinon il refuse avec une
 * exception. Elle est donc demandée au moment de la prise de vue ; refusée, rien n'est pris ([onResult] reçoit false).
 */
@Composable
internal fun rememberTakePicture(onResult: (Boolean) -> Unit): (Uri) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), onResult)
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val uri = pending
            pending = null
            if (granted && uri != null) camera.launch(uri) else onResult(false)
        }
    return { uri ->
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            camera.launch(uri)
        } else {
            pending = uri
            permission.launch(Manifest.permission.CAMERA)
        }
    }
}
