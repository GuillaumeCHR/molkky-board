package com.yogistudio.molkkyboard.feature.nearby

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionButton
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionKind
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.model.withoutUnsafeChars
import com.yogistudio.molkkyboard.core.nearby.NearbyRole
import com.yogistudio.molkkyboard.core.sync.GuestStatus
import com.yogistudio.molkkyboard.core.sync.JoinRejection
import zxingcpp.BarcodeReader
import java.util.concurrent.Executors

/**
 * « Rejoindre une partie » : on scanne le QR code de l'organisateur (sur le téléphone, sans réseau ni ML Kit), on
 * confirme son pseudo, puis on attend que l'organisateur accepte. Accepté, l'invité arrive sur l'écran de jeu.
 */
@Composable
fun JoinScreen(
    onBack: () -> Unit,
    onJoined: (String) -> Unit,
    initialLink: String? = null,
    viewModel: NearbyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Debug : une nouvelle invitation passée par adb remplace la précédente.
    var link by rememberSaveable(initialLink) { mutableStateOf(initialLink.orEmpty()) }
    var nickname by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (nickname.isEmpty()) nickname = viewModel.suggestedNickname() }
    val playing = state.role == NearbyRole.GUEST && state.guestStatus == GuestStatus.PLAYING
    LaunchedEffect(playing, state.gameId) {
        val id = state.gameId
        if (playing && id != null) onJoined(id)
    }
    val context = LocalContext.current
    var camera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { camera = it }
    val join = rememberJoin(viewModel)
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.join_title), onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MBDimens.ScreenPadding)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val busy = submitted && state.role == NearbyRole.GUEST && state.error == null && state.guestStatus !in DONE
            when {
                busy -> {
                    Waiting(state.guestStatus, state.connected, viewModel)
                }

                link.isEmpty() -> {
                    Hint(R.string.join_intro)
                    if (camera) {
                        Scanner(onFound = { link = it })
                    } else {
                        OutlinedButton(onClick = { permissions.launch(Manifest.permission.CAMERA) }) {
                            Text(stringResource(R.string.join_camera))
                        }
                    }
                }

                else -> {
                    Text(stringResource(R.string.join_found), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it.withoutUnsafeChars().take(MAX_NICKNAME) },
                        label = { Text(stringResource(R.string.join_nickname)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Hint(R.string.join_privacy)
                    BigActionButton(
                        stringResource(R.string.join_go),
                        {
                            submitted = true
                            join(link, nickname)
                        },
                        BigActionKind.Primary,
                        Modifier.fillMaxWidth(),
                        enabled = nickname.isNotBlank(),
                    )
                    TextButton(onClick = { link = "" }) { Text(stringResource(R.string.join_rescan)) }
                }
            }
            state.error?.takeIf { submitted }?.let { ErrorText(it) }
            state.rejection?.takeIf { submitted }?.let { Rejection(it) }
        }
    }
}

/** Android 17 : l'accès au réseau local est une permission, demandée au moment de rejoindre. Refusée, la connexion
 * échoue avec un message clair. */
@Composable
private fun rememberJoin(viewModel: NearbyViewModel): (String, String) -> Unit {
    var pending by remember { mutableStateOf<Pair<String, String>?>(null) }
    val localNetwork =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            pending?.let { (link, nickname) -> viewModel.join(link, nickname) }
        }
    return { link, nickname ->
        if (Build.VERSION.SDK_INT >= ANDROID_17) {
            pending = link to nickname
            localNetwork.launch(ACCESS_LOCAL_NETWORK)
        } else {
            viewModel.join(link, nickname)
        }
    }
}

@Composable
private fun Waiting(status: GuestStatus?, connected: Boolean, viewModel: NearbyViewModel) {
    CircularProgressIndicator()
    Text(
        stringResource(
            when {
                status == GuestStatus.WAITING_APPROVAL -> R.string.join_waiting
                connected -> R.string.join_connecting
                else -> R.string.join_network
            },
        ),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    Hint(R.string.join_help)
    TextButton(onClick = viewModel::stop) { Text(stringResource(R.string.join_cancel)) }
}

@Composable
private fun Rejection(reason: JoinRejection) {
    Text(
        stringResource(
            when (reason) {
                JoinRejection.REFUSED -> R.string.join_refused
                JoinRejection.TOKEN_INVALID -> R.string.join_token
                JoinRejection.FULL -> R.string.join_full
                JoinRejection.VERSION -> R.string.join_version
                JoinRejection.SESSION_INVALID -> R.string.join_session
            },
        ),
        style = MaterialTheme.typography.bodyLarge,
    )
}

/**
 * Viseur : CameraX + zxing-cpp, quelques images par seconde, nouvel essai en image inversée (reflets d'écran). Seuls
 * les liens d'invitation de Molkky Board sont retenus.
 */
@Composable
private fun Scanner(onFound: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val description = stringResource(R.string.join_viewfinder)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val reader =
        remember {
            BarcodeReader(
                BarcodeReader.Options().apply {
                    formats = setOf(BarcodeReader.Format.QR_CODE)
                    tryInvert = true
                    tryHarder = true
                },
            )
        }
    var lastScan by remember { mutableLongStateOf(0L) }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also { view ->
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                    val analysis =
                        ImageAnalysis
                            .Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                    analysis.setAnalyzer(executor) { image ->
                        image.use {
                            val now = System.currentTimeMillis()
                            if (now - lastScan < SCAN_INTERVAL_MS) return@use
                            lastScan = now
                            reader
                                .read(it)
                                .firstNotNullOfOrNull { result ->
                                    result.text?.takeIf { text -> text.startsWith(LINK_PREFIX) }
                                }?.let { text -> ContextCompat.getMainExecutor(ctx).execute { onFound(text) } }
                        }
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }, ContextCompat.getMainExecutor(context))
            }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .semantics { contentDescription = description },
    )
}

private val DONE = setOf(GuestStatus.REJECTED, GuestStatus.REMOVED, GuestStatus.ENDED)
private const val LINK_PREFIX = "molkkyboard://join#"
private const val SCAN_INTERVAL_MS = 250L
private const val MAX_NICKNAME = 20
