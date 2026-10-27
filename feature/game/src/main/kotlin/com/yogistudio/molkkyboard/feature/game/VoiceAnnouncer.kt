package com.yogistudio.molkkyboard.feature.game

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Annonces vocales par la synthèse vocale d'Android (moteur installé sur le téléphone, aucune requête de
 * l'application). Muettes si le téléphone est en silencieux ou en vibreur, et pendant TalkBack (doublon).
 */
class VoiceAnnouncer(
    context: Context,
) {
    private val appContext = context.applicationContext

    // Langue de l'écran (celle choisie dans l'app), pas celle du contexte d'application.
    private val locale = context.resources.configuration.locales[0]
    private val audio = appContext.getSystemService(AudioManager::class.java)
    private val accessibility = appContext.getSystemService(AccessibilityManager::class.java)
    private var ready = false
    private var counter = 0
    private val tts =
        TextToSpeech(appContext) { status ->
            ready = status == TextToSpeech.SUCCESS
        }

    fun speak(text: String, rate: Float, volume: Float) {
        if (!ready || muted()) return
        tts.language = locale
        tts.setSpeechRate(rate)
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume) }
        tts.speak(text, TextToSpeech.QUEUE_ADD, params, "mb-${counter++}")
    }

    private fun muted(): Boolean =
        audio?.ringerMode != AudioManager.RINGER_MODE_NORMAL || accessibility?.isTouchExplorationEnabled == true

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

/** Annonceur lié à l'écran ; créé seulement si les annonces sont activées (aucun moteur sollicité sinon). */
@Composable
internal fun rememberVoiceAnnouncer(enabled: Boolean): VoiceAnnouncer? {
    val context = LocalContext.current
    val announcer = remember(enabled) { if (enabled) VoiceAnnouncer(context) else null }
    DisposableEffect(announcer) { onDispose { announcer?.shutdown() } }
    return announcer
}

internal fun Announcement.text(context: Context): String =
    when (this) {
        is Announcement.Round -> {
            context.getString(R.string.announce_round, round)
        }

        is Announcement.Turn -> {
            if (needed == null) {
                context.getString(R.string.announce_turn, name)
            } else {
                context.getString(R.string.announce_turn_needed, name, needed)
            }
        }

        is Announcement.Reset -> {
            context.getString(R.string.announce_reset, score)
        }

        is Announcement.Eliminated -> {
            context.getString(R.string.announce_eliminated, name)
        }

        is Announcement.Victory -> {
            context.getString(if (team) R.string.announce_victory_team else R.string.announce_victory, name)
        }

        Announcement.Twelve -> {
            context.getString(R.string.announce_twelve)
        }
    }
