package com.yogistudio.molkkyboard.feature.nearby

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionButton
import com.yogistudio.molkkyboard.core.designsystem.component.BigActionKind
import com.yogistudio.molkkyboard.core.designsystem.component.MBTopBar
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.nearby.NearbyError
import com.yogistudio.molkkyboard.core.nearby.NearbyRole
import com.yogistudio.molkkyboard.core.nearby.NearbyState
import com.yogistudio.molkkyboard.core.nearby.NetworkMode
import com.yogistudio.molkkyboard.core.sync.GuestDevice
import com.yogistudio.molkkyboard.core.sync.JoinCandidate

/**
 * « Inviter des téléphones » (organisateur) : choix du réseau, grand QR code renouvelé après chaque arrivée, demandes
 * à valider (« C'est Léa », « Nouveau joueur », « Refuser »), téléphones connectés et exclusion, arrêt du partage.
 */
@Composable
fun InviteScreen(gameId: String, onBack: () -> Unit, viewModel: NearbyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val players by viewModel.freePlayers.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(NetworkMode.HOTSPOT) }
    val permissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Refusées : le partage échoue avec un message clair, la partie continue sur ce téléphone.
            viewModel.share(gameId, mode)
        }
    val sharing = state.role == NearbyRole.HOST && state.gameId == gameId
    Column(Modifier.fillMaxSize()) {
        MBTopBar(stringResource(R.string.invite_title), onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MBDimens.ScreenPadding)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!sharing || state.error != null) {
                Text(stringResource(R.string.invite_intro), style = MaterialTheme.typography.bodyLarge)
                Section(R.string.invite_section_invite) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(mode == NetworkMode.HOTSPOT, {
                            mode = NetworkMode.HOTSPOT
                        }, { Text(stringResource(R.string.invite_hotspot)) })
                        FilterChip(mode == NetworkMode.SAME_WIFI, {
                            mode = NetworkMode.SAME_WIFI
                        }, { Text(stringResource(R.string.invite_same_wifi)) })
                    }
                    Hint(
                        if (mode ==
                            NetworkMode.HOTSPOT
                        ) {
                            R.string.invite_hotspot_hint
                        } else {
                            R.string.invite_same_wifi_hint
                        },
                    )
                    state.error?.let { ErrorText(it) }
                    BigActionButton(
                        stringResource(R.string.invite_start),
                        { permissions.launch(sharePermissions(mode)) },
                        BigActionKind.Primary,
                        Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Sharing(state, players, viewModel)
            }
        }
    }
}

@Composable
private fun Sharing(state: NearbyState, players: List<PartyPlayer>, viewModel: NearbyViewModel) {
    val link = state.link
    if (state.starting || link == null) {
        CircularProgressIndicator()
        Hint(R.string.invite_starting)
        return
    }
    KeepBright()
    state.candidates.forEach { CandidateCard(it, players, viewModel) }
    Section(R.string.invite_section_invite) {
        Text(stringResource(R.string.invite_scan), style = MaterialTheme.typography.bodyLarge)
        val description = stringResource(R.string.invite_qr_description)
        val bitmap = remember(link) { qrBitmap(link) }
        Box(
            Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(12.dp)
                .semantics { contentDescription = description },
        ) {
            Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(QR_SIZE.dp))
        }
        state.ssid?.let { Hint(R.string.invite_network, it) }
        TextButton(onClick = viewModel::renewInvite, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
            Text(stringResource(R.string.invite_renew))
        }
    }
    Section(R.string.invite_section_phones) {
        Text(stringResource(R.string.invite_phones, state.guestsOnline), style = MaterialTheme.typography.titleSmall)
        if (state.guests.isEmpty()) Hint(R.string.invite_no_guest)
        state.guests.forEach { GuestRow(it, players, viewModel) }
        OutlinedButton(onClick = viewModel::stop, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
            Text(stringResource(R.string.invite_stop))
        }
    }
}

/** Un téléphone invité : pseudo, état, joueur qu'il saisit, et « Déconnecter ». */
@Composable
private fun GuestRow(guest: GuestDevice, players: List<PartyPlayer>, viewModel: NearbyViewModel) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            val player = players.firstOrNull { it.id == guest.player }?.name
            Text(
                stringResource(
                    if (guest.connection != null) R.string.invite_guest_online else R.string.invite_guest_offline,
                    guest.nickname,
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            if (player != null) Hint(R.string.invite_guest_player, player) else Hint(R.string.invite_guest_referee)
            TextButton(
                onClick = { viewModel.exclude(guest.device) },
                modifier = Modifier.align(Alignment.End).heightIn(min = MBDimens.MinTouch),
            ) {
                Text(stringResource(R.string.invite_exclude), color = MolkkyBoardTheme.colors.coralText)
            }
        }
    }
}

/** Pavé titré de l'écran « Gérer les téléphones ». */
@Composable
private fun Section(title: Int, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(MBDimens.Gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
            content()
        }
    }
}

/** « 🦊 Léa veut rejoindre la partie » : associer à un joueur, ajouter un nouveau joueur, ou refuser. */
@Composable
private fun CandidateCard(candidate: JoinCandidate, players: List<PartyPlayer>, viewModel: NearbyViewModel) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MolkkyBoardTheme.colors.saffron.copy(alpha = CANDIDATE_ALPHA),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.invite_candidate, candidate.nickname),
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                players.forEach { player ->
                    OutlinedButton(onClick = {
                        viewModel.accept(candidate, player.id)
                    }, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
                        Text(stringResource(R.string.invite_is_player, player.name))
                    }
                }
                OutlinedButton(onClick = {
                    viewModel.acceptAsNewPlayer(candidate)
                }, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
                    Text(stringResource(R.string.invite_new_player))
                }
                // Arbitre : compte les points de tout le monde sans jouer.
                OutlinedButton(onClick = {
                    viewModel.acceptAsReferee(candidate)
                }, modifier = Modifier.heightIn(min = MBDimens.MinTouch)) {
                    Text(stringResource(R.string.invite_referee))
                }
                TextButton(
                    onClick = { viewModel.refuse(candidate) },
                    modifier = Modifier.heightIn(min = MBDimens.MinTouch),
                ) {
                    Text(stringResource(R.string.invite_refuse), color = MolkkyBoardTheme.colors.coralText)
                }
            }
        }
    }
}

@Composable
internal fun ErrorText(error: NearbyError) {
    Text(
        stringResource(
            when (error) {
                NearbyError.NO_NETWORK -> R.string.nearby_error_no_network
                NearbyError.HOTSPOT_FAILED -> R.string.nearby_error_hotspot
                NearbyError.SERVER_FAILED -> R.string.nearby_error_server
                NearbyError.INVALID_LINK -> R.string.nearby_error_link
                NearbyError.NETWORK_UNAVAILABLE -> R.string.nearby_error_network
                NearbyError.WRONG_HOST -> R.string.nearby_error_wrong_host
            },
        ),
        style = MaterialTheme.typography.bodyLarge,
        color = MolkkyBoardTheme.colors.coralText,
    )
}

@Composable
internal fun Hint(text: Int, vararg args: Any) {
    Text(
        stringResource(text, *args),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Écran en pleine luminosité tant que le QR code est affiché (lecture plus facile en plein soleil). */
@Composable
private fun KeepBright() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        val window = activity.window
        val previous = window.attributes.screenBrightness
        window.attributes =
            window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
        onDispose { window.attributes = window.attributes.apply { screenBrightness = previous } }
    }
}

/** Permissions du partage, demandées au moment de l'usage (aucune n'est la localisation). */
private fun sharePermissions(mode: NetworkMode): Array<String> =
    buildList {
        if (mode == NetworkMode.HOTSPOT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= ANDROID_17) add(ACCESS_LOCAL_NETWORK)
    }.toTypedArray()

private fun qrBitmap(text: String): Bitmap {
    val matrix =
        QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            QR_PIXELS,
            QR_PIXELS,
            mapOf(EncodeHintType.MARGIN to 1),
        )
    val pixels = IntArray(QR_PIXELS * QR_PIXELS) { i -> if (matrix[i % QR_PIXELS, i / QR_PIXELS]) BLACK else WHITE }
    return Bitmap.createBitmap(pixels, QR_PIXELS, QR_PIXELS, Bitmap.Config.RGB_565)
}

internal const val ANDROID_17 = 37
internal const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
private const val QR_SIZE = 260
private const val QR_PIXELS = 600
private const val BLACK = -0x1000000
private const val WHITE = -0x1
private const val CANDIDATE_ALPHA = 0.35f
