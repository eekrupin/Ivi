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
import ru.ekrupin.ivi.data.auth.session.AuthSessionManager
import ru.ekrupin.ivi.data.auth.session.AuthSessionResult
import ru.ekrupin.ivi.data.sync.AppSyncRunner
import ru.ekrupin.ivi.data.sync.AppSyncStatus
import ru.ekrupin.ivi.data.sync.config.SyncSessionStore
import ru.ekrupin.ivi.data.sync.conflict.SyncConflictRepository
import ru.ekrupin.ivi.domain.model.ReminderSettings
import ru.ekrupin.ivi.domain.repository.ReminderSettingsRepository
import java.time.LocalDateTime

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderSettingsRepository: ReminderSettingsRepository,
    private val appSyncRunner: AppSyncRunner,
    private val authSessionManager: AuthSessionManager,
    private val syncSessionStore: SyncSessionStore,
    private val syncConflictRepository: SyncConflictRepository,
) : ViewModel() {
    val settings: StateFlow<ReminderSettings?> = reminderSettingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _syncUiState = MutableStateFlow(SyncUiState())
    val syncUiState: StateFlow<SyncUiState> = combine(
        _syncUiState,
        syncSessionStore.session,
        appSyncRunner.status,
        syncConflictRepository.observeConflictCount(),
    ) { ui, session, status, conflictCount ->
        val connectionStatus = when {
            session.isAuthenticated -> ConnectionStatus.Connected(
                backendUrl = session.baseUrl,
                email = session.email ?: "",
                displayName = session.displayName,
            )
            ui.connectionStatus is ConnectionStatus.Loading -> ui.connectionStatus
            ui.connectionStatus is ConnectionStatus.Error -> ui.connectionStatus
            session.isConfigured -> ConnectionStatus.NotConnected(session.baseUrl)
            else -> ConnectionStatus.NotConfigured
        }
        val derivedSyncStatus = if (!session.isAuthenticated && status == AppSyncStatus.Idle) {
            SyncStatus.NotConfigured
        } else {
            status.toSyncStatus()
        }
        ui.copy(
            baseUrl = if (ui.baseUrlEdited) ui.baseUrl else session.baseUrl.ifBlank { ui.baseUrl },
            email = if (ui.emailEdited) ui.email else session.email.orEmpty(),
            displayName = if (ui.displayNameEdited) ui.displayName else session.displayName.orEmpty(),
            isConfigured = session.isConfigured,
            isConnected = session.isAuthenticated,
            connectionStatus = connectionStatus,
            status = derivedSyncStatus,
            conflictCount = conflictCount,
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

    fun updateEmail(value: String) {
        _syncUiState.value = _syncUiState.value.copy(email = value, emailEdited = true)
    }

    fun updatePassword(value: String) {
        _syncUiState.value = _syncUiState.value.copy(password = value)
    }

    fun updateDisplayName(value: String) {
        _syncUiState.value = _syncUiState.value.copy(displayName = value, displayNameEdited = true)
    }

    fun runSync() {
        appSyncRunner.triggerManualSync()
    }

    fun login() {
        val current = _syncUiState.value
        viewModelScope.launch {
            _syncUiState.value = current.copy(connectionStatus = ConnectionStatus.Loading)
            val result = authSessionManager.login(
                baseUrl = current.baseUrl.trim(),
                email = current.email.trim(),
                password = current.password,
            )
            _syncUiState.value = _syncUiState.value.afterAuthResult(result)
        }
    }

    fun register() {
        val current = _syncUiState.value
        viewModelScope.launch {
            _syncUiState.value = current.copy(connectionStatus = ConnectionStatus.Loading)
            val result = authSessionManager.register(
                baseUrl = current.baseUrl.trim(),
                email = current.email.trim(),
                password = current.password,
                displayName = current.displayName.trim(),
            )
            _syncUiState.value = _syncUiState.value.afterAuthResult(result)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authSessionManager.logout()
            _syncUiState.value = SyncUiState()
        }
    }
}

data class SyncUiState(
    val baseUrl: String = "http://10.0.2.2:8080",
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isConfigured: Boolean = false,
    val isConnected: Boolean = false,
    val baseUrlEdited: Boolean = false,
    val emailEdited: Boolean = false,
    val displayNameEdited: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.NotConfigured,
    val status: SyncStatus = SyncStatus.Idle,
    val conflictCount: Int = 0,
)

sealed interface ConnectionStatus {
    data object NotConfigured : ConnectionStatus
    data class NotConnected(val backendUrl: String) : ConnectionStatus
    data object Loading : ConnectionStatus
    data class Connected(
        val backendUrl: String,
        val email: String,
        val displayName: String?,
    ) : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus
}

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

private fun SyncUiState.afterAuthResult(result: AuthSessionResult): SyncUiState = when (result) {
    is AuthSessionResult.Success -> copy(
        password = "",
        connectionStatus = ConnectionStatus.Connected(
            backendUrl = baseUrl.trim(),
            email = result.email,
            displayName = result.displayName,
        ),
        emailEdited = false,
        displayNameEdited = false,
        baseUrlEdited = false,
    )
    AuthSessionResult.NotConnected -> copy(connectionStatus = ConnectionStatus.NotConfigured)
    AuthSessionResult.InvalidCredentials -> copy(connectionStatus = ConnectionStatus.Error("Неверный email или пароль"))
    is AuthSessionResult.ValidationError -> copy(connectionStatus = ConnectionStatus.Error(result.message))
    is AuthSessionResult.NetworkError -> copy(connectionStatus = ConnectionStatus.Error(result.message))
    is AuthSessionResult.ServerError -> copy(connectionStatus = ConnectionStatus.Error("Ошибка сервера: HTTP ${result.code}"))
    is AuthSessionResult.AuthError -> copy(connectionStatus = ConnectionStatus.Error(result.message))
    is AuthSessionResult.UnknownError -> copy(connectionStatus = ConnectionStatus.Error(result.message))
}

private fun AppSyncStatus.toSyncStatus(): SyncStatus = when (this) {
    AppSyncStatus.Idle -> SyncStatus.Idle
    is AppSyncStatus.Running -> SyncStatus.Running
    is AppSyncStatus.Success -> if (trigger == ru.ekrupin.ivi.data.sync.AppSyncTrigger.Foreground) SyncStatus.ForegroundSuccess else SyncStatus.Success
    is AppSyncStatus.Conflicts -> SyncStatus.Conflicts
    is AppSyncStatus.RequiresBootstrap -> SyncStatus.RequiresBootstrap
    is AppSyncStatus.Error -> SyncStatus.Error(message)
}
