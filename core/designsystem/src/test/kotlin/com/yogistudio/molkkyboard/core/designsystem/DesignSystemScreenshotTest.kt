package com.yogistudio.molkkyboard.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.yogistudio.molkkyboard.core.designsystem.catalog.DesignSystemCatalog
import com.yogistudio.molkkyboard.core.designsystem.catalog.GameScreenMockup
import com.yogistudio.molkkyboard.core.designsystem.component.EliminatedStamp
import com.yogistudio.molkkyboard.core.designsystem.component.FitToScreen
import com.yogistudio.molkkyboard.core.designsystem.component.ImpactBurst
import com.yogistudio.molkkyboard.core.designsystem.component.MissIndicator
import com.yogistudio.molkkyboard.core.designsystem.component.RoundBadge
import com.yogistudio.molkkyboard.core.designsystem.component.ScoreboardRow
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.designsystem.theme.TeamPalette
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures de référence du design system (J2), en clair, en sombre et en « Plein soleil ».
 * Enregistrement : `./gradlew :core:designsystem:recordRoborazziDebug` ; vérification : `verifyRoborazziDebug`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignSystemScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun capture(
        name: String,
        dark: Boolean,
        sunlight: Boolean = false,
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            // Taille de police système simulée (1,0 / 1,5 / 2,0) : aucun score ne doit être tronqué.
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MolkkyBoardTheme(darkTheme = dark, sunlight = sunlight, reducedMotion = false, content = content)
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    @Config(qualifiers = CATALOG)
    fun catalogueClair() = capture("catalogue_clair", dark = false) { DesignSystemCatalog() }

    @Test
    @Config(qualifiers = CATALOG)
    fun catalogueSombre() = capture("catalogue_sombre", dark = true) { DesignSystemCatalog() }

    @Test
    @Config(qualifiers = CATALOG)
    fun cataloguePleinSoleil() =
        capture(
            "catalogue_plein_soleil",
            dark = false,
            sunlight = true,
        ) { DesignSystemCatalog() }

    @Test
    @Config(qualifiers = PHONE)
    fun tourEtTableauClair() = capture("tour_tableau_clair", dark = false) { RoundAndScoreboard() }

    @Test
    @Config(qualifiers = PHONE)
    fun tourEtTableauSombre() = capture("tour_tableau_sombre", dark = true) { RoundAndScoreboard() }

    @Test
    @Config(qualifiers = PHONE)
    fun tourEtTableauPolice200() =
        capture(
            "tour_tableau_police_200",
            dark = false,
            fontScale = 2f,
        ) { RoundAndScoreboard() }

    @Test
    @Config(qualifiers = PHONE)
    fun bulleDouzeEtTampon() = capture("bulle_douze_tampon", dark = false) { BurstAndStamp() }

    @Test
    @Config(qualifiers = PHONE)
    fun jeuClair() = capture("jeu_clair", dark = false) { GameScreenMockup() }

    @Test
    @Config(qualifiers = PHONE)
    fun jeuSombre() = capture("jeu_sombre", dark = true) { GameScreenMockup() }

    @Test
    @Config(qualifiers = PHONE)
    fun jeuRapideClair() = capture("jeu_rapide_clair", dark = false) { GameScreenMockup(quickMode = true) }

    @Test
    @Config(qualifiers = PHONE)
    fun jeuPolice150() = capture("jeu_police_150", dark = false, fontScale = 1.5f) { GameScreenMockup() }

    @Test
    @Config(qualifiers = PHONE)
    fun jeuPolice200() = capture("jeu_police_200", dark = false, fontScale = 2f) { GameScreenMockup() }

    @Test
    @Config(qualifiers = PHONE)
    fun jeuRapidePolice200() =
        capture("jeu_rapide_police_200", dark = false, fontScale = 2f) {
            GameScreenMockup(quickMode = true)
        }

    @Test
    @Config(qualifiers = SMALL_PHONE)
    fun jeuPetitEcranAjuste() =
        capture("jeu_petit_ecran_ajuste", dark = false) {
            FitToScreen { GameScreenMockup() }
        }

    private companion object {
        const val CATALOG = "fr-w420dp-h1500dp-xhdpi"
        const val PHONE = "fr-w411dp-h891dp-xxhdpi"
        const val SMALL_PHONE = "fr-w360dp-h640dp-xhdpi"
    }
}

@Composable
private fun RoundAndScoreboard() {
    val teams = TeamPalette.colors(dark = false)
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundBadge(1)
                RoundBadge(3)
                RoundBadge(12)
            }
            MissIndicator(2, warning = true)
            ScoreboardRow(1, "Les Bouleaux", teams[0], 47, 3, 0, 3, active = true, members = listOf("Léa", "Hugo"))
            ScoreboardRow(2, "Bruno", teams[1], 38, 12, 2, 3)
            ScoreboardRow(2, "Chloé-Marguerite de la Forêt", teams[2], 38, 12, 1, 3)
            ScoreboardRow(null, "Denis", teams[3], 21, 29, 3, 3, status = "Éliminé")
        }
    }
}

@Composable
private fun BurstAndStamp() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ImpactBurst("12 !")
            ImpactBurst("12 !", caption = "Strike !", size = 180.dp)
            EliminatedStamp("Éliminé")
        }
    }
}
