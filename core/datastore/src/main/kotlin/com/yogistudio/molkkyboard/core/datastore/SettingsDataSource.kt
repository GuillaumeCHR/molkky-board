package com.yogistudio.molkkyboard.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Réglages persistés dans DataStore (Preferences). */
@Singleton
class SettingsDataSource
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        val settings: Flow<UserSettings> = dataStore.data.map { it.toSettings() }

        suspend fun setInputMode(mode: InputMode) = dataStore.edit { it[Keys.INPUT_MODE] = mode.name }

        suspend fun setSunlight(enabled: Boolean) = dataStore.edit { it[Keys.SUNLIGHT] = enabled }

        suspend fun setVoiceAnnouncements(enabled: Boolean) = dataStore.edit { it[Keys.VOICE] = enabled }

        suspend fun setVoiceRate(rate: Float) = dataStore.edit { it[Keys.VOICE_RATE] = rate }

        suspend fun setVoiceVolume(volume: Float) = dataStore.edit { it[Keys.VOICE_VOLUME] = volume }

        suspend fun setOngoingNotification(enabled: Boolean) =
            dataStore.edit {
                it[Keys.ONGOING_NOTIFICATION] = enabled
            }

        suspend fun setNotificationPrompted() = dataStore.edit { it[Keys.NOTIFICATION_PROMPTED] = true }

        suspend fun setSounds(enabled: Boolean) = dataStore.edit { it[Keys.SOUNDS] = enabled }

        suspend fun setCoach(enabled: Boolean) = dataStore.edit { it[Keys.COACH] = enabled }

        suspend fun setShortIntro(enabled: Boolean) = dataStore.edit { it[Keys.SHORT_INTRO] = enabled }

        suspend fun setOnboardingDone(done: Boolean) = dataStore.edit { it[Keys.ONBOARDING_DONE] = done }

        /** Les surcharges par équipe dépendent d'une partie : elles ne font pas partie des valeurs par défaut. */
        suspend fun setPlayfulRules(config: GameConfig) =
            dataStore.edit { it[Keys.PLAYFUL_RULES] = json.encodeToString(config.copy(teamOverrides = emptyMap())) }

        /** Compte un lancement de l'application et renvoie les réglages à jour. */
        suspend fun recordLaunch(): UserSettings =
            dataStore.edit { it[Keys.LAUNCH_COUNT] = (it[Keys.LAUNCH_COUNT] ?: 0) + 1 }.toSettings()

        private fun Preferences.toSettings() =
            UserSettings(
                inputMode = decodeInputMode(this[Keys.INPUT_MODE]),
                sunlight = this[Keys.SUNLIGHT] ?: false,
                voiceAnnouncements = this[Keys.VOICE] ?: false,
                voiceRate = this[Keys.VOICE_RATE] ?: 1f,
                voiceVolume = this[Keys.VOICE_VOLUME] ?: 1f,
                ongoingNotification = this[Keys.ONGOING_NOTIFICATION] ?: true,
                notificationPrompted = this[Keys.NOTIFICATION_PROMPTED] ?: false,
                sounds = this[Keys.SOUNDS] ?: false,
                coach = this[Keys.COACH] ?: true,
                shortIntro = this[Keys.SHORT_INTRO] ?: false,
                launchCount = this[Keys.LAUNCH_COUNT] ?: 0,
                onboardingDone = this[Keys.ONBOARDING_DONE] ?: false,
                playfulRules = this[Keys.PLAYFUL_RULES]?.let(::decodeRules) ?: GameConfig.Playful,
            )

        private fun decodeInputMode(name: String?): InputMode =
            InputMode.entries.firstOrNull { it.name == name } ?: InputMode.PINS

        /** Une valeur illisible (version future, fichier abîmé) redonne les exceptions par défaut. */
        private fun decodeRules(encoded: String): GameConfig? =
            try {
                json.decodeFromString<GameConfig>(encoded)
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }

        private val json = Json { ignoreUnknownKeys = true }

        private object Keys {
            val INPUT_MODE = stringPreferencesKey("input_mode")
            val SUNLIGHT = booleanPreferencesKey("sunlight")
            val VOICE = booleanPreferencesKey("voice_announcements")
            val VOICE_RATE = floatPreferencesKey("voice_rate")
            val VOICE_VOLUME = floatPreferencesKey("voice_volume")
            val ONGOING_NOTIFICATION = booleanPreferencesKey("ongoing_notification")
            val NOTIFICATION_PROMPTED = booleanPreferencesKey("notification_prompted")
            val SOUNDS = booleanPreferencesKey("sounds")
            val COACH = booleanPreferencesKey("coach")
            val SHORT_INTRO = booleanPreferencesKey("short_intro")
            val LAUNCH_COUNT = intPreferencesKey("launch_count")
            val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
            val PLAYFUL_RULES = stringPreferencesKey("playful_rules")
        }
    }
