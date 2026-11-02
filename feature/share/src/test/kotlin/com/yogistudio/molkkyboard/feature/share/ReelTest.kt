package com.yogistudio.molkkyboard.feature.share

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import com.yogistudio.molkkyboard.core.designsystem.R as DesignR

/** Images clés du mini-clip (une par séquence) et GIF animé valide. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReelTest {
    private val labels = ReelLabels({ "Tour $it" }, "Retour à 25 !", "Éliminé", "Comptez vos parties avec", "#molkky")
    private val data =
        ReelData(
            teams =
                listOf(
                    ReelTeam("Les Bouleaux", 0xFF3949AB.toInt()),
                    ReelTeam("Alice", 0xFF00796B.toInt()),
                    ReelTeam("Bruno", 0xFFA35200.toInt()),
                ),
            steps =
                listOf(
                    ReelStep(0, 12, 1, StepKind.POINTS),
                    ReelStep(1, 7, 1, StepKind.POINTS),
                    ReelStep(2, 0, 1, StepKind.MISS),
                    ReelStep(0, 24, 2, StepKind.POINTS),
                    ReelStep(1, 19, 2, StepKind.POINTS),
                    ReelStep(2, 0, 2, StepKind.MISS),
                    ReelStep(0, 36, 3, StepKind.POINTS),
                    ReelStep(1, 31, 3, StepKind.POINTS),
                    ReelStep(2, 0, 3, StepKind.ELIMINATED),
                    ReelStep(0, 25, 4, StepKind.RESET),
                    ReelStep(1, 43, 4, StepKind.POINTS),
                    ReelStep(0, 37, 5, StepKind.POINTS),
                    ReelStep(1, 50, 5, StepKind.WIN),
                ),
            winner = 1,
            target = 50,
            title = "Victoire !",
            keyFigure = "50 en 5 lancers · tour 5",
            labels = labels,
        )

    private fun renderer(width: Int = 540, height: Int = 960) =
        ReelRenderer(data, width, height, Typeface.DEFAULT_BOLD, Typeface.DEFAULT)

    @Test
    fun imagesCles() {
        val renderer = renderer()
        val bitmap = Bitmap.createBitmap(renderer.width, renderer.height, Bitmap.Config.ARGB_8888)
        val frames = listOf("1_strike" to 700L, "2_course" to 4_300L, "3_victoire" to 6_600L, "4_carton" to 8_000L)
        frames.forEach { (name, t) ->
            renderer.render(bitmap, t)
            bitmap.captureRoboImage("src/test/screenshots/clip_$name.png")
        }
        assertEquals(8_800L, renderer.durationMs)
    }

    @Test
    fun `GIF anime valide - en-tete, images, fin, lisible`() {
        val renderer = renderer(180, 320)
        val bitmap = Bitmap.createBitmap(180, 320, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream()
        val gif = GifEncoder(bytes, 180, 320, delayCs = 10)
        val frames = 12
        repeat(frames) {
            renderer.render(bitmap, it * 700L)
            gif.addFrame(bitmap)
        }
        gif.finish()
        val data = bytes.toByteArray()
        assertEquals("GIF89a", String(data, 0, 6, Charsets.US_ASCII))
        assertEquals(0x3B, data.last().toInt())
        assertTrue("taille raisonnable : ${data.size} octets", data.size in 1_000..2_000_000)
        assertNotNull(BitmapFactory.decodeByteArray(data, 0, data.size))
    }

    /**
     * Aucun texte ne déborde de l'image, ni dans la vidéo (1 080 × 1 920) ni dans le GIF (360 × 640) : titres les plus
     * longs en français et en anglais, noms d'équipe à la longueur maximale, vraies polices de l'app.
     */
    @Test
    fun `aucun texte ne deborde, video et GIF`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val display = ResourcesCompat.getFont(context, DesignR.font.bricolage_grotesque_extrabold)!!
        val body = ResourcesCompat.getFont(context, DesignR.font.inter_semibold)!!
        val long = "Les Grands Bouleaux de".take(22)
        val worst =
            data.copy(
                teams = data.teams.mapIndexed { i, team -> team.copy(name = if (i == 1) long else "$long $i") },
                title = "Quelle remontée !",
                keyFigure = "50 en 5 lancers : impossible de faire mieux",
            )
        val texts =
            listOf(
                "Quelle remontée !",
                "What a comeback!",
                "Partie parfaite !",
                "Nouveau record !",
                "50 en 5 lancers : impossible de faire mieux",
                "50 in 5 throws: it can't be done better",
                "Mené de 15 points… et vainqueur",
                "15 points behind… and the winner",
                "Comptez vos parties avec",
                "Retour à 25 !",
                "Éliminé",
                long,
                "1. $long",
            )
        listOf(1_080 to 1_920, 360 to 640).forEach { (w, h) ->
            val renderer = ReelRenderer(worst, w, h, display, body)
            val margin = 80f * w / 1_080f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = display }
            texts.forEach { text ->
                with(renderer) { paint.fit(text, 150f * w / 1_080f) }
                assertTrue("« $text » dépasse en $w px", paint.measureText(text) <= w - 2 * margin + 0.5f)
            }
            // Course et carton final : fond uni, rien ne doit toucher les marges latérales.
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            listOf(4_300L, 8_000L).forEach { t ->
                renderer.render(bitmap, t)
                assertMarginsClear(bitmap, (margin / 2).toInt(), "$t ms en $w px")
            }
            if (w == 360) {
                listOf("1_strike" to 700L, "3_victoire" to 6_600L, "4_carton" to 8_000L).forEach { (name, t) ->
                    renderer.render(bitmap, t)
                    bitmap.captureRoboImage("src/test/screenshots/gif_long_$name.png")
                }
            }
        }
    }

    /** Les bandes latérales de largeur [strip] ont toutes la couleur du fond (aucun texte n'y passe). */
    private fun assertMarginsClear(bitmap: Bitmap, strip: Int, where: String) {
        val background = bitmap.getPixel(1, bitmap.height / 2)
        val columns = (0 until strip) + (bitmap.width - strip until bitmap.width)
        val stray =
            (0 until bitmap.height).firstNotNullOfOrNull { y ->
                columns.firstOrNull { x -> bitmap.getPixel(x, y) != background }?.let { x -> x to y }
            }
        assertEquals("pixel hors marge à $where", null, stray)
    }
}
