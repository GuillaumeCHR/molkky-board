package com.yogistudio.molkkyboard.crash

import android.content.Context
import android.os.Build
import com.yogistudio.molkkyboard.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Contexte technique d'un rapport : rien d'autre que la version de l'app, celle d'Android et le modèle. */
data class CrashContext(
    val appVersion: String,
    val androidVersion: String,
    val device: String,
    val at: Instant,
)

/**
 * Rapport de plantage lisible, sans donnée personnelle : seulement les types d'exception et la pile d'appels. Les
 * messages des exceptions sont **omis** (ils pourraient contenir un nom de joueur ou d'équipe), comme toute donnée de
 * partie ou tout identifiant.
 */
fun formatCrashReport(throwable: Throwable, context: CrashContext): String =
    buildString {
        appendLine("Molkky Board ${context.appVersion}")
        appendLine("Android ${context.androidVersion} · ${context.device}")
        appendLine(context.at.truncatedTo(ChronoUnit.MINUTES).toString())
        appendLine()
        var cause: Throwable? = throwable
        var depth = 0
        val seen = mutableSetOf<Throwable>()
        while (cause != null && depth < MAX_CAUSES && seen.add(cause)) {
            appendLine(if (depth == 0) cause.javaClass.name else "Caused by: ${cause.javaClass.name}")
            cause.stackTrace.take(MAX_FRAMES).forEach { appendLine("    at $it") }
            cause = cause.cause
            depth++
        }
    }.trimEnd()

private const val MAX_CAUSES = 5
private const val MAX_FRAMES = 25

/**
 * Rapports de plantage **à la demande** : le dernier plantage est écrit sur le téléphone, et au lancement suivant
 * l'app propose de l'envoyer par e-mail, avec un aperçu complet. Rien n'est jamais envoyé automatiquement.
 */
@Singleton
class CrashReports
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private val file get() = File(context.filesDir, "crash/last.txt")

        /** Enregistre le rapport puis laisse le gestionnaire d'origine terminer l'app (comportement système). */
        fun install() {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                runCatching { save(throwable) }
                previous?.uncaughtException(thread, throwable)
            }
        }

        private fun save(throwable: Throwable) {
            val report =
                formatCrashReport(
                    throwable,
                    CrashContext(
                        appVersion = BuildConfig.VERSION_NAME,
                        androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                        device = "${Build.MANUFACTURER} ${Build.MODEL}",
                        at = Instant.now(),
                    ),
                )
            file.parentFile?.mkdirs()
            file.writeText(report)
        }

        /** Rapport en attente d'une décision de l'utilisateur, ou `null`. */
        fun pending(): String? = file.takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }

        fun clear() {
            file.delete()
        }
    }
