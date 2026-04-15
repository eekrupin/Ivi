package ru.ekrupin.ivi.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AppSyncRunnerTest {
    @Test
    fun foregroundSync_doesNotRunWithoutConfig() = runBlocking {
        val useCase = FakeRunFullSyncUseCase()
        val stateStore = FakeRunnerSyncStateStore()
        val runner = AppSyncRunner(useCase, stateStore)

        runner.triggerForegroundSync()
        delay(100)

        assertEquals(0, useCase.calls)
    }

    @Test
    fun foregroundSync_runsWhenConfigExists() = runBlocking {
        val useCase = FakeRunFullSyncUseCase()
        val stateStore = FakeRunnerSyncStateStore(
            state = SyncReadState(
                cursor = "changes:1000",
                lastBootstrapAt = null,
                lastChangesAt = null,
                lastSuccessfulReadAt = null,
                requiresBootstrap = false,
                configuredBaseUrl = "http://localhost:8080",
                configuredAccessToken = "token",
                lastForegroundSyncStartedAt = null,
            ),
        )
        val runner = AppSyncRunner(useCase, stateStore)

        runner.triggerForegroundSync()
        delay(100)

        assertEquals(1, useCase.calls)
        assertTrue(runner.status.value is AppSyncStatus.Success)
    }

    @Test
    fun foregroundSync_respectsCooldown() = runBlocking {
        val useCase = FakeRunFullSyncUseCase()
        val stateStore = FakeRunnerSyncStateStore(
            state = SyncReadState(
                cursor = "changes:1000",
                lastBootstrapAt = null,
                lastChangesAt = null,
                lastSuccessfulReadAt = null,
                requiresBootstrap = false,
                configuredBaseUrl = "http://localhost:8080",
                configuredAccessToken = "token",
                lastForegroundSyncStartedAt = LocalDateTime.now(),
            ),
        )
        val runner = AppSyncRunner(useCase, stateStore)

        runner.triggerForegroundSync()
        delay(100)

        assertEquals(0, useCase.calls)
    }

    @Test
    fun manualSync_savesConfig_andRunsImmediately() = runBlocking {
        val useCase = FakeRunFullSyncUseCase()
        val stateStore = FakeRunnerSyncStateStore()
        val runner = AppSyncRunner(useCase, stateStore)

        runner.triggerManualSync("http://localhost:8080", "token")
        delay(100)

        assertEquals(1, useCase.calls)
        assertEquals("http://localhost:8080", stateStore.state.configuredBaseUrl)
        assertEquals("token", stateStore.state.configuredAccessToken)
    }
}

private class FakeRunFullSyncUseCase : FullSyncRunner {
    var calls = 0

    override suspend fun run(baseUrl: String, accessToken: String): SyncRunResult {
        calls += 1
        return SyncRunResult.Success(
            bootstrapPerformed = false,
            pushPerformed = false,
            changesPerformed = true,
        )
    }
}

private class FakeRunnerSyncStateStore(
    var state: SyncReadState = SyncReadState(
        cursor = null,
        lastBootstrapAt = null,
        lastChangesAt = null,
        lastSuccessfulReadAt = null,
        requiresBootstrap = false,
        configuredBaseUrl = null,
        configuredAccessToken = null,
        lastForegroundSyncStartedAt = null,
    ),
) : SyncStateStore {
    override suspend fun get(): SyncReadState = state

    override suspend fun saveBootstrapCursor(cursor: String, timestamp: LocalDateTime) {
        state = state.copy(cursor = cursor, lastBootstrapAt = timestamp, lastSuccessfulReadAt = timestamp, requiresBootstrap = false)
    }

    override suspend fun saveChangesCursor(cursor: String, timestamp: LocalDateTime) {
        state = state.copy(cursor = cursor, lastChangesAt = timestamp, lastSuccessfulReadAt = timestamp, requiresBootstrap = false)
    }

    override suspend fun setRequiresBootstrap(value: Boolean) {
        state = state.copy(requiresBootstrap = value)
    }

    override suspend fun saveSyncConfig(baseUrl: String, accessToken: String) {
        state = state.copy(configuredBaseUrl = baseUrl, configuredAccessToken = accessToken)
    }

    override suspend fun markForegroundSyncStarted(timestamp: LocalDateTime) {
        state = state.copy(lastForegroundSyncStartedAt = timestamp)
    }
}
