package com.yogistudio.molkkyboard.feature.intro

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.yogistudio.molkkyboard.core.designsystem.component.MolkkyBoardLogotype
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Une capture par étape du storyboard (intro figée à un instant donné, graine fixe : images stables).
 * Enregistrement : `./gradlew :feature:intro:recordRoborazziDebug`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class IntroScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun capture(name: String, timeMs: Long, dark: Boolean = false) {
        compose.setContent {
            MolkkyBoardTheme(darkTheme = dark, reducedMotion = false) {
                IntroScreen(
                    loadState = IntroLoadState.Ready,
                    onFinished = {},
                    logotype = { MolkkyBoardLogotype(it, fontSize = 52.sp) },
                    frameTimeMs = timeMs,
                )
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun etape1MiseEnPlace() = capture("intro_1_mise_en_place", 450)

    @Test
    fun etape2Lancer() = capture("intro_2_lancer", 1_050)

    @Test
    fun etape3Impact() = capture("intro_3_impact", 1_280)

    @Test
    fun etape4Signature() = capture("intro_4_signature", 3_200)

    @Test
    fun etape5Logotype() = capture("intro_5_logotype", 4_300)

    @Test
    fun etape3ImpactSombre() = capture("intro_3_impact_sombre", 1_280, dark = true)
}
