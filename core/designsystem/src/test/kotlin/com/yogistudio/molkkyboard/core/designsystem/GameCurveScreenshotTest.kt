package com.yogistudio.molkkyboard.core.designsystem

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.yogistudio.molkkyboard.core.designsystem.component.CurveDot
import com.yogistudio.molkkyboard.core.designsystem.component.CurveDotMark
import com.yogistudio.molkkyboard.core.designsystem.component.GameCurve
import com.yogistudio.molkkyboard.core.designsystem.component.GameCurveHighlight
import com.yogistudio.molkkyboard.core.designsystem.component.GameCurveLine
import com.yogistudio.molkkyboard.core.designsystem.component.GameCurveTimeline
import com.yogistudio.molkkyboard.core.designsystem.icon.LineEmblems
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.designsystem.theme.TeamPalette
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Courbe de la partie : image d'impact figée, vue finale en clair et en sombre, avec 2 et 6 participants et avec
 * beaucoup de tours (spécification « Courbe de la partie », Tests).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h400dp-xxhdpi")
class GameCurveScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val emblems = LineEmblems.all.values.toList()

    private fun line(i: Int, scores: List<Int>, marks: Map<Int, CurveDotMark> = emptyMap(), winner: Boolean = false) =
        GameCurveLine(
            color = TeamPalette.colors(dark = false)[i].fill,
            emblem = emblems[i % emblems.size],
            initial = "J$i",
            dots = scores.mapIndexed { round, score -> CurveDot(round, score, marks[round] ?: CurveDotMark.NONE) },
            winner = winner,
        )

    /** Léa gagne au tour 8 après une chute à 25 au tour 6 ; Tom est éliminé au tour 7. */
    private val duel =
        listOf(
            line(
                0,
                listOf(0, 12, 20, 31, 38, 45, 25, 37, 50),
                mapOf(6 to CurveDotMark.RESET, 8 to CurveDotMark.WIN),
                winner = true,
            ),
            line(1, listOf(0, 3, 10, 10, 22, 29, 29, 29), mapOf(7 to CurveDotMark.ELIMINATED)),
        )

    private val six =
        (0 until 6).map { i ->
            val scores = (0..7).map { round -> (round * (3 + i)).coerceAtMost(48 - i) }
            if (i == 2) {
                line(i, scores.dropLast(1) + 50, mapOf(7 to CurveDotMark.WIN), winner = true)
            } else {
                line(i, scores)
            }
        }

    private val long =
        listOf(
            line(0, (0..24).map { (it * 2).coerceAtMost(50) }, mapOf(24 to CurveDotMark.WIN), winner = true),
            line(1, (0..24).map { r -> if (r < 15) r * 3 else 25 + (r - 15) }, mapOf(15 to CurveDotMark.RESET)),
            line(2, (0..24).map { it }),
        )

    private fun capture(name: String, lines: List<GameCurveLine>, rounds: Int, time: Float, dark: Boolean = false) {
        compose.setContent {
            MolkkyBoardTheme(darkTheme = dark, reducedMotion = false) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    GameCurve(
                        lines = lines,
                        rounds = rounds,
                        target = 50,
                        timeMs = { time },
                        highlight = GameCurveHighlight(0, 6),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/courbe_$name.png")
    }

    @Test
    fun impactFige() = capture("impact", duel, 8, GameCurveTimeline.IMPACT + 20f)

    @Test
    fun traceEnCours() = capture("trace", duel, 8, GameCurveTimeline.DRAW_END * 0.6f)

    @Test
    fun explosion() = capture("explosion", duel, 8, GameCurveTimeline.FREEZE_END + 200f)

    @Test
    fun finaleClair() = capture("finale_clair", duel, 8, GameCurveTimeline.END)

    @Test
    fun finaleSombre() = capture("finale_sombre", duel, 8, GameCurveTimeline.END, dark = true)

    @Test
    fun sixParticipants() = capture("six", six, 7, GameCurveTimeline.END)

    @Test
    fun beaucoupDeTours() = capture("longue", long, 24, GameCurveTimeline.END)
}
