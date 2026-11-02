package com.yogistudio.molkkyboard.feature.game

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.scoring.SeriesMark
import com.yogistudio.molkkyboard.core.scoring.SeriesPoint
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Courbe de la partie : un tap passe l'animation ; la courbe posée, chaque tour se consulte sans geste précis. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
class EndCurveTest {
    @get:Rule
    val compose = createComposeRule()

    private val lea = TeamUi(TeamId("A"), "Léa", 0, 50, 0, active = false, out = false)
    private val tom = TeamUi(TeamId("B"), "Tom", 1, 29, 0, active = false, out = false)
    private val curve =
        CurveUi(
            lines =
                listOf(
                    CurveLineUi(
                        lea,
                        listOf(
                            SeriesPoint(0, 0),
                            SeriesPoint(1, 12),
                            SeriesPoint(2, 38),
                            SeriesPoint(3, 25, SeriesMark.RESET),
                        ) +
                            SeriesPoint(4, 50, SeriesMark.WIN),
                        winner = true,
                    ),
                    CurveLineUi(
                        tom,
                        listOf(SeriesPoint(0, 0), SeriesPoint(1, 7), SeriesPoint(2, 17), SeriesPoint(3, 29)),
                        winner = false,
                    ),
                ),
            rounds = 4,
            target = 50,
            highlight = 0 to 3,
        )

    private val summary = "Courbe de la partie sur 4 tours : Léa gagne au tour 4, Léa retombe à 25 au tour 3"

    private fun show(celebrating: Boolean = false) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MolkkyBoardTheme(reducedMotion = false) { CurveCard(curve, celebrating = celebrating, sounds = false) }
        }
    }

    @Test
    fun `un tap passe l'animation et affiche les scores du dernier tour`() {
        show()
        compose.mainClock.advanceTimeBy(300)
        // Animation en cours : ni sélecteur de tour ni « Rejouer ».
        compose.onNodeWithText("Rejouer").assertDoesNotExist()
        compose.onNode(hasContentDescription(summary)).performSemanticsAction(SemanticsActions.OnClick)
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Rejouer").assertIsDisplayed()
        compose.onNodeWithText("Tour 4").assertIsDisplayed()
        compose.onNodeWithText("Léa : 50").assertIsDisplayed()
    }

    @Test
    fun `chaque tour se consulte avec les fleches`() {
        show()
        compose.mainClock.advanceTimeBy(3_500)
        // Animation finie : l'horloge reprend son cours pour traiter le toucher.
        compose.mainClock.autoAdvance = true
        compose.onNodeWithContentDescription("Tour précédent").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Tour 3").assertIsDisplayed()
        compose.onNodeWithText("Léa : 25").assertIsDisplayed()
        compose.onNodeWithText("Tom : 29").assertIsDisplayed()
    }

    @Test
    fun `l'animation attend la fin de la sequence de victoire`() {
        show(celebrating = true)
        compose.mainClock.advanceTimeBy(3_500)
        compose.onNodeWithText("Rejouer").assertDoesNotExist()
    }
}
