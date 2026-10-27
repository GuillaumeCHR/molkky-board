package com.yogistudio.molkkyboard.feature.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext
import com.yogistudio.molkkyboard.core.designsystem.R as DesignR

/** Mini-vidéo MP4 ou GIF : génération sur le téléphone avec progression et annulation, puis partage du fichier. */
@Composable
internal fun ReelSection(data: ReelData, text: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    val title = stringResource(R.string.share_chooser)
    DisposableEffect(Unit) { onDispose { job?.cancel() } }

    fun start(gif: Boolean) {
        failed = false
        progress = 0
        job =
            scope.launch {
                try {
                    val file =
                        if (gif) {
                            encodeGif(context, data) { progress = it }
                        } else {
                            encodeVideo(context, data) {
                                progress =
                                    it
                            }
                        }
                    val mime = if (gif) "image/gif" else "video/mp4"
                    context.startActivity(ShareFiles.shareIntent(ShareFiles.uriFor(context, file), mime, text, title))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    failed = true
                } finally {
                    job = null
                }
            }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (job != null) {
            Text(stringResource(R.string.reel_progress, progress), style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(progress = { progress / FULL }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = {
                job?.cancel()
            }, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.reel_cancel)) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { start(gif = false) }) { Text(stringResource(R.string.reel_video)) }
                TextButton(onClick = { start(gif = true) }) { Text(stringResource(R.string.reel_gif)) }
            }
            if (failed) Text(stringResource(R.string.reel_failed), color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun typefaces(context: Context): Pair<Typeface, Typeface> {
    val display = ResourcesCompat.getFont(context, DesignR.font.bricolage_grotesque_extrabold) ?: Typeface.DEFAULT_BOLD
    val body = ResourcesCompat.getFont(context, DesignR.font.inter_semibold) ?: Typeface.DEFAULT
    return display to body
}

/** MP4 1 080 × 1 920 à 30 i/s. */
internal suspend fun encodeVideo(context: Context, data: ReelData, onProgress: (Int) -> Unit): File {
    val (display, body) = withContext(Dispatchers.IO) { typefaces(context) }
    val renderer = ReelRenderer(data, VIDEO_WIDTH, VIDEO_HEIGHT, display, body)
    return ReelEncoder(context).encode(renderer, onProgress)
}

/** GIF 360 × 640 à 10 i/s, bien plus léger que la vidéo. */
internal suspend fun encodeGif(context: Context, data: ReelData, onProgress: (Int) -> Unit): File =
    withContext(Dispatchers.Default) {
        val (display, body) = typefaces(context)
        val renderer = ReelRenderer(data, GIF_WIDTH, GIF_HEIGHT, display, body)
        ShareFiles.clean(context)
        val file = File(ShareFiles.directory(context), "molkky-board-${System.currentTimeMillis()}.gif")
        val bitmap = Bitmap.createBitmap(GIF_WIDTH, GIF_HEIGHT, Bitmap.Config.ARGB_8888)
        val frames = (renderer.durationMs / GIF_FRAME_MS).toInt()
        try {
            file.outputStream().buffered().use { out ->
                val gif = GifEncoder(out, GIF_WIDTH, GIF_HEIGHT, delayCs = (GIF_FRAME_MS / 10).toInt())
                for (f in 0 until frames) {
                    coroutineContext.ensureActive()
                    renderer.render(bitmap, f * GIF_FRAME_MS)
                    gif.addFrame(bitmap)
                    withContext(Dispatchers.Main) { onProgress((f + 1) * PERCENT / frames) }
                }
                gif.finish()
            }
        } catch (e: CancellationException) {
            file.delete()
            throw e
        } finally {
            bitmap.recycle()
        }
        file
    }

private const val FULL = 100f
private const val PERCENT = 100
private const val VIDEO_WIDTH = 1_080
private const val VIDEO_HEIGHT = 1_920
private const val GIF_WIDTH = 360
private const val GIF_HEIGHT = 640
private const val GIF_FRAME_MS = 100L
