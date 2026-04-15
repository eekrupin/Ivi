package ru.ekrupin.ivi.data.sync

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import ru.ekrupin.ivi.data.sync.config.SyncConfig
import ru.ekrupin.ivi.data.sync.config.SyncConfigStore

class AppSyncRunnerTest {
    @Test
    fun foregroundSync_doesNotRunWithoutConfig() = runBlocking {
        val useCase = FakeRunFullSyncUseCase()
        val stateStore = FakeRunnerSyncStateStore()
        val configStore = FakeSyncConfigStore()
        val runner = AppSyncRunner(useCase, stateStore, configStore, SyncExecutionGate())

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
                configuredBaseUrl = null,
                configuredAccessToken = null,
                lastForegroundSyncStartedAt = null,
            ),
        )
        val configStore = FakeSyncConfigStore(SyncConfig("http://localhost:8080", "token"))
        val runner = AppSyncRunner(useCase, stateStore, configStore, SyncExecutionGate())

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
                configuredBaseUrl = null,
                configuredAccessToken = null,
                lastForegroundSyncStartedAt = LocalDateTime.now(),
            ),
        )
        val configStore = FakeSyncConfigStore(SyncConfig("http://localhost:8080", "token"))
        val runner = AppSyncRunner(useCase, stateStore, configStore, SyncExecutionGate())

        runner.triggerForegroundSync()
        delay(100)

        assertEquals(0, useCase.calls)
    }

    @Test
    fun manualSync_savesConfig_andRunsImmediately() = runBlocking {
        val useCase = FakeRunFullSyncUseCase()
        val stateStore = FakeRunnerSyncStateStore()
        val configStore = FakeSyncConfigStore()
        val runner = AppSyncRunner(useCase, stateStore, configStore, SyncExecutionGate())

        runner.triggerManualSync("http://localhost:8080", "token")
        delay(100)

        assertEquals(1, useCase.calls)
        assertEquals("http://localhost:8080", configStore.current.baseUrl)
        assertEquals("token", configStore.current.accessToken)
    }
}

private class FakeSyncConfigStore(
    initial: SyncConfig = SyncConfig(baseUrl = "", accessToken = ""),
) : SyncConfigStore {
    var current: SyncConfig = initial
    override val config = kotlinx.coroutines.flow.MutableStateFlow(initial)

    override suspend fun get(): SyncConfig = current

    override suspend fun save(baseUrl: String, accessToken: String) {
        current = SyncConfig(baseUrl, accessToken)
        config.value = current
    }

    override suspend fun clear() {
        current = SyncConfig("", "")
        config.value = current
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
