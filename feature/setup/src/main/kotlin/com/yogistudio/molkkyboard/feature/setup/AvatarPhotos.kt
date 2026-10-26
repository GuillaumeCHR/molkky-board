package com.yogistudio.molkkyboard.feature.setup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** FileProvider propre aux photos de joueurs (une sous-classe évite les conflits avec d'autres FileProvider). */
class AvatarFileProvider : FileProvider()

/**
 * Photos de joueurs, facultatives et purement locales : la photo choisie (galerie ou appareil photo) est réduite
 * puis réencodée en JPEG dans le stockage privé de l'app, ce qui supprime toutes ses métadonnées (EXIF, lieu).
 */
@Singleton
class AvatarPhotos
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : PhotoStore {
        /** Fichier temporaire où l'appareil photo enregistre la prise de vue. */
        override fun captureUri(): Uri {
            val file = File(context.cacheDir, "camera/capture.jpg").apply { parentFile?.mkdirs() }
            return FileProvider.getUriForFile(context, "${context.packageName}.avatars", file)
        }

        /** Importe [source] comme photo de [playerId] ; renvoie l'URI du fichier privé, ou `null` en cas d'échec. */
        override suspend fun import(source: Uri, playerId: String): String? =
            withContext(Dispatchers.IO) {
                try {
                    val bitmap = decode(source) ?: return@withContext null
                    val dir = File(context.filesDir, "avatars").apply { mkdirs() }
                    // Un nom nouveau à chaque import : l'affichage (cache d'images) se met à jour.
                    dir.listFiles { f -> f.name.startsWith("$playerId-") }?.forEach { it.delete() }
                    val file = File(dir, "$playerId-${System.currentTimeMillis()}.jpg")
                    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
                    Uri.fromFile(file).toString()
                } catch (_: IOException) {
                    null
                } catch (_: SecurityException) {
                    null
                }
            }

        /** Supprime la photo d'un joueur. */
        override suspend fun delete(playerId: String) {
            withContext(Dispatchers.IO) {
                File(context.filesDir, "avatars")
                    .listFiles { f ->
                        f.name.startsWith("$playerId-")
                    }?.forEach { it.delete() }
            }
        }

        private fun decode(source: Uri): Bitmap? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // ImageDecoder applique l'orientation de la photo avant que l'EXIF ne disparaisse.
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, source),
                ) { decoder, info, _ ->
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

        private companion object {
            const val MAX_SIZE = 512
            const val JPEG_QUALITY = 85
        }
    }
