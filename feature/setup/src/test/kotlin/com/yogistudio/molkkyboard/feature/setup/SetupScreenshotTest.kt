package com.yogistudio.molkkyboard.feature.setup

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.yogistudio.molkkyboard.core.data.KnownTeam
import com.yogistudio.molkkyboard.core.data.PlayerProfile
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captures de la création de partie, en solo et en équipes. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h1400dp-xxhdpi")
class SetupScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val players =
        listOf("Alice", "Bruno", "Chloé", "Denis").mapIndexed { i, name ->
            SetupPlayer(PlayerId(name), name, i, PLAYER_AVATARS[i])
        }

    private fun capture(name: String, state: SetupUiState) {
        compose.setContent { MolkkyBoardTheme(reducedMotion = true) { SetupContent(state, SetupActions()) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun solo() =
        capture(
            "creation_solo",
            SetupUiState(players = players, knownProfiles = listOf(PlayerProfile("e", "Emma", 4, "🦉"))),
        )

    @Test
    fun equipes() =
        capture(
            "creation_equipes",
            SetupUiState(
                playMode = PlayMode.TEAMS,
                players = players,
                teams =
                    listOf(
                        SetupTeam(
                            TeamId("t1"),
                            "Les Bouleaux",
                            0,
                            "leaf",
                            listOf(PlayerId("Alice"), PlayerId("Chloé")),
                            known = KnownTeam("k", "Les Pives", 5, "bolt"),
                        ),
                        SetupTeam(TeamId("t2"), "Les Élans", 1, "bolt", listOf(PlayerId("Bruno"), PlayerId("Denis"))),
                    ),
            ),
        )
}
