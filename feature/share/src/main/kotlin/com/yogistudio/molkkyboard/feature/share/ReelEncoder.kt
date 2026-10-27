package com.yogistudio.molkkyboard.feature.share

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Mini-clip MP4 H.264 9:16, encodé sur le téléphone (Media3 Transformer, aucun serveur) : une image fixe sert de
 * support, et chaque image du clip est dessinée par [ReelRenderer] dans un calque ([BitmapOverlay]). Vidéo muette
 * (aucune musique sous droits : l'utilisateur ajoute la sienne dans son appli).
 */
@OptIn(UnstableApi::class)
class ReelEncoder(
    private val context: Context,
) {
    /**
     * Encode le clip dans `cache/share` et renvoie le fichier. [onProgress] reçoit 0 à 100. L'annulation de la
     * coroutine annule l'encodage et supprime le fichier partiel.
     */
    suspend fun encode(renderer: ReelRenderer, onProgress: (Int) -> Unit): File {
        ShareFiles.clean(context)
        val dir = ShareFiles.directory(context)
        val background = File(dir, "reel-background.png")
        Bitmap.createBitmap(renderer.width, renderer.height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(BACKGROUND)
            background.outputStream().use { compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
            recycle()
        }
        val output = File(dir, "molkky-board-${System.currentTimeMillis()}.mp4")
        val item =
            EditedMediaItem
                .Builder(
                    MediaItem
                        .Builder()
                        .setUri(Uri.fromFile(background))
                        .setImageDurationMs(renderer.durationMs)
                        .build(),
                ).setFrameRate(FPS)
                .setEffects(
                    Effects(
                        ImmutableList.of(),
                        listOf<Effect>(OverlayEffect(listOf<TextureOverlay>(ReelOverlay(renderer)))),
                    ),
                ).build()
        return try {
            run(item, output, onProgress)
            output
        } finally {
            background.delete()
        }
    }

    private suspend fun run(item: EditedMediaItem, output: File, onProgress: (Int) -> Unit) =
        suspendCancellableCoroutine { continuation ->
            val main = Handler(Looper.getMainLooper())
            lateinit var transformer: Transformer
            val progress = ProgressHolder()
            val poll =
                object : Runnable {
                    override fun run() {
                        if (transformer.getProgress(progress) ==
                            Transformer.PROGRESS_STATE_AVAILABLE
                        ) {
                            onProgress(progress.progress)
                        }
                        main.postDelayed(this, PROGRESS_MS)
                    }
                }
            main.post {
                transformer =
                    Transformer
                        .Builder(context)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setEncoderFactory(
                            DefaultEncoderFactory
                                .Builder(context)
                                .setRequestedVideoEncoderSettings(
                                    VideoEncoderSettings.Builder().setBitrate(BITRATE).build(),
                                ).build(),
                        ).addListener(
                            object : Transformer.Listener {
                                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                    main.removeCallbacks(poll)
                                    onProgress(DONE)
                                    if (continuation.isActive) continuation.resume(Unit)
                                }

                                override fun onError(
                                    composition: Composition,
                                    exportResult: ExportResult,
                                    exportException: ExportException,
                                ) {
                                    main.removeCallbacks(poll)
                                    output.delete()
                                    if (continuation.isActive) continuation.resumeWithException(exportException)
                                }
                            },
                        ).build()
                transformer.start(item, output.absolutePath)
                main.post(poll)
            }
            continuation.invokeOnCancellation {
                main.post {
                    main.removeCallbacks(poll)
                    transformer.cancel()
                    output.delete()
                }
            }
        }

    companion object {
        const val FPS = 30

        /** ≈ 4 Mbit/s : un clip de 9 s pèse ≈ 4,5 Mo (moins de 8 Mo pour 10 s). */
        const val BITRATE = 4_000_000
        private const val PROGRESS_MS = 200L
        private const val DONE = 100
        private const val PNG_QUALITY = 100
        private const val BACKGROUND = 0xFFF4EBDD.toInt()
    }
}

/**
 * Calque redessiné à chaque image. Deux bitmaps alternent : Media3 ne renvoie la texture au GPU que si le bitmap
 * change, et on évite ainsi d'en allouer un par image.
 */
@OptIn(UnstableApi::class)
private class ReelOverlay(
    private val renderer: ReelRenderer,
) : BitmapOverlay() {
    private val buffers =
        Array(2) { Bitmap.createBitmap(renderer.width, renderer.height, Bitmap.Config.ARGB_8888) }
    private var frame = 0

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val bitmap = buffers[frame++ % buffers.size]
        renderer.render(bitmap, presentationTimeUs / MICROS_PER_MS)
        return bitmap
    }

    companion object {
        private const val MICROS_PER_MS = 1_000L
    }
}
