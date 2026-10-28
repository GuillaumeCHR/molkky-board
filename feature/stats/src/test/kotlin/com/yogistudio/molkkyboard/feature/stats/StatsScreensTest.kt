package com.yogistudio.molkkyboard.feature.stats

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.stats.FastestEntry
import com.yogistudio.molkkyboard.core.stats.HallTab
import com.yogistudio.molkkyboard.core.stats.PlayerStats
import com.yogistudio.molkkyboard.core.stats.SetAverage
import com.yogistudio.molkkyboard.core.stats.WinsEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
class StatsScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val paris = ZoneId.of("Europe/Paris")
    private val day =
        LocalDateTime
            .of(2026, 10, 27, 15, 0)
            .atZone(paris)
            .toInstant()
            .toEpochMilli()

    private val hall =
        HallUiState(
            loading = false,
            wins =
                listOf(
                    WinsEntry("a", "Alice", 1, 5, 8, day, "g1"),
                    WinsEntry("b", "Bruno", 1, 3, 8, day, "g2"),
                    WinsEntry("c", "Chloé-Amandine de la Roche-Bernard", 1, 1, 6, day, null),
                ),
            fastest =
                listOf(
                    FastestEntry("a", "Alice", 1, 5, "g1", 0, day),
                    FastestEntry("b", "Bruno", 1, 8, "g2", 0, day),
                ),
        )

    @Test
    fun `periodes dans le fuseau local`() {
        assertNull(Period.ALL.since(day, paris))
        assertEquals(
            LocalDateTime
                .of(2026, 1, 1, 0, 0)
                .atZone(paris)
                .toInstant()
                .toEpochMilli(),
            Period.YEAR.since(day, paris),
        )
        assertEquals(
            LocalDateTime
                .of(2026, 10, 1, 0, 0)
                .atZone(paris)
                .toInstant()
                .toEpochMilli(),
            Period.MONTH.since(day, paris),
        )
    }

    @Test
    fun `hall of fame joueurs`() {
        compose.setContent { MolkkyBoardTheme(reducedMotion = true) { HallOfFameContent(hall) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/hall_of_fame_joueurs.png")
        compose.onRoot().assertAccessible()
    }

    @Test
    fun `hall of fame equipes en sombre a la police 2`() {
        val teams =
            hall.copy(
                filters = HallFilters(tab = HallTab.TEAMS),
                teamSizes = listOf(2, 3),
                wins = listOf(WinsEntry("t", "Les Bouleaux", 2, 4, 5, day, "g1")),
                fastest = listOf(FastestEntry("t", "Les Bouleaux", 2, 7, "g1", 0, day)),
            )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MolkkyBoardTheme(darkTheme = true, reducedMotion = true) { HallOfFameContent(teams) }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/hall_of_fame_equipes_police_200.png")
        compose.onRoot().assertAccessible()
    }

    @Test
    fun `hall of fame vide`() {
        compose.setContent {
            MolkkyBoardTheme(
                reducedMotion = true,
            ) { HallOfFameContent(HallUiState(loading = false)) }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/hall_of_fame_vide.png")
        compose.onRoot().assertAccessible()
    }

    @Test
    fun `statistiques d'un joueur avec graphique`() {
        val alice = PlayerId("a")
        val averages =
            listOf(4.2f, 5.1f, 3.8f, 6.0f, 7.1f, 5.5f).mapIndexed {
                i,
                avg,
                ->
                SetAverage("g$i", 0, day + i, avg)
            }
        val state =
            StatsUiState(
                loading = false,
                players = listOf(StatsPlayer(alice, "Alice", "🦊"), StatsPlayer(PlayerId("b"), "Bruno", "🐻")),
                selected = alice,
                stats = PlayerStats(alice, 6, 3, 42, 5, 228, averages, 9, 7),
            )
        compose.setContent { MolkkyBoardTheme(reducedMotion = true) { StatsContent(state) } }
        compose.onRoot().captureRoboImage("src/test/screenshots/statistiques.png")
        compose.onRoot().assertAccessible()
    }
}

/** Contrôles d'accessibilité automatiques (ATF via Roborazzi) : échec sur toute erreur (cible tactile, libellé…). */
@OptIn(ExperimentalRoborazziApi::class)
private fun SemanticsNodeInteraction.assertAccessible() =
    checkRoboAccessibility(
        roborazziATFAccessibilityCheckOptions =
            RoborazziATFAccessibilityCheckOptions(
                checker = RoborazziATFAccessibilityChecker(preset = AccessibilityCheckPreset.LATEST),
                failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Error,
            ),
    )
