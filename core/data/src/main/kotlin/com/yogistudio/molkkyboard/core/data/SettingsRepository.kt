package com.yogistudio.molkkyboard.core.data

import com.yogistudio.molkkyboard.core.datastore.SettingsDataSource
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setInputMode(mode: InputMode)

    suspend fun setSunlight(enabled: Boolean)

    suspend fun setVoiceAnnouncements(enabled: Boolean)

    suspend fun setSounds(enabled: Boolean)

    suspend fun setCoach(enabled: Boolean)

    suspend fun setShortIntro(enabled: Boolean)

    suspend fun setOnboardingDone(done: Boolean)

    suspend fun recordLaunch(): UserSettings
}

@Singleton
class DataStoreSettingsRepository
    @Inject
    constructor(
        private val source: SettingsDataSource,
    ) : SettingsRepository {
        override val settings: Flow<UserSettings> = source.settings

        override suspend fun setInputMode(mode: InputMode) {
            source.setInputMode(mode)
        }

        override suspend fun setSunlight(enabled: Boolean) {
            source.setSunlight(enabled)
        }

        override suspend fun setVoiceAnnouncements(enabled: Boolean) {
            source.setVoiceAnnouncements(enabled)
        }

        override suspend fun setSounds(enabled: Boolean) {
            source.setSounds(enabled)
        }

        override suspend fun setCoach(enabled: Boolean) {
            source.setCoach(enabled)
        }

        override suspend fun setShortIntro(enabled: Boolean) {
            source.setShortIntro(enabled)
        }

        override suspend fun setOnboardingDone(done: Boolean) {
            source.setOnboardingDone(done)
        }

        override suspend fun recordLaunch(): UserSettings = source.recordLaunch()
    }
