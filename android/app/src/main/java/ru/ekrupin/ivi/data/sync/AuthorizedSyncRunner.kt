package ru.ekrupin.ivi.data.sync

import javax.inject.Inject
import javax.inject.Singleton
import ru.ekrupin.ivi.data.auth.session.AuthSessionManager
import ru.ekrupin.ivi.data.auth.session.AuthSessionResult

@Singleton
class AuthorizedSyncRunner @Inject constructor(
    private val fullSyncRunner: FullSyncRunner,
    private val authSessionManager: AuthSessionManager,
) {
    suspend fun runWithSession(): SyncRunResult {
        val session = authSessionManager.getSession()
        if (!session.isAuthenticated) {
            return SyncRunResult.AuthError
        }

        val firstResult = fullSyncRunner.run(session.baseUrl, session.accessToken)
        if (firstResult != SyncRunResult.AuthError || session.refreshToken.isBlank()) {
            return firstResult
        }

        val refreshed = authSessionManager.refreshSession()
        if (!refreshed) {
            return SyncRunResult.AuthError
        }

        val refreshedSession = authSessionManager.getSession()
        if (!refreshedSession.isAuthenticated) {
            return SyncRunResult.AuthError
        }

        return fullSyncRunner.run(refreshedSession.baseUrl, refreshedSession.accessToken)
    }
}
