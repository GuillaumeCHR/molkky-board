package com.yogistudio.molkkyboard.feature.game

import androidx.compose.runtime.Composable
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

/**
 * Captures de la fiche du Play Store, en français et en anglais (`tools/store/compose_screenshots.py` les met en
 * page). Les noms des joueurs suivent la langue de la fiche.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StoreScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val threePhones = ConnectionBadge(phones = 3, online = true)

    private class Names(
        val lang: String,
        val chloe: String,
        val out: String,
    )

    private val fr = Names("fr", "Chloé", "Denis")
    private val en = Names("en", "Chloe", "Dan")

    private fun teams(n: Names) =
        listOf(
            TeamUi(TeamId("A"), "Alice", 0, 38, 1, active = true, out = false, remaining = 12, avatar = "🦊"),
            TeamUi(TeamId("B"), "Bruno", 1, 27, 0, active = false, out = false, remaining = 23, avatar = "🐻"),
            TeamUi(TeamId("C"), n.chloe, 2, 12, 2, active = false, out = false, remaining = 38),
        )

    private fun playing(n: Names) =
        teams(n).let { teams ->
            GameUiState(
                loading = false,
                teams = teams,
                currentTeam = teams.first(),
                currentPlayerName = "Alice",
                round = 4,
                selection = Selection.Pins(setOf(12, 4)),
                preview = Preview(PreviewKind.POINTS, 2, 40),
            )
        }

    private fun capture(
        name: String,
        n: Names,
        dark: Boolean = false,
        sunlight: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            MolkkyBoardTheme(
                darkTheme = dark,
                sunlight = sunlight,
                reducedMotion = true,
                content = content,
            )
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/store/${name}_${n.lang}.png")
    }

    private fun game(n: Names) = capture("game", n) { GameContent(playing(n), GameActions(), connection = threePhones) }

    private fun sunlight(n: Names) =
        capture("sunlight", n, sunlight = true) { GameContent(playing(n), GameActions(), connection = threePhones) }

    private fun dark(n: Names) = capture("dark", n, dark = true) { GameContent(playing(n), GameActions()) }

    private fun scoreboard(n: Names) {
        val eliminated =
            TeamUi(TeamId("D"), n.out, 3, 21, 3, active = false, out = true, eliminated = true, remaining = 29)
        compose.setContent {
            MolkkyBoardTheme(darkTheme = false, reducedMotion = true) {
                GameContent(
                    playing(n).copy(teams = teams(n) + eliminated),
                    GameActions(),
                    scoreboardOpen = true,
                    connection = threePhones,
                )
            }
        }
        compose.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/store/scoreboard_${n.lang}.png")
    }

    private fun end(n: Names) {
        val teams = teams(n)
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
                                SeriesRowUi(n.chloe, 2, 0, listOf(18, 40, 12), 70),
                            ),
                    ),
            )
        val finished =
            playing(
                n,
            ).copy(finished = true, winnerName = "Alice", ranking = ranked, teams = ranked, round = 7, end = end)
        capture("end", n) { GameContent(finished, GameActions()) }
    }

    @Test
    @Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
    fun gameFrancais() = game(fr)

    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun gameAnglais() = game(en)

    @Test
    @Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
    fun sunlightFrancais() = sunlight(fr)

    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun sunlightAnglais() = sunlight(en)

    @Test
    @Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
    fun darkFrancais() = dark(fr)

    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun darkAnglais() = dark(en)

    @Test
    @Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
    fun scoreboardFrancais() = scoreboard(fr)

    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun scoreboardAnglais() = scoreboard(en)

    @Test
    @Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
    fun endFrancais() = end(fr)

    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
    fun endAnglais() = end(en)
}
