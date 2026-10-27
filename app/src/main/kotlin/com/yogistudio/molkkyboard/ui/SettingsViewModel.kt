package com.yogistudio.molkkyboard.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yogistudio.molkkyboard.core.data.BackupRepository
import com.yogistudio.molkkyboard.core.data.RestoreError
import com.yogistudio.molkkyboard.core.data.RestoreException
import com.yogistudio.molkkyboard.core.data.RestoreReport
import com.yogistudio.molkkyboard.core.data.SettingsRepository
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/** Résultat de la dernière opération sur les données, affiché sous la section « Données ». */
sealed interface DataMessage {
    data object BackupDone : DataMessage

    data object BackupFailed : DataMessage

    data class Restored(
        val report: RestoreReport,
    ) : DataMessage

    data class RestoreFailed(
        val error: RestoreError,
    ) : DataMessage

    data object Deleted : DataMessage
}

data class DataState(
    val busy: Boolean = false,
    val message: DataMessage? = null,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val repository: SettingsRepository,
        private val backup: BackupRepository,
        @param:ApplicationContext private val context: Context,
    ) : ViewModel() {
        val settings: StateFlow<UserSettings> =
            repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UserSettings())

        private val mutableData = MutableStateFlow(DataState())
        val data: StateFlow<DataState> = mutableData.asStateFlow()

        fun setSunlight(enabled: Boolean) = launch { repository.setSunlight(enabled) }

        fun setInputMode(mode: InputMode) = launch { repository.setInputMode(mode) }

        fun setCoach(enabled: Boolean) = launch { repository.setCoach(enabled) }

        fun setVoiceAnnouncements(enabled: Boolean) = launch { repository.setVoiceAnnouncements(enabled) }

        fun setVoiceRate(rate: Float) = launch { repository.setVoiceRate(rate) }

        fun setVoiceVolume(volume: Float) = launch { repository.setVoiceVolume(volume) }

        fun setOngoingNotification(enabled: Boolean) = launch { repository.setOngoingNotification(enabled) }

        /** « Revoir la prise en main » : les bulles d'aide reviennent à la prochaine partie. */
        fun replayGuide() = launch { repository.setOnboardingDone(false) }

        fun setReadyCard(enabled: Boolean) = launch { repository.setReadyCard(enabled) }

        fun setBeginnerMode(enabled: Boolean) = launch { repository.setBeginnerMode(enabled) }

        fun setSounds(enabled: Boolean) = launch { repository.setSounds(enabled) }

        /** Écrit la sauvegarde dans le fichier choisi par l'utilisateur (sélecteur de documents Android). */
        fun backupTo(uri: Uri) =
            dataOperation {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "wt")?.use { backup.export(it) }
                            ?: throw IOException("flux indisponible")
                    }
                    DataMessage.BackupDone
                } catch (_: IOException) {
                    DataMessage.BackupFailed
                } catch (_: SecurityException) {
                    DataMessage.BackupFailed
                }
            }

        fun restoreFrom(uri: Uri) =
            dataOperation {
                try {
                    val report =
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { backup.restore(it) }
                                ?: throw RestoreException(RestoreError.UNREADABLE)
                        }
                    DataMessage.Restored(report)
                } catch (e: RestoreException) {
                    DataMessage.RestoreFailed(e.error)
                } catch (_: IOException) {
                    DataMessage.RestoreFailed(RestoreError.UNREADABLE)
                } catch (_: SecurityException) {
                    DataMessage.RestoreFailed(RestoreError.UNREADABLE)
                }
            }

        fun deleteAll() =
            dataOperation {
                backup.deleteAll()
                DataMessage.Deleted
            }

        private fun dataOperation(block: suspend () -> DataMessage) {
            if (mutableData.value.busy) return
            mutableData.update { DataState(busy = true) }
            viewModelScope.launch {
                val message = block()
                mutableData.update { DataState(message = message) }
            }
        }

        private fun launch(block: suspend () -> Unit) {
            viewModelScope.launch { block() }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
