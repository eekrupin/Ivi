package ru.ekrupin.ivi.data.auth.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ekrupin.ivi.data.auth.remote.AuthRemoteDataSource
import ru.ekrupin.ivi.data.auth.remote.RemoteAuthResult
import ru.ekrupin.ivi.data.auth.remote.RemoteAuthTokens
import ru.ekrupin.ivi.data.auth.remote.RemoteAuthUser
import ru.ekrupin.ivi.data.auth.remote.RemoteMeResult
import ru.ekrupin.ivi.data.sync.config.SyncSession
import ru.ekrupin.ivi.data.sync.config.SyncSessionStore
import ru.ekrupin.ivi.data.sync.remote.SyncHttpException

class AuthSessionManagerTest {
    @Test
    fun loginSuccess_savesAuthorizedSession() = runBlocking {
        val store = FakeSessionStore()
        val manager = AuthSessionManager(SuccessAuthRemoteDataSource(), store)

        val result = manager.login("http://localhost:8080", "user@example.com", "secret123")

        assertTrue(result is AuthSessionResult.Success)
        assertEquals("http://localhost:8080", store.current.baseUrl)
        assertEquals("access-token", store.current.accessToken)
        assertEquals("refresh-token", store.current.refreshToken)
        assertTrue(store.current.isAuthenticated)
    }

    @Test
    fun loginFailure_returnsInvalidCredentials() = runBlocking {
        val store = FakeSessionStore()
        val manager = AuthSessionManager(FailingAuthRemoteDataSource(401), store)

        val result = manager.login("http://localhost:8080", "user@example.com", "wrong")

        assertEquals(AuthSessionResult.InvalidCredentials, result)
        assertTrue(!store.current.isAuthenticated)
    }

    @Test
    fun logout_clearsSession() = runBlocking {
        val store = FakeSessionStore(
            SyncSession(
                baseUrl = "http://localhost:8080",
                accessToken = "access-token",
                refreshToken = "refresh-token",
                userId = "1",
                email = "user@example.com",
                displayName = "User",
            ),
        )
        val manager = AuthSessionManager(SuccessAuthRemoteDataSource(), store)

        manager.logout()

        assertEquals("", store.current.baseUrl)
        assertEquals("", store.current.accessToken)
        assertEquals("", store.current.refreshToken)
        assertTrue(!store.current.isAuthenticated)
    }
}

private class FakeSessionStore(
    initial: SyncSession = SyncSession("", "", "", null, null, null),
) : SyncSessionStore {
    var current: SyncSession = initial
    override val session = MutableStateFlow(initial)

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

private class SuccessAuthRemoteDataSource : AuthRemoteDataSource {
    override suspend fun register(baseUrl: String, email: String, password: String, displayName: String): RemoteAuthResult =
        authResult(email, displayName)

    override suspend fun login(baseUrl: String, email: String, password: String): RemoteAuthResult =
        authResult(email, "User")

    override suspend fun refresh(baseUrl: String, refreshToken: String): RemoteAuthResult =
        authResult("user@example.com", "User")

    override suspend fun me(baseUrl: String, accessToken: String): RemoteMeResult =
        RemoteMeResult(RemoteAuthUser("1", "user@example.com", "User"))

    private fun authResult(email: String, displayName: String) = RemoteAuthResult(
        user = RemoteAuthUser("1", email, displayName),
        tokens = RemoteAuthTokens("access-token", "refresh-token"),
    )
}

private class FailingAuthRemoteDataSource(
    private val code: Int,
) : AuthRemoteDataSource {
    override suspend fun register(baseUrl: String, email: String, password: String, displayName: String): RemoteAuthResult = fail()
    override suspend fun login(baseUrl: String, email: String, password: String): RemoteAuthResult = fail()
    override suspend fun refresh(baseUrl: String, refreshToken: String): RemoteAuthResult = fail()
    override suspend fun me(baseUrl: String, accessToken: String): RemoteMeResult = throw SyncHttpException(code, "error")

    private fun fail(): Nothing = throw SyncHttpException(code, "error")
}
