package com.yogistudio.molkkyboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) : ViewModel() {
        val settings: StateFlow<UserSettings> =
            repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UserSettings())

        fun setSunlight(enabled: Boolean) = launch { repository.setSunlight(enabled) }

        fun setInputMode(mode: InputMode) = launch { repository.setInputMode(mode) }

        fun setCoach(enabled: Boolean) = launch { repository.setCoach(enabled) }

        fun setVoiceAnnouncements(enabled: Boolean) = launch { repository.setVoiceAnnouncements(enabled) }

        fun setVoiceRate(rate: Float) = launch { repository.setVoiceRate(rate) }

        fun setVoiceVolume(volume: Float) = launch { repository.setVoiceVolume(volume) }

        fun setOngoingNotification(enabled: Boolean) = launch { repository.setOngoingNotification(enabled) }

        /** « Revoir la prise en main » : les bulles d'aide reviennent à la prochaine partie. */
        fun replayGuide() = launch { repository.setOnboardingDone(false) }

        fun setSounds(enabled: Boolean) = launch { repository.setSounds(enabled) }

        fun setShortIntro(enabled: Boolean) = launch { repository.setShortIntro(enabled) }

        private fun launch(block: suspend () -> Unit) {
            viewModelScope.launch { block() }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
