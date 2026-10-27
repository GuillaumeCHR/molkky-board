package com.yogistudio.molkkyboard.core.designsystem.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yogistudio.molkkyboard.core.designsystem.R

/** Effets sonores maison (`tools/sounds/generate.py`), synthétisés pour l'app : libres de droits. */
enum class Sound(
    @param:RawRes val res: Int,
) {
    IMPACT(R.raw.sound_impact),
    TWELVE(R.raw.sound_twelve),
    VICTORY(R.raw.sound_victory),
    ELIMINATED(R.raw.sound_eliminated),
}

/**
 * Lecteur des effets sonores (désactivés par défaut, réglage « Sons »). Muet si le téléphone est en silencieux ou en
 * vibreur. Les sons sont courts et préchargés : aucun délai à la lecture.
 */
class SoundEffects(
    context: Context,
) {
    private val audio = context.applicationContext.getSystemService(AudioManager::class.java)
    private val pool =
        SoundPool
            .Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).build()
    private val ids = Sound.entries.associateWith { pool.load(context.applicationContext, it.res, 1) }

    fun play(sound: Sound) {
        if (audio?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        ids[sound]?.let { pool.play(it, VOLUME, VOLUME, 1, 0, 1f) }
    }

    fun release() = pool.release()

    private companion object {
        const val VOLUME = 0.8f
    }
}

/** Lecteur lié à l'écran ; créé seulement si les sons sont activés (rien n'est chargé sinon). */
@Composable
fun rememberSoundEffects(enabled: Boolean): SoundEffects? {
    val context = LocalContext.current
    val sounds = remember(enabled) { if (enabled) SoundEffects(context) else null }
    DisposableEffect(sounds) { onDispose { sounds?.release() } }
    return sounds
}
