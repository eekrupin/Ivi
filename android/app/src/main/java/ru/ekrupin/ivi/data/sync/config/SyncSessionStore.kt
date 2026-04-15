package ru.ekrupin.ivi.data.sync.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.ekrupin.ivi.data.sync.SyncStateStore

data class SyncSession(
    val baseUrl: String,
    val accessToken: String,
    val refreshToken: String,
    val userId: String?,
    val email: String?,
    val displayName: String?,
) {
    val isConfigured: Boolean = baseUrl.isNotBlank()
    val isAuthenticated: Boolean = baseUrl.isNotBlank() && accessToken.isNotBlank()
}

interface SyncSessionStore {
    val session: Flow<SyncSession>
    suspend fun get(): SyncSession
    suspend fun saveAuthorizedSession(
        baseUrl: String,
        accessToken: String,
        refreshToken: String,
        userId: String?,
        email: String?,
        displayName: String?,
    )

    suspend fun updateBaseUrl(baseUrl: String)
    suspend fun updateTokens(accessToken: String, refreshToken: String)
    suspend fun clear()
}

@Singleton
class DataStoreSyncSessionStore @Inject constructor(
    private val appContext: Context,
    private val syncStateStore: SyncStateStore,
) : SyncSessionStore {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { appContext.preferencesDataStoreFile("sync_session.preferences_pb") },
    )

    override val session: Flow<SyncSession> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            SyncSession(
                baseUrl = preferences[Keys.BASE_URL].orEmpty(),
                accessToken = preferences[Keys.ACCESS_TOKEN].orEmpty(),
                refreshToken = preferences[Keys.REFRESH_TOKEN].orEmpty(),
                userId = preferences[Keys.USER_ID],
                email = preferences[Keys.EMAIL],
                displayName = preferences[Keys.DISPLAY_NAME],
            )
        }

    override suspend fun get(): SyncSession {
        migrateLegacyIfNeeded()
        return session.first()
    }

    override suspend fun saveAuthorizedSession(
        baseUrl: String,
        accessToken: String,
        refreshToken: String,
        userId: String?,
        email: String?,
        displayName: String?,
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.BASE_URL] = baseUrl.trim()
            preferences[Keys.ACCESS_TOKEN] = accessToken.trim()
            preferences[Keys.REFRESH_TOKEN] = refreshToken.trim()
            setOrRemove(preferences, Keys.USER_ID, userId)
            setOrRemove(preferences, Keys.EMAIL, email)
            setOrRemove(preferences, Keys.DISPLAY_NAME, displayName)
        }
    }

    override suspend fun updateBaseUrl(baseUrl: String) {
        dataStore.edit { preferences ->
            preferences[Keys.BASE_URL] = baseUrl.trim()
        }
    }

    override suspend fun updateTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { preferences ->
            preferences[Keys.ACCESS_TOKEN] = accessToken.trim()
            preferences[Keys.REFRESH_TOKEN] = refreshToken.trim()
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private suspend fun migrateLegacyIfNeeded() {
        val current = session.first()
        if (current.isConfigured) return

        val legacy = syncStateStore.get()
        val baseUrl = legacy.configuredBaseUrl?.trim().orEmpty()
        val accessToken = legacy.configuredAccessToken?.trim().orEmpty()
        if (baseUrl.isBlank()) return

        saveAuthorizedSession(
            baseUrl = baseUrl,
            accessToken = accessToken,
            refreshToken = "",
            userId = null,
            email = null,
            displayName = null,
        )
    }

    private fun setOrRemove(preferences: androidx.datastore.preferences.core.MutablePreferences, key: Preferences.Key<String>, value: String?) {
        if (value.isNullOrBlank()) preferences.remove(key) else preferences[key] = value
    }

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
    }
}
