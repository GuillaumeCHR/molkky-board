package com.yogistudio.molkkyboard.feature.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onLast
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.MissPenalty
import com.yogistudio.molkkyboard.core.model.TeamId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Contrôles d'accessibilité automatiques (Accessibility Test Framework, via Roborazzi) sur les écrans du jeu :
 * cibles tactiles d'au moins 48 dp, éléments cliquables nommés pour TalkBack, contrastes, libellés en double.
 * Retour C13 du test de rattrapage : TalkBack n'a pas été essayé sur téléphone, cette vérification le remplace en
 * partie (elle ne juge pas la pertinence des textes lus).
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
class AccessibilityTest {
    @get:Rule
    val compose = createComposeRule()

    private val teams =
        listOf(
            TeamUi(TeamId("A"), "Alice", 0, 38, 2, active = true, out = false, remaining = 12, avatar = "🦊"),
            TeamUi(TeamId("B"), "Bruno", 1, 27, 0, active = false, out = false, remaining = 23, avatar = "🐻"),
            TeamUi(TeamId("C"), "Chloé", 2, 12, 2, active = false, out = false, remaining = 38),
        )
    private val playing =
        GameUiState(
            loading = false,
            teams = teams,
            currentTeam = teams.first(),
            currentPlayerName = "Alice",
            selection = Selection.Pins(setOf(12, 4)),
            preview = Preview(PreviewKind.POINTS, 2, 40),
            canUndo = true,
            missWarning = true,
            missPenalty = MissPenalty.Eliminate,
        )

    /**
     * Échoue sur toute erreur ATF ; les avertissements sont seulement journalisés : le contraste d'un emoji (avatar)
     * est un faux positif, ses couleurs étant celles du dessin.
     */
    private fun check(dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent { MolkkyBoardTheme(darkTheme = dark, reducedMotion = true) { content() } }
        // La dernière fenêtre : l'écran, ou la feuille (tableau des scores) ouverte par-dessus.
        compose.onAllNodes(isRoot()).onLast().checkRoboAccessibility(
            roborazziATFAccessibilityCheckOptions =
                RoborazziATFAccessibilityCheckOptions(
                    checker = RoborazziATFAccessibilityChecker(preset = AccessibilityCheckPreset.LATEST),
                    failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Error,
                ),
        )
    }

    @Test
    fun jeuQuilles() = check { GameContent(playing, GameActions()) }

    @Test
    fun jeuSombre() = check(dark = true) { GameContent(playing, GameActions()) }

    @Test
    fun jeuRapide() =
        check {
            GameContent(
                playing.copy(inputMode = InputMode.QUICK, selection = Selection.Quick(7)),
                GameActions(),
            )
        }

    @Test
    fun tableauDesScores() = check { GameContent(playing, GameActions(), scoreboardOpen = true) }

    @Test
    fun finDePartie() =
        check {
            val winner = teams.first().copy(score = 50, remaining = 0, active = false)
            val ranked = listOf(winner, teams[1].copy(active = false), teams[2])
            GameContent(
                playing.copy(
                    finished = true,
                    winnerName = "Alice",
                    ranking = ranked,
                    teams = ranked,
                    end = EndUi(winner = winner, winnerThrows = 7),
                ),
                GameActions(),
            )
        }

    @Test
    fun regles() = check { RulesScreen(onBack = {}) }

    @Test
    fun placement() = check { PlacementScreen(onBack = {}) }
}
