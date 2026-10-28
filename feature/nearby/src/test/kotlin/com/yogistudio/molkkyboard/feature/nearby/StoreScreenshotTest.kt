package com.yogistudio.molkkyboard.feature.nearby

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.nearby.NearbyRole
import com.yogistudio.molkkyboard.core.nearby.NearbyState
import com.yogistudio.molkkyboard.core.nearby.NetworkMode
import com.yogistudio.molkkyboard.core.sync.ConnectionId
import com.yogistudio.molkkyboard.core.sync.DeviceId
import com.yogistudio.molkkyboard.core.sync.GuestDevice
import com.yogistudio.molkkyboard.core.sync.JoinCandidate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Écran « Inviter » en anglais (QR code, demande d'arrivée, téléphone connecté) pour la fiche du Play Store. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
class StoreScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun inviter() {
        // Lien factice : adresse privée, empreinte et jeton fictifs (aucun réseau n'est ouvert).
        val link = "molkkyboard://join#v=1&h=192.168.49.1&p=48213&fp=${"ab".repeat(32)}&t=${"cd".repeat(16)}"
        val state =
            NearbyState(
                role = NearbyRole.HOST,
                gameId = "g1",
                link = link,
                networkMode = NetworkMode.HOTSPOT,
                ssid = "DIRECT-MolkkyBoard",
                candidates = listOf(JoinCandidate(ConnectionId(2L), "Chloe", null)),
                guests = listOf(GuestDevice(DeviceId("d1"), "Bruno", PlayerId("b"), ConnectionId(1L), "token")),
            )
        val players = listOf(PartyPlayer(PlayerId("b"), "Bruno"), PartyPlayer(PlayerId("c"), "Chloe"))
        compose.setContent {
            MolkkyBoardTheme(darkTheme = false, reducedMotion = true) {
                InviteContent(state, players, true, NetworkMode.HOTSPOT, {}, {}, {}, InviteActions())
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/store/invite.png")
    }
}
