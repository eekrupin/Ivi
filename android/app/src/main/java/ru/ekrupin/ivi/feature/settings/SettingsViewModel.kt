package ru.ekrupin.ivi.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.ekrupin.ivi.app.core.AppConstants
import ru.ekrupin.ivi.data.sync.AppSyncRunner
import ru.ekrupin.ivi.data.sync.AppSyncStatus
import ru.ekrupin.ivi.data.sync.SyncStateStore
import ru.ekrupin.ivi.domain.model.ReminderSettings
import ru.ekrupin.ivi.domain.repository.ReminderSettingsRepository
import java.time.LocalDateTime

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderSettingsRepository: ReminderSettingsRepository,
    private val appSyncRunner: AppSyncRunner,
    private val syncStateStore: SyncStateStore,
) : ViewModel() {
    val settings: StateFlow<ReminderSettings?> = reminderSettingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _syncUiState = MutableStateFlow(SyncUiState())
    val syncUiState: StateFlow<SyncUiState> = combine(
        _syncUiState,
        appSyncRunner.status,
    ) { ui, status ->
        ui.copy(status = status.toSyncStatus())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncUiState())

    init {
        viewModelScope.launch {
            val state = syncStateStore.get()
            _syncUiState.value = _syncUiState.value.copy(
                baseUrl = state.configuredBaseUrl ?: _syncUiState.value.baseUrl,
                accessToken = state.configuredAccessToken ?: "",
            )
        }
    }

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
        viewModelScope.launch {
            syncStateStore.saveSyncConfig(value, _syncUiState.value.accessToken)
        }
    }

    fun updateSyncAccessToken(value: String) {
        _syncUiState.value = _syncUiState.value.copy(accessToken = value)
        viewModelScope.launch {
            syncStateStore.saveSyncConfig(_syncUiState.value.baseUrl, value)
        }
    }

    fun runSync() {
        val current = _syncUiState.value
        appSyncRunner.triggerManualSync(current.baseUrl.trim(), current.accessToken.trim())
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
    data object ForegroundSuccess : SyncStatus
    data class Error(val message: String) : SyncStatus
}

private fun AppSyncStatus.toSyncStatus(): SyncStatus = when (this) {
    AppSyncStatus.Idle -> SyncStatus.Idle
    is AppSyncStatus.Running -> SyncStatus.Running
    is AppSyncStatus.Success -> if (trigger == ru.ekrupin.ivi.data.sync.AppSyncTrigger.Foreground) SyncStatus.ForegroundSuccess else SyncStatus.Success
    is AppSyncStatus.Conflicts -> SyncStatus.Conflicts
    is AppSyncStatus.RequiresBootstrap -> SyncStatus.RequiresBootstrap
    is AppSyncStatus.Error -> SyncStatus.Error(message)
}
