package com.yogistudio.molkkyboard.feature.stats

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.stats.FastestEntry
import com.yogistudio.molkkyboard.core.stats.WinsEntry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId

/** Hall of Fame en anglais pour la fiche du Play Store. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-w411dp-h891dp-xxhdpi")
class StoreScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val day =
        LocalDateTime
            .of(2026, 10, 27, 15, 0)
            .atZone(ZoneId.of("Europe/Paris"))
            .toInstant()
            .toEpochMilli()

    @Test
    fun hallOfFame() {
        val hall =
            HallUiState(
                loading = false,
                wins =
                    listOf(
                        WinsEntry("a", "Alice", 1, 5, 8, day, "g1"),
                        WinsEntry("b", "Bruno", 1, 3, 8, day, "g2"),
                        WinsEntry("c", "Chloe", 1, 1, 6, day, null),
                    ),
                fastest =
                    listOf(
                        FastestEntry("a", "Alice", 1, 5, "g1", 0, day),
                        FastestEntry("b", "Bruno", 1, 8, "g2", 0, day),
                    ),
            )
        compose.setContent { MolkkyBoardTheme(darkTheme = false, reducedMotion = true) { HallOfFameContent(hall) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/store/hall_of_fame.png")
    }
}
