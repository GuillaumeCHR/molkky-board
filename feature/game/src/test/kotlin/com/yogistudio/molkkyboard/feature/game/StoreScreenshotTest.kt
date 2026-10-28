package com.yogistudio.molkkyboard.feature.game

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.TeamId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captures en anglais pour la fiche du Play Store (`tools/store/compose_screenshots.py` les met en page). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
class StoreScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val teams =
        listOf(
            TeamUi(TeamId("A"), "Alice", 0, 38, 1, active = true, out = false, remaining = 12, avatar = "🦊"),
            TeamUi(TeamId("B"), "Bruno", 1, 27, 0, active = false, out = false, remaining = 23, avatar = "🐻"),
            TeamUi(TeamId("C"), "Chloe", 2, 12, 2, active = false, out = false, remaining = 38),
        )
    private val eliminated =
        TeamUi(TeamId("D"), "Dan", 3, 21, 3, active = false, out = true, eliminated = true, remaining = 29)
    private val playing =
        GameUiState(
            loading = false,
            teams = teams,
            currentTeam = teams.first(),
            currentPlayerName = "Alice",
            round = 4,
            selection = Selection.Pins(setOf(12, 4)),
            preview = Preview(PreviewKind.POINTS, 2, 40),
        )
    private val threePhones = ConnectionBadge(phones = 3, online = true)

    private fun shot(name: String) = "src/test/screenshots/store/$name.png"

    @Test
    fun jeu() {
        compose.setContent {
            MolkkyBoardTheme(darkTheme = false, reducedMotion = true) {
                GameContent(playing, GameActions(), connection = threePhones)
            }
        }
        compose.onRoot().captureRoboImage(shot("game"))
    }

    @Test
    fun pleinSoleil() {
        compose.setContent {
            MolkkyBoardTheme(sunlight = true, reducedMotion = true) {
                GameContent(playing, GameActions(), connection = threePhones)
            }
        }
        compose.onRoot().captureRoboImage(shot("sunlight"))
    }

    @Test
    fun sombre() {
        compose.setContent {
            MolkkyBoardTheme(darkTheme = true, reducedMotion = true) { GameContent(playing, GameActions()) }
        }
        compose.onRoot().captureRoboImage(shot("dark"))
    }

    @Test
    fun tableauDesScores() {
        compose.setContent {
            MolkkyBoardTheme(darkTheme = false, reducedMotion = true) {
                GameContent(
                    playing.copy(teams = teams + eliminated),
                    GameActions(),
                    scoreboardOpen = true,
                    connection = threePhones,
                )
            }
        }
        compose.waitForIdle()
        captureScreenRoboImage(shot("scoreboard"))
    }

    @Test
    fun finDePartie() {
        val winner = teams.first().copy(score = 50, remaining = 0, active = false)
        val ranked = listOf(winner, teams[1].copy(active = false), teams[2])
        val end =
            EndUi(
                winner = winner,
                winnerThrows = 7,
                newRecord = true,
                teamStats =
                    listOf(
                        TeamStatsUi(ranked[0], 7, 0, 7.1f),
                        TeamStatsUi(ranked[1], 7, 1, 3.9f),
                        TeamStatsUi(ranked[2], 6, 2, 2f),
                    ),
                series =
                    SeriesUi(
                        sets = 3,
                        rows =
                            listOf(
                                SeriesRowUi("Alice", 0, 2, listOf(50, 31, 50), 131),
                                SeriesRowUi("Bruno", 1, 1, listOf(22, 50, 27), 99),
                                SeriesRowUi("Chloe", 2, 0, listOf(18, 40, 12), 70),
                            ),
                    ),
            )
        val finished =
            playing.copy(finished = true, winnerName = "Alice", ranking = ranked, teams = ranked, round = 7, end = end)
        compose.setContent {
            MolkkyBoardTheme(darkTheme = false, reducedMotion = true) { GameContent(finished, GameActions()) }
        }
        compose.onRoot().captureRoboImage(shot("end"))
    }
}
