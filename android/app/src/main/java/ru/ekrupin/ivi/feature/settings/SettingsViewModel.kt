package ru.ekrupin.ivi.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.time.LocalDateTime
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
import ru.ekrupin.ivi.data.pet.remote.PetAccessRemoteDataSource
import ru.ekrupin.ivi.data.pet.remote.RemoteLeavePetOptions
import ru.ekrupin.ivi.data.sync.AppSyncRunner
import ru.ekrupin.ivi.data.sync.AppSyncStatus
import ru.ekrupin.ivi.data.sync.ClearServerPetLocalDataUseCase
import ru.ekrupin.ivi.data.sync.config.SyncSessionStore
import ru.ekrupin.ivi.data.sync.conflict.SyncConflictRepository
import ru.ekrupin.ivi.data.sync.remote.SyncHttpException
import ru.ekrupin.ivi.domain.model.ReminderSettings
import ru.ekrupin.ivi.domain.repository.ReminderSettingsRepository

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderSettingsRepository: ReminderSettingsRepository,
    private val appSyncRunner: AppSyncRunner,
    private val authSessionManager: AuthSessionManager,
    private val syncSessionStore: SyncSessionStore,
    private val syncConflictRepository: SyncConflictRepository,
    private val petAccessRemoteDataSource: PetAccessRemoteDataSource,
    private val clearServerPetLocalData: ClearServerPetLocalDataUseCase,
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

    fun updateInviteCode(value: String) {
        _syncUiState.value = _syncUiState.value.copy(inviteCode = value)
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

    fun refreshCurrentPetAccess(force: Boolean = false) {
        val current = _syncUiState.value
        if (current.petAccess is PetAccessUiState.Loading || current.petAccess is PetAccessUiState.Known) return
        if (!force && current.petAccess is PetAccessUiState.NoServerPet) return
        viewModelScope.launch {
            val session = authSessionManager.getSession()
            if (!session.isAuthenticated) return@launch
            _syncUiState.value = _syncUiState.value.copy(petAccess = PetAccessUiState.Loading)
            try {
                val access = petAccessRemoteDataSource.getCurrentPetAccess(session.baseUrl, session.accessToken)
                _syncUiState.value = _syncUiState.value.copy(petAccess = access.toPetAccessUiState())
            } catch (exception: Exception) {
                _syncUiState.value = _syncUiState.value.copy(
                    petAccess = if (exception.isCurrentPetNotFound()) PetAccessUiState.NoServerPet else PetAccessUiState.Unknown,
                )
            }
        }
    }

    fun publishLocalDataToServer() {
        appSyncRunner.triggerPublishLocalDataToServer()
    }

    fun replaceLocalDataFromServer() {
        appSyncRunner.triggerReplaceLocalDataFromServer()
    }

    fun createInvite() {
        viewModelScope.launch {
            val session = authSessionManager.getSession()
            if (!session.isAuthenticated) {
                _syncUiState.value = _syncUiState.value.copy(inviteStatus = InviteStatus.Error("Сначала войдите в синхронизацию"))
                return@launch
            }

            _syncUiState.value = _syncUiState.value.copy(inviteStatus = InviteStatus.Loading)
            try {
                val petAccess = when (val access = _syncUiState.value.petAccess) {
                    is PetAccessUiState.Known -> access
                    else -> petAccessRemoteDataSource.getCurrentPetAccess(session.baseUrl, session.accessToken)
                        .toPetAccessUiState()
                        .also { _syncUiState.value = _syncUiState.value.copy(petAccess = it) }
                }
                if (petAccess.role != PetAccessRole.Owner) {
                    _syncUiState.value = _syncUiState.value.copy(inviteStatus = InviteStatus.Error("Код приглашения может создать только владелец питомца."))
                    return@launch
                }
                val invite = petAccessRemoteDataSource.createInvite(session.baseUrl, session.accessToken, petAccess.petId)
                _syncUiState.value = _syncUiState.value.copy(inviteStatus = InviteStatus.Created(invite.code))
            } catch (exception: Exception) {
                _syncUiState.value = _syncUiState.value.copy(inviteStatus = InviteStatus.Error(exception.toInviteMessage("Не удалось создать приглашение")))
            }
        }
    }

    fun acceptInvite() {
        viewModelScope.launch {
            val current = _syncUiState.value
            val code = current.inviteCode.trim()
            if (code.isBlank()) {
                _syncUiState.value = current.copy(inviteStatus = InviteStatus.Error("Введите код приглашения"))
                return@launch
            }

            val session = authSessionManager.getSession()
            if (!session.isAuthenticated) {
                _syncUiState.value = current.copy(inviteStatus = InviteStatus.Error("Сначала войдите в синхронизацию"))
                return@launch
            }

            _syncUiState.value = current.copy(inviteStatus = InviteStatus.Loading)
            try {
                val access = petAccessRemoteDataSource.acceptInvite(session.baseUrl, session.accessToken, code)
                _syncUiState.value = _syncUiState.value.copy(
                    inviteCode = "",
                    inviteStatus = InviteStatus.Accepted(access.pet.name, access.membership.role.toPetAccessRole()),
                    petAccess = access.toPetAccessUiState(),
                    leavePetStatus = LeavePetStatus.Idle,
                )
            } catch (exception: Exception) {
                _syncUiState.value = _syncUiState.value.copy(inviteStatus = InviteStatus.Error(exception.toInviteMessage("Не удалось принять приглашение")))
            }
        }
    }

    fun leaveSharedPet() {
        viewModelScope.launch {
            val current = _syncUiState.value
            val petAccess = current.petAccess as? PetAccessUiState.Known
            val session = authSessionManager.getSession()
            if (!session.isAuthenticated) {
                _syncUiState.value = current.copy(leavePetStatus = LeavePetStatus.Error("Сначала войдите в синхронизацию"))
                return@launch
            }
            if (petAccess?.role == PetAccessRole.Owner) {
                loadOwnerLeaveOptions(current, session.baseUrl, session.accessToken)
            } else if (petAccess?.role == PetAccessRole.Member) {
                leavePet(current, session.baseUrl, session.accessToken)
            } else {
                _syncUiState.value = current.copy(leavePetStatus = LeavePetStatus.Error("Сначала загрузите данные общего доступа"))
            }
        }
    }

    fun transferOwnerAndLeave(userId: String) {
        viewModelScope.launch {
            val current = _syncUiState.value
            val session = authSessionManager.getSession()
            if (!session.isAuthenticated) {
                _syncUiState.value = current.copy(leavePetStatus = LeavePetStatus.Error("Сначала войдите в синхронизацию"))
                return@launch
            }
            leavePet(current, session.baseUrl, session.accessToken, transferOwnerToUserId = userId)
        }
    }

    fun deletePetAndLeave() {
        viewModelScope.launch {
            val current = _syncUiState.value
            val session = authSessionManager.getSession()
            if (!session.isAuthenticated) {
                _syncUiState.value = current.copy(leavePetStatus = LeavePetStatus.Error("Сначала войдите в синхронизацию"))
                return@launch
            }
            leavePet(current, session.baseUrl, session.accessToken, deletePet = true)
        }
    }

    private suspend fun loadOwnerLeaveOptions(current: SyncUiState, baseUrl: String, accessToken: String) {
        _syncUiState.value = current.copy(leavePetStatus = LeavePetStatus.Loading)
        try {
            val options = petAccessRemoteDataSource.getCurrentPetLeaveOptions(baseUrl, accessToken)
            _syncUiState.value = _syncUiState.value.copy(leavePetStatus = options.toLeavePetStatus())
        } catch (exception: Exception) {
            _syncUiState.value = _syncUiState.value.copy(leavePetStatus = exception.toLeavePetStatus())
        }
    }

    private suspend fun leavePet(
        current: SyncUiState,
        baseUrl: String,
        accessToken: String,
        transferOwnerToUserId: String? = null,
        deletePet: Boolean = false,
    ) {
        _syncUiState.value = current.copy(leavePetStatus = LeavePetStatus.Loading)
        try {
            petAccessRemoteDataSource.leaveCurrentPet(
                baseUrl = baseUrl,
                accessToken = accessToken,
                transferOwnerToUserId = transferOwnerToUserId,
                deletePet = deletePet,
            )
            clearServerPetLocalData()
            _syncUiState.value = _syncUiState.value.copy(
                petAccess = PetAccessUiState.NoServerPet,
                leavePetStatus = LeavePetStatus.Left,
                inviteStatus = InviteStatus.Idle,
            )
        } catch (exception: Exception) {
            if (exception.isCurrentPetNotFound()) {
                clearServerPetLocalData()
                _syncUiState.value = _syncUiState.value.copy(
                    petAccess = PetAccessUiState.NoServerPet,
                    leavePetStatus = LeavePetStatus.Left,
                    inviteStatus = InviteStatus.Idle,
                )
            } else {
                _syncUiState.value = _syncUiState.value.copy(leavePetStatus = exception.toLeavePetStatus())
            }
        }
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
    val inviteCode: String = "",
    val inviteStatus: InviteStatus = InviteStatus.Idle,
    val leavePetStatus: LeavePetStatus = LeavePetStatus.Idle,
    val petAccess: PetAccessUiState = PetAccessUiState.Unknown,
)

sealed interface InviteStatus {
    data object Idle : InviteStatus
    data object Loading : InviteStatus
    data class Created(val code: String) : InviteStatus
    data class Accepted(val petName: String, val role: PetAccessRole) : InviteStatus
    data class Error(val message: String) : InviteStatus
}

sealed interface PetAccessUiState {
    data object Unknown : PetAccessUiState
    data object Loading : PetAccessUiState
    data object NoServerPet : PetAccessUiState
    data class Known(
        val petId: String,
        val petName: String,
        val role: PetAccessRole,
    ) : PetAccessUiState
}

sealed interface LeavePetStatus {
    data object Idle : LeavePetStatus
    data object Loading : LeavePetStatus
    data object Left : LeavePetStatus
    data class TransferRequired(val candidates: List<PetOwnerTransferCandidate>) : LeavePetStatus
    data object DeletePetConfirmation : LeavePetStatus
    data class Error(val message: String) : LeavePetStatus
}

data class PetOwnerTransferCandidate(
    val id: String,
    val email: String,
    val displayName: String?,
)

enum class PetAccessRole {
    Owner,
    Member,
    Unknown,
}

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
    data object NoServerPet : SyncStatus
    data object ForegroundSuccess : SyncStatus
    data class Error(val message: String) : SyncStatus
}

private fun SyncUiState.afterAuthResult(result: AuthSessionResult): SyncUiState = when (result) {
    is AuthSessionResult.Success -> copy(
        password = "",
        petAccess = PetAccessUiState.Unknown,
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
    is AppSyncStatus.NoServerPet -> SyncStatus.NoServerPet
    is AppSyncStatus.Error -> SyncStatus.Error(message)
}

private fun Exception.toInviteMessage(fallback: String): String {
    if (this is SyncHttpException) {
        val body = message.orEmpty()
        return when {
            code == 404 && body.contains("current_pet_not_found") -> "Сначала отправьте данные этого устройства на сервер, чтобы создать серверного питомца"
            code == 404 && body.contains("invite_not_found") -> "Приглашение не найдено. Проверьте код"
            code == 409 && body.contains("user_already_bound_to_pet") -> "Этот аккаунт уже привязан к питомцу"
            code == 409 && body.contains("invite_pet_not_available") -> "Приглашение больше недоступно: питомец удален или закрыт"
            code == 409 && body.contains("invite_expired") -> "Срок действия приглашения истек"
            code == 409 && body.contains("invite_not_active") -> "Приглашение уже использовано или недействительно"
            code == 403 && body.contains("owner_required") -> "Создать приглашение может только владелец питомца"
            code in 500..599 -> "Ошибка сервера: HTTP $code"
            else -> fallback
        }
    }
    if (this is SocketTimeoutException || this is InterruptedIOException || message.orEmpty().contains("timeout", ignoreCase = true)) {
        return "Сервер не ответил вовремя. Проверьте подключение и попробуйте еще раз"
    }
    if (this is IOException) {
        return "Не удалось подключиться к серверу. Проверьте адрес и сеть"
    }
    return message ?: fallback
}

private fun Exception.toLeavePetStatus(): LeavePetStatus {
    if (this is SyncHttpException) {
        val body = message.orEmpty()
        return when {
            code == 409 && body.contains("owner_leave_requires_action") -> LeavePetStatus.Error("Чтобы выйти, сначала выберите передачу владения или удаление питомца.")
            code == 409 && body.contains("owner_delete_requires_no_members") -> LeavePetStatus.Error("Нельзя удалить питомца, пока у него есть другие участники.")
            code == 409 && body.contains("invalid_owner_transfer_candidate") -> LeavePetStatus.Error("Нельзя передать владение выбранному участнику.")
            code == 409 && body.contains("owner_cannot_leave_pet") -> LeavePetStatus.Error("Владелец не может выйти без передачи владения или удаления питомца.")
            code == 401 -> LeavePetStatus.Error("Сессия истекла. Войдите в синхронизацию заново")
            code in 500..599 -> LeavePetStatus.Error("Ошибка сервера: HTTP $code")
            else -> LeavePetStatus.Error(message ?: "Не удалось покинуть общего питомца")
        }
    }
    if (this is SocketTimeoutException || this is InterruptedIOException || message.orEmpty().contains("timeout", ignoreCase = true)) {
        return LeavePetStatus.Error("Сервер не ответил вовремя. Проверьте подключение и попробуйте еще раз")
    }
    if (this is IOException) {
        return LeavePetStatus.Error("Не удалось подключиться к серверу. Проверьте адрес и сеть")
    }
    return LeavePetStatus.Error(message ?: "Не удалось покинуть общего питомца")
}

private fun Exception.isCurrentPetNotFound(): Boolean = this is SyncHttpException &&
    code == 404 &&
    message.orEmpty().contains("current_pet_not_found")

private fun ru.ekrupin.ivi.data.pet.remote.RemotePetAccessContext.toPetAccessUiState(): PetAccessUiState.Known = PetAccessUiState.Known(
    petId = pet.id,
    petName = pet.name,
    role = membership.role.toPetAccessRole(),
)

private fun RemoteLeavePetOptions.toLeavePetStatus(): LeavePetStatus = when {
    transferCandidates.isNotEmpty() -> LeavePetStatus.TransferRequired(
        candidates = transferCandidates.map { candidate ->
            PetOwnerTransferCandidate(
                id = candidate.id,
                email = candidate.email,
                displayName = candidate.displayName,
            )
        },
    )
    canDeletePet -> LeavePetStatus.DeletePetConfirmation
    else -> LeavePetStatus.Error("Сейчас нельзя покинуть питомца. Попробуйте позже.")
}

private fun String.toPetAccessRole(): PetAccessRole = when (uppercase()) {
    "OWNER" -> PetAccessRole.Owner
    "MEMBER" -> PetAccessRole.Member
    else -> PetAccessRole.Unknown
}
