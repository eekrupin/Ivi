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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.ekrupin.ivi.data.sync.config.SyncConfigStore

@Singleton
class AppSyncRunner @Inject constructor(
    private val fullSyncRunner: FullSyncRunner,
    private val syncStateStore: SyncStateStore,
    private val syncConfigStore: SyncConfigStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _status = MutableStateFlow<AppSyncStatus>(AppSyncStatus.Idle)
    val status: StateFlow<AppSyncStatus> = _status.asStateFlow()

    fun triggerForegroundSync() {
        scope.launch {
            if (!mutex.tryLock()) return@launch
            try {
                val state = syncStateStore.get()
                val config = syncConfigStore.get()
                if (!config.isConfigured) return@launch

                val now = LocalDateTime.now()
                val lastStart = state.lastForegroundSyncStartedAt
                if (lastStart != null && Duration.between(lastStart, now) < Duration.ofSeconds(30)) {
                    return@launch
                }

                syncStateStore.markForegroundSyncStarted(now)
                _status.value = AppSyncStatus.Running(AppSyncTrigger.Foreground)
                _status.value = fullSyncRunner.run(config.baseUrl, config.accessToken).toAppStatus(AppSyncTrigger.Foreground)
            } finally {
                mutex.unlock()
            }
        }
    }

    fun triggerManualSync(baseUrl: String, accessToken: String) {
        scope.launch {
            mutex.withLock {
                syncConfigStore.save(baseUrl.trim(), accessToken.trim())
                _status.value = AppSyncStatus.Running(AppSyncTrigger.Manual)
                _status.value = fullSyncRunner.run(baseUrl.trim(), accessToken.trim()).toAppStatus(AppSyncTrigger.Manual)
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
