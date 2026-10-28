package com.yogistudio.molkkyboard.feature.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Photo du moment (N1) : prise par l'appareil photo du système ou choisie dans le sélecteur de photos, donc sans
 * permission. Elle est décodée en mémoire, réduite, et le fichier de prise de vue est effacé aussitôt : l'app ne
 * conserve aucune photo. L'image partagée est un PNG régénéré, sans métadonnées (ni EXIF ni lieu).
 */
internal object MomentPhoto {
    private const val CAPTURE = "moment-capture.jpg"
    private const val MAX_SIZE = 2_160

    /** Fichier où l'appareil photo enregistre la prise de vue, dans le seul dossier exposé par le FileProvider. */
    fun captureUri(context: Context): Uri {
        val file = File(ShareFiles.directory(context), CAPTURE)
        return FileProvider.getUriForFile(context, "${context.packageName}.share", file)
    }

    /** Décode [source] (orientation appliquée sous Android 9 et plus), puis efface la prise de vue. */
    suspend fun load(context: Context, source: Uri): ImageBitmap? =
        withContext(Dispatchers.IO) {
            try {
                decode(context, source)?.asImageBitmap()
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            } finally {
                File(ShareFiles.directory(context), CAPTURE).delete()
            }
        }

    private fun decode(context: Context, source: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, source)) { decoder, info, _ ->
                // Bitmap logiciel : il doit pouvoir être redessiné dans la vignette capturée.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val largest = maxOf(info.size.width, info.size.height)
                if (largest > MAX_SIZE) {
                    val ratio = MAX_SIZE.toFloat() / largest
                    decoder.setTargetSize((info.size.width * ratio).toInt(), (info.size.height * ratio).toInt())
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIZE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
}
