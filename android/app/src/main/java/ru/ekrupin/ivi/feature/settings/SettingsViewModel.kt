package ru.ekrupin.ivi.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.ekrupin.ivi.app.core.AppConstants
import ru.ekrupin.ivi.data.sync.RunFullSyncUseCase
import ru.ekrupin.ivi.data.sync.SyncRunResult
import ru.ekrupin.ivi.domain.model.ReminderSettings
import ru.ekrupin.ivi.domain.repository.ReminderSettingsRepository
import java.time.LocalDateTime

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderSettingsRepository: ReminderSettingsRepository,
    private val runFullSyncUseCase: RunFullSyncUseCase,
) : ViewModel() {
    val settings: StateFlow<ReminderSettings?> = reminderSettingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _syncUiState = MutableStateFlow(SyncUiState())
    val syncUiState: StateFlow<SyncUiState> = _syncUiState.asStateFlow()

    fun saveSettings(
        firstEnabled: Boolean,
        firstDays: Int,
        secondEnabled: Boolean,
        secondDays: Int,
    ) {
        val current = settings.value
        viewModelScope.launch {
            reminderSettingsRepository.saveSettings(
                ReminderSettings(
                    id = current?.id ?: AppConstants.REMINDER_SETTINGS_ID,
                    firstReminderEnabled = firstEnabled,
                    firstReminderDaysBefore = firstDays,
                    secondReminderEnabled = secondEnabled,
                    secondReminderDaysBefore = secondDays,
                    createdAt = current?.createdAt ?: LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )
        }
    }

    fun updateSyncBaseUrl(value: String) {
        _syncUiState.value = _syncUiState.value.copy(baseUrl = value)
    }

    fun updateSyncAccessToken(value: String) {
        _syncUiState.value = _syncUiState.value.copy(accessToken = value)
    }

    fun runSync() {
        val current = _syncUiState.value
        viewModelScope.launch {
            _syncUiState.value = current.copy(status = SyncStatus.Running)
            val result = runFullSyncUseCase(current.baseUrl.trim(), current.accessToken.trim())
            _syncUiState.value = _syncUiState.value.copy(status = result.toSyncStatus())
        }
    }
}

data class SyncUiState(
    val baseUrl: String = "http://10.0.2.2:8080",
    val accessToken: String = "",
    val status: SyncStatus = SyncStatus.Idle,
)

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Running : SyncStatus
    data object Success : SyncStatus
    data object Conflicts : SyncStatus
    data object RequiresBootstrap : SyncStatus
    data class Error(val message: String) : SyncStatus
}

private fun SyncRunResult.toSyncStatus(): SyncStatus = when (this) {
    is SyncRunResult.Success -> SyncStatus.Success
    is SyncRunResult.RequiresBootstrap -> SyncStatus.RequiresBootstrap
    SyncRunResult.ConflictsDetected -> SyncStatus.Conflicts
    is SyncRunResult.ValidationError -> SyncStatus.Error(message)
    SyncRunResult.AuthError -> SyncStatus.Error("Проверьте access token")
    is SyncRunResult.NetworkError -> SyncStatus.Error(message)
    is SyncRunResult.ServerError -> SyncStatus.Error("Ошибка сервера: HTTP $code")
    is SyncRunResult.UnknownError -> SyncStatus.Error(message)
}
