package ru.ekrupin.ivi.data.sync.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ru.ekrupin.ivi.data.sync.FullSyncRunner
import ru.ekrupin.ivi.data.sync.SyncExecutionGate
import ru.ekrupin.ivi.data.sync.SyncRunResult
import ru.ekrupin.ivi.data.sync.config.SyncConfigStore

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val fullSyncRunner: FullSyncRunner,
    private val syncConfigStore: SyncConfigStore,
    private val syncExecutionGate: SyncExecutionGate,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val config = syncConfigStore.get()
        if (!config.isConfigured) return Result.success()

        val syncResult = syncExecutionGate.runOrSkip {
            fullSyncRunner.run(config.baseUrl, config.accessToken)
        } ?: return Result.success()

        return when (syncResult) {
            is SyncRunResult.Success -> Result.success()
            SyncRunResult.ConflictsDetected -> Result.success()
            is SyncRunResult.RequiresBootstrap -> Result.success()
            SyncRunResult.AuthError -> Result.success()
            is SyncRunResult.ValidationError -> Result.success()
            is SyncRunResult.NetworkError -> Result.retry()
            is SyncRunResult.ServerError -> if (syncResult.code in 500..599) Result.retry() else Result.success()
            is SyncRunResult.UnknownError -> Result.retry()
        }
    }
}
