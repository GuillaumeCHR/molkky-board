package com.yogistudio.molkkyboard.feature.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

/**
 * Fichiers générés pour le partage : uniquement dans `cache/share` (seul dossier exposé par le FileProvider),
 * nettoyés à chaque nouveau partage. Les images PNG sont produites de zéro : aucune métadonnée EXIF ni
 * localisation n'y figure.
 */
object ShareFiles {
    private const val DIR = "share"
    private const val MAX_AGE_MS = 60 * 60 * 1000L
    private const val PNG_QUALITY = 100

    fun directory(context: Context): File = File(context.cacheDir, DIR).apply { mkdirs() }

    /** Supprime les fichiers de partage de plus d'une heure (l'appli cible peut encore lire les récents). */
    fun clean(context: Context, now: Long = System.currentTimeMillis()) {
        directory(context).listFiles()?.filter { now - it.lastModified() > MAX_AGE_MS }?.forEach { it.delete() }
    }

    fun writePng(context: Context, bitmap: Bitmap, name: String): File {
        clean(context)
        val file = File(directory(context), "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
        return file
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.share",
            file,
        )

    /** Feuille de partage Android, avec le texte d'accompagnement ; [target] force une application (WhatsApp). */
    fun shareIntent(
        uri: Uri,
        mime: String,
        text: String,
        title: String,
        target: String? = null,
    ): Intent {
        val send =
            Intent(Intent.ACTION_SEND)
                .setType(mime)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_TEXT, text)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return if (target != null) send.setPackage(target) else Intent.createChooser(send, title)
    }

    fun isInstalled(context: Context, packageName: String): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    /** La galerie sans permission n'existe qu'à partir d'Android 10 (MediaStore). */
    val canSaveToGallery: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** Enregistre l'image dans Images/Molkky Board (Android 10+, aucune permission demandée). */
    fun saveToGallery(context: Context, bitmap: Bitmap, name: String): Boolean {
        if (!canSaveToGallery) return false
        val resolver = context.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Molkky Board")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (_: IOException) {
            resolver.delete(uri, null, null)
            false
        }
    }

    const val WHATSAPP = "com.whatsapp"
}
