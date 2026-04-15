package ru.ekrupin.ivi.data.sync

import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import ru.ekrupin.ivi.data.sync.remote.SyncHttpException

interface FullSyncRunner {
    suspend fun run(baseUrl: String, accessToken: String): SyncRunResult
}

class RunFullSyncUseCase @Inject constructor(
    private val syncEngine: SyncEngine,
    private val syncStateStore: SyncStateStore,
    private val syncOutboxStore: SyncOutboxStore,
) : FullSyncRunner {
    suspend operator fun invoke(baseUrl: String, accessToken: String): SyncRunResult = run(baseUrl, accessToken)

    override suspend fun run(baseUrl: String, accessToken: String): SyncRunResult {
        if (baseUrl.isBlank() || accessToken.isBlank()) {
            return SyncRunResult.ValidationError("Укажите адрес сервера и access token")
        }

        return try {
            val state = syncStateStore.get()
            val hasPendingOutbox = syncOutboxStore.pending(limit = 1).isNotEmpty()

            if (state.requiresBootstrap || state.cursor == null) {
                if (hasPendingOutbox) {
                    return SyncRunResult.RequiresBootstrap(
                        reason = "Нужен bootstrap, но в outbox уже есть локальные изменения. Сначала нужно вручную очистить или разрулить локальное состояние V1.",
                    )
                }

                syncEngine.bootstrapImport(baseUrl, accessToken)
                return SyncRunResult.Success(bootstrapPerformed = true, pushPerformed = false, changesPerformed = false)
            }

            var hadConflicts = false
            var pushPerformed = false
            if (hasPendingOutbox) {
                pushPerformed = true
                when (val pushResult = syncEngine.drainOutbox(baseUrl, accessToken, deviceId = deviceId())) {
                    PushDrainResult.Empty -> Unit
                    PushDrainResult.RequiresBootstrap -> {
                        return SyncRunResult.RequiresBootstrap(
                            reason = "Сервер запросил полный bootstrap перед продолжением sync",
                        )
                    }

                    is PushDrainResult.Applied -> {
                        hadConflicts = pushResult.conflictCount > 0
                    }
                }
            }

            syncEngine.pullChanges(baseUrl, accessToken)
            if (hadConflicts) {
                SyncRunResult.ConflictsDetected
            } else {
                SyncRunResult.Success(bootstrapPerformed = false, pushPerformed = pushPerformed, changesPerformed = true)
            }
        } catch (exception: SyncHttpException) {
            when (exception.code) {
                401 -> SyncRunResult.AuthError
                in 500..599 -> SyncRunResult.ServerError(exception.code)
                else -> SyncRunResult.NetworkError(exception.message ?: "HTTP ${exception.code}")
            }
        } catch (exception: IOException) {
            SyncRunResult.NetworkError(exception.message ?: "Ошибка сети")
        } catch (exception: Exception) {
            SyncRunResult.UnknownError(exception.message ?: "Неизвестная ошибка")
        }
    }

    private fun deviceId(): String = "android-${UUID.randomUUID()}"
}

sealed interface SyncRunResult {
    data class Success(
        val bootstrapPerformed: Boolean,
        val pushPerformed: Boolean,
        val changesPerformed: Boolean,
    ) : SyncRunResult

    data class RequiresBootstrap(val reason: String) : SyncRunResult
    data object ConflictsDetected : SyncRunResult
    data class ValidationError(val message: String) : SyncRunResult
    data object AuthError : SyncRunResult
    data class NetworkError(val message: String) : SyncRunResult
    data class ServerError(val code: Int) : SyncRunResult
    data class UnknownError(val message: String) : SyncRunResult
}
