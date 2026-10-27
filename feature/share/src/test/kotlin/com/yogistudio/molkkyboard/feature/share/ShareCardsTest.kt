package com.yogistudio.molkkyboard.feature.share

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.stats.Moment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId

/** Chaque modèle dans les trois formats, avec des noms longs, à 2 et à 6 équipes, et noms masqués. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
class ShareCardsTest {
    @get:Rule
    val compose = createComposeRule()

    private val day =
        LocalDateTime
            .of(2026, 10, 27, 15, 0)
            .atZone(ZoneId.of("Europe/Paris"))
            .toInstant()
            .toEpochMilli()
    private val names =
        listOf("Les Bouleaux", "Alice", "Les Quilles Folles de Kerlouan-Plouguerneau", "Bruno", "Chloé", "Denis")
    private val avatars = listOf("🌲", "🦊", "⚡", "🐻", "🦉", null)
    private val scores = listOf(50, 43, 38, 27, 12, 0)

    private fun teams(count: Int) =
        (0 until count).map { i -> ShareTeam(TeamId("t$i"), names[i], i, avatars[i], scores[i], out = i == 5) }

    private fun content(moment: Moment, count: Int = 3) =
        teams(count).let { list ->
            ShareContent(
                moment,
                list,
                list.first(),
                target = 50,
                throws = 7,
                round = 6,
                playedAt = day,
                place = "Plage du Moulin",
            )
        }

    private val victory = Moment.Victory(TeamId("t0"), 7, 6)

    private data class Shot(
        val name: String,
        val content: ShareContent,
        val template: ShareTemplate,
        val format: ShareFormat,
    )

    /** Une seule composition par test : on change la vignette affichée, puis on la capture. */
    private fun captureAll(shots: List<Shot>) {
        var current by mutableStateOf(shots.first())
        compose.setContent { ShareCard(current.content, current.template, current.format) }
        shots.forEach { shot ->
            current = shot
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("src/test/screenshots/${shot.name}.png")
        }
    }

    private fun capture(name: String, content: ShareContent, template: ShareTemplate, format: ShareFormat) =
        captureAll(listOf(Shot(name, content, template, format)))

    private fun inThreeFormats(prefix: String, content: ShareContent, template: ShareTemplate) =
        captureAll(ShareFormat.entries.map { Shot("${prefix}_${it.name.lowercase()}", content, template, it) })

    @Test
    fun podiumTroisFormats() {
        inThreeFormats("podium", content(victory), ShareTemplate.PODIUM)
    }

    @Test
    fun tableauTroisFormatsSixEquipes() {
        inThreeFormats("tableau_6", content(victory, 6), ShareTemplate.SCOREBOARD)
    }

    @Test
    fun tableauDeuxEquipes() =
        capture(
            "tableau_2_story",
            content(victory, 2),
            ShareTemplate.SCOREBOARD,
            ShareFormat.STORY,
        )

    @Test
    fun recordTroisFormats() {
        inThreeFormats("record", content(Moment.Record(TeamId("t0"), 5)), ShareTemplate.HIGHLIGHT)
    }

    @Test
    fun strike() =
        capture(
            "strike_story",
            content(Moment.Twelve(TeamId("t0"), PlayerId("p"), strike = true)),
            ShareTemplate.HIGHLIGHT,
            ShareFormat.STORY,
        )

    @Test
    fun aieTroisFormats() {
        inThreeFormats("aie", content(Moment.ResetFrom49(TeamId("t0"), PlayerId("p"))), ShareTemplate.OUCH)
    }

    @Test
    fun nomsMasques() {
        val masked =
            content(victory).let { c ->
                c.copy(teams = c.teams.mapIndexed { i, t -> t.copy(name = "Joueur ${i + 1}") })
            }
        capture(
            "podium_noms_masques",
            masked.copy(hero = masked.teams.first()),
            ShareTemplate.PODIUM,
            ShareFormat.STORY,
        )
    }

    @Test
    fun `l'image capturee fait exactement 1080 x 1920`() {
        var captured: GraphicsLayer? = null
        compose.setContent {
            val layer = rememberGraphicsLayer().also { captured = it }
            Box {
                ShareCard(
                    content(victory),
                    ShareTemplate.PODIUM,
                    ShareFormat.STORY,
                    Modifier.drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    },
                )
            }
        }
        // Sous Robolectric, rien n'est dessiné tant qu'on ne capture pas l'écran : on force un rendu (dans l'app,
        // l'aperçu est déjà affiché quand on touche « Partager »).
        compose.onRoot().captureToImage()
        val image = compose.runOnIdle { runBlocking { captured!!.toImageBitmap() } }
        assertEquals(1080, image.width)
        assertEquals(1920, image.height)
    }
}
