package com.yogistudio.molkkyboard.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

        suspend fun setSounds(enabled: Boolean) = dataStore.edit { it[Keys.SOUNDS] = enabled }

        suspend fun setCoach(enabled: Boolean) = dataStore.edit { it[Keys.COACH] = enabled }

        suspend fun setShortIntro(enabled: Boolean) = dataStore.edit { it[Keys.SHORT_INTRO] = enabled }

        suspend fun setOnboardingDone(done: Boolean) = dataStore.edit { it[Keys.ONBOARDING_DONE] = done }

        /** Compte un lancement de l'application et renvoie les réglages à jour. */
        suspend fun recordLaunch(): UserSettings =
            dataStore.edit { it[Keys.LAUNCH_COUNT] = (it[Keys.LAUNCH_COUNT] ?: 0) + 1 }.toSettings()

        private fun Preferences.toSettings() =
            UserSettings(
                inputMode =
                    this[Keys.INPUT_MODE]?.let { name -> InputMode.entries.firstOrNull { it.name == name } }
                        ?: InputMode.PINS,
                sunlight = this[Keys.SUNLIGHT] ?: false,
                voiceAnnouncements = this[Keys.VOICE] ?: false,
                sounds = this[Keys.SOUNDS] ?: false,
                coach = this[Keys.COACH] ?: true,
                shortIntro = this[Keys.SHORT_INTRO] ?: false,
                launchCount = this[Keys.LAUNCH_COUNT] ?: 0,
                onboardingDone = this[Keys.ONBOARDING_DONE] ?: false,
            )

        private object Keys {
            val INPUT_MODE = stringPreferencesKey("input_mode")
            val SUNLIGHT = booleanPreferencesKey("sunlight")
            val VOICE = booleanPreferencesKey("voice_announcements")
            val SOUNDS = booleanPreferencesKey("sounds")
            val COACH = booleanPreferencesKey("coach")
            val SHORT_INTRO = booleanPreferencesKey("short_intro")
            val LAUNCH_COUNT = intPreferencesKey("launch_count")
            val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        }
    }
