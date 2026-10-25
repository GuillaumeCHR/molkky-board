package com.yogistudio.molkkyboard.feature.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.TeamId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures du vrai écran de jeu, en clair et en sombre, et aux tailles de police 1,0 / 1,5 / 2,0 :
 * aucun score ne doit être tronqué et tout tient sur un écran.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class GameScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val teams =
        listOf(
            TeamUi(TeamId("A"), "Alice", 0, 38, 1, active = true, out = false),
            TeamUi(TeamId("B"), "Bruno", 1, 27, 0, active = false, out = false),
            TeamUi(TeamId("C"), "Chloé", 2, 12, 2, active = false, out = false),
        )
    private val playing =
        GameUiState(
            loading = false,
            teams = teams,
            currentTeam = teams.first(),
            currentPlayerName = "Alice",
            selection = Selection.Pins(setOf(12, 4)),
            preview = Preview(PreviewKind.POINTS, 2, 40),
        )

    private fun capture(name: String, state: GameUiState, dark: Boolean = false, fontScale: Float = 1f) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MolkkyBoardTheme(darkTheme = dark, reducedMotion = true) { Screen(state) }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Composable
    private fun Screen(state: GameUiState) = GameContent(state, GameActions())

    @Test
    fun jeuClair() = capture("jeu_clair", playing)

    @Test
    fun jeuSombre() = capture("jeu_sombre", playing, dark = true)

    @Test
    fun jeuPolice150() = capture("jeu_police_150", playing, fontScale = 1.5f)

    @Test
    fun jeuPolice200() = capture("jeu_police_200", playing, fontScale = 2f)

    @Test
    fun jeuRapide() =
        capture(
            "jeu_rapide",
            playing.copy(
                inputMode = InputMode.QUICK,
                selection = Selection.Quick(7),
                preview = Preview(PreviewKind.OVERSHOOT, 7, 25),
            ),
        )

    @Test
    fun jeuRapidePolice200() =
        capture(
            "jeu_rapide_police_200",
            playing.copy(inputMode = InputMode.QUICK, selection = Selection.None, preview = Preview()),
            fontScale = 2f,
        )

    @Test
    fun finDePartie() = capture("fin_de_partie", playing.copy(finished = true, winnerName = "Alice", ranking = teams))
}
