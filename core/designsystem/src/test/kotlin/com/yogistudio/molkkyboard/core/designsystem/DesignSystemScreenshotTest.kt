package com.yogistudio.molkkyboard.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.yogistudio.molkkyboard.core.designsystem.catalog.DesignSystemCatalog
import com.yogistudio.molkkyboard.core.designsystem.catalog.GameScreenMockup
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
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

    private companion object {
        const val CATALOG = "w420dp-h1500dp-xhdpi"
        const val PHONE = "w411dp-h891dp-xxhdpi"
    }
}
