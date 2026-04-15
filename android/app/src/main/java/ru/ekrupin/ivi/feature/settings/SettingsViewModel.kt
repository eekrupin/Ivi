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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.ekrupin.ivi.app.core.AppConstants
import ru.ekrupin.ivi.data.sync.AppSyncRunner
import ru.ekrupin.ivi.data.sync.AppSyncStatus
import ru.ekrupin.ivi.data.sync.config.SyncConfigStore
import ru.ekrupin.ivi.domain.model.ReminderSettings
import ru.ekrupin.ivi.domain.repository.ReminderSettingsRepository
import java.time.LocalDateTime

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderSettingsRepository: ReminderSettingsRepository,
    private val appSyncRunner: AppSyncRunner,
    private val syncConfigStore: SyncConfigStore,
) : ViewModel() {
    val settings: StateFlow<ReminderSettings?> = reminderSettingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _syncUiState = MutableStateFlow(SyncUiState())
    val syncUiState: StateFlow<SyncUiState> = combine(
        _syncUiState,
        syncConfigStore.config,
        appSyncRunner.status,
    ) { ui, config, status ->
        val derivedStatus = if (!config.isConfigured && status == AppSyncStatus.Idle) {
            SyncStatus.NotConfigured
        } else {
            status.toSyncStatus()
        }
        ui.copy(
            baseUrl = if (ui.baseUrlEdited) ui.baseUrl else config.baseUrl.ifBlank { ui.baseUrl },
            accessToken = if (ui.accessTokenEdited) ui.accessToken else config.accessToken,
            isConfigured = config.isConfigured,
            configuredBaseUrl = config.baseUrl.ifBlank { null },
            status = derivedStatus,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncUiState())

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
        _syncUiState.value = _syncUiState.value.copy(baseUrl = value, baseUrlEdited = true)
    }

    fun updateSyncAccessToken(value: String) {
        _syncUiState.value = _syncUiState.value.copy(accessToken = value, accessTokenEdited = true)
    }

    fun runSync() {
        val current = _syncUiState.value
        appSyncRunner.triggerManualSync(current.baseUrl.trim(), current.accessToken.trim())
    }

    fun saveSyncConfig() {
        val current = _syncUiState.value
        viewModelScope.launch {
            syncConfigStore.save(current.baseUrl.trim(), current.accessToken.trim())
            _syncUiState.value = _syncUiState.value.copy(
                baseUrlEdited = false,
                accessTokenEdited = false,
            )
        }
    }

    fun clearSyncConfig() {
        viewModelScope.launch {
            syncConfigStore.clear()
            _syncUiState.value = SyncUiState()
        }
    }
}

data class SyncUiState(
    val baseUrl: String = "http://10.0.2.2:8080",
    val accessToken: String = "",
    val configuredBaseUrl: String? = null,
    val isConfigured: Boolean = false,
    val baseUrlEdited: Boolean = false,
    val accessTokenEdited: Boolean = false,
    val status: SyncStatus = SyncStatus.Idle,
)

sealed interface SyncStatus {
    data object NotConfigured : SyncStatus
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
