package com.yogistudio.molkkyboard.feature.share

import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Encodage réel du mini-clip (émulateur ou téléphone) : durée, résolution, poids et temps de génération. */
@RunWith(AndroidJUnit4::class)
class ReelEncodingTest {
    @Test
    fun clipMp4DeNeufSecondesEn1080x1920DeMoinsDe8Mo() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val data =
            ReelData(
                teams =
                    listOf(
                        ReelTeam("Alice", 0xFF3949AB.toInt()),
                        ReelTeam("Bruno", 0xFF00796B.toInt()),
                    ),
                steps =
                    (1..9).map {
                        ReelStep(
                            it % 2,
                            minOf(50, it * 6),
                            (it + 1) / 2,
                            if (it ==
                                9
                            ) {
                                StepKind.WIN
                            } else {
                                StepKind.POINTS
                            },
                        )
                    },
                winner = 1,
                target = 50,
                title = "Victoire !",
                keyFigure = "50 en 5 lancers",
                labels = ReelLabels({ "Tour $it" }, "Retour à 25 !", "Éliminé", "Comptez vos parties avec", "#molkky"),
            )
        val renderer = ReelRenderer(data, 1_080, 1_920, Typeface.DEFAULT_BOLD, Typeface.DEFAULT)
        val started = System.currentTimeMillis()
        val file = runBlocking { ReelEncoder(context).encode(renderer) {} }
        val elapsed = System.currentTimeMillis() - started
        val retriever = MediaMetadataRetriever().apply { setDataSource(file.absolutePath) }
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
        val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        // Media3 peut encoder le portrait en paysage et l'indiquer par une rotation (l'affichage reste vertical).
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
        retriever.release()
        val (shownWidth, shownHeight) = if (rotation % 180 == 0) width to height else height to width
        println(
            "REEL duration=$duration size=${file.length()} encoding=${elapsed}ms ${width}x$height rot=$rotation $mime",
        )
        assertTrue("durée $duration ms", duration in 8_000..9_500)
        assertEquals(1_080, shownWidth)
        assertEquals(1_920, shownHeight)
        assertEquals("video/mp4", mime)
        assertTrue("poids ${file.length()} octets", file.length() < 8L * 1024 * 1024)
        file.delete()
    }
}
