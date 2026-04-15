package ru.ekrupin.ivi.data.auth.session

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import ru.ekrupin.ivi.data.auth.remote.AuthRemoteDataSource
import ru.ekrupin.ivi.data.sync.config.SyncSession
import ru.ekrupin.ivi.data.sync.config.SyncSessionStore
import ru.ekrupin.ivi.data.sync.remote.SyncHttpException

@Singleton
class AuthSessionManager @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val sessionStore: SyncSessionStore,
) {
    val session: Flow<SyncSession> = sessionStore.session

    suspend fun getSession(): SyncSession = sessionStore.get()

    suspend fun register(baseUrl: String, email: String, password: String, displayName: String): AuthSessionResult {
        if (baseUrl.isBlank() || email.isBlank() || password.isBlank() || displayName.isBlank()) {
            return AuthSessionResult.ValidationError("Заполните адрес backend, email, пароль и имя")
        }

        return try {
            val response = authRemoteDataSource.register(baseUrl, email, password, displayName)
            sessionStore.saveAuthorizedSession(
                baseUrl = baseUrl,
                accessToken = response.tokens.accessToken,
                refreshToken = response.tokens.refreshToken,
                userId = response.user.id,
                email = response.user.email,
                displayName = response.user.displayName,
            )
            AuthSessionResult.Success(response.user.email, response.user.displayName)
        } catch (exception: SyncHttpException) {
            exception.toAuthResult(isLogin = false)
        } catch (exception: IOException) {
            AuthSessionResult.NetworkError(exception.message ?: "Ошибка сети")
        } catch (exception: Exception) {
            AuthSessionResult.UnknownError(exception.message ?: "Неизвестная ошибка")
        }
    }

    suspend fun login(baseUrl: String, email: String, password: String): AuthSessionResult {
        if (baseUrl.isBlank() || email.isBlank() || password.isBlank()) {
            return AuthSessionResult.ValidationError("Заполните адрес backend, email и пароль")
        }

        return try {
            val response = authRemoteDataSource.login(baseUrl, email, password)
            sessionStore.saveAuthorizedSession(
                baseUrl = baseUrl,
                accessToken = response.tokens.accessToken,
                refreshToken = response.tokens.refreshToken,
                userId = response.user.id,
                email = response.user.email,
                displayName = response.user.displayName,
            )
            AuthSessionResult.Success(response.user.email, response.user.displayName)
        } catch (exception: SyncHttpException) {
            exception.toAuthResult(isLogin = true)
        } catch (exception: IOException) {
            AuthSessionResult.NetworkError(exception.message ?: "Ошибка сети")
        } catch (exception: Exception) {
            AuthSessionResult.UnknownError(exception.message ?: "Неизвестная ошибка")
        }
    }

    suspend fun refreshSession(): Boolean {
        val session = sessionStore.get()
        if (!session.isConfigured || session.refreshToken.isBlank()) return false

        return try {
            val response = authRemoteDataSource.refresh(session.baseUrl, session.refreshToken)
            sessionStore.saveAuthorizedSession(
                baseUrl = session.baseUrl,
                accessToken = response.tokens.accessToken,
                refreshToken = response.tokens.refreshToken,
                userId = response.user.id,
                email = response.user.email,
                displayName = response.user.displayName,
            )
            true
        } catch (_: Exception) {
            sessionStore.clear()
            false
        }
    }

    suspend fun refreshCurrentUser(): AuthSessionResult {
        val session = sessionStore.get()
        if (!session.isAuthenticated) return AuthSessionResult.NotConnected

        return try {
            val me = authRemoteDataSource.me(session.baseUrl, session.accessToken)
            sessionStore.saveAuthorizedSession(
                baseUrl = session.baseUrl,
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                userId = me.user.id,
                email = me.user.email,
                displayName = me.user.displayName,
            )
            AuthSessionResult.Success(me.user.email, me.user.displayName)
        } catch (exception: SyncHttpException) {
            exception.toAuthResult(isLogin = true)
        } catch (exception: IOException) {
            AuthSessionResult.NetworkError(exception.message ?: "Ошибка сети")
        } catch (exception: Exception) {
            AuthSessionResult.UnknownError(exception.message ?: "Неизвестная ошибка")
        }
    }

    suspend fun logout() {
        sessionStore.clear()
    }

    private fun SyncHttpException.toAuthResult(isLogin: Boolean): AuthSessionResult = when (code) {
        401 -> AuthSessionResult.InvalidCredentials
        409 -> if (isLogin) AuthSessionResult.AuthError("Сессия недействительна") else AuthSessionResult.AuthError("Пользователь с таким email уже существует")
        in 500..599 -> AuthSessionResult.ServerError(code)
        else -> AuthSessionResult.AuthError(message ?: "Ошибка авторизации")
    }
}

sealed interface AuthSessionResult {
    data class Success(val email: String, val displayName: String) : AuthSessionResult
    data object NotConnected : AuthSessionResult
    data object InvalidCredentials : AuthSessionResult
    data class ValidationError(val message: String) : AuthSessionResult
    data class NetworkError(val message: String) : AuthSessionResult
    data class ServerError(val code: Int) : AuthSessionResult
    data class AuthError(val message: String) : AuthSessionResult
    data class UnknownError(val message: String) : AuthSessionResult
}
