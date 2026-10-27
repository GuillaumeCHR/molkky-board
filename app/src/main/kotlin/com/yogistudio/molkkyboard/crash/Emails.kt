package com.yogistudio.molkkyboard.crash

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.yogistudio.molkkyboard.BuildConfig

/**
 * Ouvre l'app d'e-mail du téléphone avec un message prérempli : c'est l'utilisateur qui relit, choisit d'envoyer ou
 * non, et l'envoi passe par sa messagerie (l'app elle-même n'a pas accès à Internet). Renvoie `false` si aucune app
 * d'e-mail n'est installée.
 */
fun composeEmail(context: Context, subject: String, body: String): Boolean {
    val intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL).filter { it.isNotBlank() }.toTypedArray())
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

/** Version et modèle, et rien d'autre : le contexte joint à un retour de testeur. */
fun deviceSummary(): String =
    "Molkky Board ${BuildConfig.VERSION_NAME} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · " +
        "${Build.MANUFACTURER} ${Build.MODEL}"

/**
 * Adresse de contact publique dédiée (à créer, voir « Publication sur le Google Play Store ») : tant qu'elle est vide,
 * l'utilisateur saisit le destinataire lui-même.
 */
const val SUPPORT_EMAIL = ""
