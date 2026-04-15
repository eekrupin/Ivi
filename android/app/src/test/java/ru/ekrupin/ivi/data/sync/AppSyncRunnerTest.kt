package ru.ekrupin.ivi.data.sync

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import ru.ekrupin.ivi.data.auth.remote.AuthRemoteDataSource
import ru.ekrupin.ivi.data.auth.remote.RemoteAuthResult
import ru.ekrupin.ivi.data.auth.remote.RemoteAuthTokens
import ru.ekrupin.ivi.data.auth.remote.RemoteAuthUser
import ru.ekrupin.ivi.data.auth.remote.RemoteMeResult
import ru.ekrupin.ivi.data.auth.session.AuthSessionManager
import ru.ekrupin.ivi.data.sync.config.SyncSession
import ru.ekrupin.ivi.data.sync.config.SyncSessionStore

class AppSyncRunnerTest {
    @Test
    fun foregroundSync_doesNotRunWithoutConfig() = runBlocking {
        val useCase = FakeRunFullSyncUseCase()
        val stateStore = FakeRunnerSyncStateStore()
        val sessionStore = FakeSyncSessionStore()
        val authManager = AuthSessionManager(FakeAuthRemoteDataSource(), sessionStore)
        val runner = AppSyncRunner(AuthorizedSyncRunner(useCase, authManager), stateStore, authManager, SyncExecutionGate())

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
        val sessionStore = FakeSyncSessionStore(
            SyncSession(
                baseUrl = "http://localhost:8080",
                accessToken = "token",
                refreshToken = "refresh",
                userId = "1",
                email = "user@example.com",
                displayName = "User",
            ),
        )
        val authManager = AuthSessionManager(FakeAuthRemoteDataSource(), sessionStore)
        val runner = AppSyncRunner(AuthorizedSyncRunner(useCase, authManager), stateStore, authManager, SyncExecutionGate())

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
        val sessionStore = FakeSyncSessionStore(
            SyncSession(
                baseUrl = "http://localhost:8080",
                accessToken = "token",
                refreshToken = "refresh",
                userId = "1",
                email = "user@example.com",
                displayName = "User",
            ),
        )
        val authManager = AuthSessionManager(FakeAuthRemoteDataSource(), sessionStore)
        val runner = AppSyncRunner(AuthorizedSyncRunner(useCase, authManager), stateStore, authManager, SyncExecutionGate())

        runner.triggerForegroundSync()
        delay(100)

        assertEquals(0, useCase.calls)
    }

    @Test
    fun manualSync_runsImmediatelyWhenSessionExists() = runBlocking {
        val useCase = FakeRunFullSyncUseCase()
        val stateStore = FakeRunnerSyncStateStore()
        val sessionStore = FakeSyncSessionStore(
            SyncSession(
                baseUrl = "http://localhost:8080",
                accessToken = "token",
                refreshToken = "refresh",
                userId = "1",
                email = "user@example.com",
                displayName = "User",
            ),
        )
        val authManager = AuthSessionManager(FakeAuthRemoteDataSource(), sessionStore)
        val runner = AppSyncRunner(AuthorizedSyncRunner(useCase, authManager), stateStore, authManager, SyncExecutionGate())

        runner.triggerManualSync()
        delay(100)

        assertEquals(1, useCase.calls)
        assertTrue(runner.status.value is AppSyncStatus.Success)
    }
}

private class FakeSyncSessionStore(
    initial: SyncSession = SyncSession("", "", "", null, null, null),
) : SyncSessionStore {
    var current: SyncSession = initial
    override val session = kotlinx.coroutines.flow.MutableStateFlow(initial)

    override suspend fun get(): SyncSession = current

    override suspend fun saveAuthorizedSession(
        baseUrl: String,
        accessToken: String,
        refreshToken: String,
        userId: String?,
        email: String?,
        displayName: String?,
    ) {
        current = SyncSession(baseUrl, accessToken, refreshToken, userId, email, displayName)
        session.value = current
    }

    override suspend fun updateBaseUrl(baseUrl: String) {
        current = current.copy(baseUrl = baseUrl)
        session.value = current
    }

    override suspend fun updateTokens(accessToken: String, refreshToken: String) {
        current = current.copy(accessToken = accessToken, refreshToken = refreshToken)
        session.value = current
    }

    override suspend fun clear() {
        current = SyncSession("", "", "", null, null, null)
        session.value = current
    }
}

private class FakeAuthRemoteDataSource : AuthRemoteDataSource {
    override suspend fun register(baseUrl: String, email: String, password: String, displayName: String): RemoteAuthResult =
        RemoteAuthResult(RemoteAuthUser("1", email, displayName), RemoteAuthTokens("token", "refresh"))

    override suspend fun login(baseUrl: String, email: String, password: String): RemoteAuthResult =
        RemoteAuthResult(RemoteAuthUser("1", email, "User"), RemoteAuthTokens("token", "refresh"))

    override suspend fun refresh(baseUrl: String, refreshToken: String): RemoteAuthResult =
        RemoteAuthResult(RemoteAuthUser("1", "user@example.com", "User"), RemoteAuthTokens("token", "refresh"))

    override suspend fun me(baseUrl: String, accessToken: String): RemoteMeResult =
        RemoteMeResult(RemoteAuthUser("1", "user@example.com", "User"))
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
        Unit
    }

    override suspend fun markForegroundSyncStarted(timestamp: LocalDateTime) {
        state = state.copy(lastForegroundSyncStartedAt = timestamp)
    }
}
