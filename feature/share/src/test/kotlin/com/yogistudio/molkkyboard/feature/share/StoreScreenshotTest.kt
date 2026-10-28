package com.yogistudio.molkkyboard.feature.share

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.stats.Moment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId

/** Vignette de partage en anglais pour la fiche du Play Store. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
class StoreScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun podium() {
        val day =
            LocalDateTime
                .of(2026, 10, 27, 15, 0)
                .atZone(ZoneId.of("Europe/Paris"))
                .toInstant()
                .toEpochMilli()
        val teams =
            listOf(
                ShareTeam(TeamId("t0"), "The Birches", 0, "🌲", 50, out = false),
                ShareTeam(TeamId("t1"), "Alice", 1, "🦊", 43, out = false),
                ShareTeam(TeamId("t2"), "Pin Pirates", 2, "⚡", 38, out = false),
            )
        val content =
            ShareContent(
                Moment.Victory(TeamId("t0"), 7, 6),
                teams,
                teams.first(),
                target = 50,
                throws = 7,
                round = 6,
                playedAt = day,
                place = "Mill Beach",
            )
        compose.setContent { ShareCard(content, ShareTemplate.PODIUM, ShareFormat.STORY) }
        compose.onRoot().captureRoboImage("src/test/screenshots/store/share_card.png")
    }
}
