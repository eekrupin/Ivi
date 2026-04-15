package ru.ekrupin.ivi.data.sync

import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.ekrupin.ivi.data.auth.session.AuthSessionManager

@Singleton
class AppSyncRunner @Inject constructor(
    private val authorizedSyncRunner: AuthorizedSyncRunner,
    private val syncStateStore: SyncStateStore,
    private val authSessionManager: AuthSessionManager,
    private val syncExecutionGate: SyncExecutionGate,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow<AppSyncStatus>(AppSyncStatus.Idle)
    val status: StateFlow<AppSyncStatus> = _status.asStateFlow()

    fun triggerForegroundSync() {
        scope.launch {
            syncExecutionGate.runOrSkip {
                val state = syncStateStore.get()
                val session = authSessionManager.getSession()
                if (!session.isAuthenticated) return@runOrSkip

                val now = LocalDateTime.now()
                val lastStart = state.lastForegroundSyncStartedAt
                if (lastStart != null && Duration.between(lastStart, now) < Duration.ofSeconds(30)) {
                    return@runOrSkip
                }

                syncStateStore.markForegroundSyncStarted(now)
                _status.value = AppSyncStatus.Running(AppSyncTrigger.Foreground)
                _status.value = authorizedSyncRunner.runWithSession().toAppStatus(AppSyncTrigger.Foreground)
            }
        }
    }

    fun triggerManualSync() {
        scope.launch {
            syncExecutionGate.runOrSkip {
                _status.value = AppSyncStatus.Running(AppSyncTrigger.Manual)
                _status.value = authorizedSyncRunner.runWithSession().toAppStatus(AppSyncTrigger.Manual)
            }
        }
    }
}

enum class AppSyncTrigger {
    Manual,
    Foreground,
}

sealed interface AppSyncStatus {
    data object Idle : AppSyncStatus
    data class Running(val trigger: AppSyncTrigger) : AppSyncStatus
    data class Success(val trigger: AppSyncTrigger) : AppSyncStatus
    data class Conflicts(val trigger: AppSyncTrigger) : AppSyncStatus
    data class RequiresBootstrap(val trigger: AppSyncTrigger, val reason: String) : AppSyncStatus
    data class Error(val trigger: AppSyncTrigger, val message: String) : AppSyncStatus
}

private fun SyncRunResult.toAppStatus(trigger: AppSyncTrigger): AppSyncStatus = when (this) {
    is SyncRunResult.Success -> AppSyncStatus.Success(trigger)
    is SyncRunResult.RequiresBootstrap -> AppSyncStatus.RequiresBootstrap(trigger, reason)
    SyncRunResult.ConflictsDetected -> AppSyncStatus.Conflicts(trigger)
    is SyncRunResult.ValidationError -> AppSyncStatus.Error(trigger, message)
    SyncRunResult.AuthError -> AppSyncStatus.Error(trigger, "Проверьте access token")
    is SyncRunResult.NetworkError -> AppSyncStatus.Error(trigger, message)
    is SyncRunResult.ServerError -> AppSyncStatus.Error(trigger, "Ошибка сервера: HTTP $code")
    is SyncRunResult.UnknownError -> AppSyncStatus.Error(trigger, message)
}
