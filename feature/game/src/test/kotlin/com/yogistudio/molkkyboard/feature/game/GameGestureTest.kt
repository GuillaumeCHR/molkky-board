package com.yogistudio.molkkyboard.feature.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.TeamId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Gestes et messages de l'écran de jeu : le tableau des scores s'ouvre au glissement, le coach parle. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
class GameGestureTest {
    @get:Rule
    val compose = createComposeRule()

    private val teams =
        listOf(
            TeamUi(TeamId("A"), "Alice", 0, 43, 0, active = true, out = false, remaining = 7),
            TeamUi(TeamId("B"), "Bruno", 1, 27, 0, active = false, out = false, remaining = 23),
        )
    private val playing =
        GameUiState(loading = false, teams = teams, currentTeam = teams.first(), currentPlayerName = "Alice")

    private fun show(state: GameUiState = playing) =
        compose.setContent { MolkkyBoardTheme(reducedMotion = true) { GameContent(state, GameActions()) } }

    @Test
    fun `glisser le bandeau vers le haut ouvre le tableau des scores`() {
        show()
        compose.onNodeWithTag(SCORES_STRIP_TAG).performTouchInput { swipeUp() }
        compose.onNodeWithText("Tableau des scores").assertIsDisplayed()
    }

    @Test
    fun `glisser vers le bas depuis la puce d'un joueur l'ouvre aussi`() {
        show()
        compose.onNodeWithText("Bruno").performTouchInput { swipeDown() }
        compose.onNodeWithText("Tableau des scores").assertIsDisplayed()
    }

    @Test
    fun `toucher le bandeau l'ouvre toujours`() {
        show()
        compose.onNodeWithTag(SCORES_STRIP_TAG).performClick()
        compose.onNodeWithText("Tableau des scores").assertIsDisplayed()
    }
}
