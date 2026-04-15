package ru.ekrupin.ivi.data.sync.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.ekrupin.ivi.data.sync.SyncStateStore

data class SyncConfig(
    val baseUrl: String,
    val accessToken: String,
) {
    val isConfigured: Boolean = baseUrl.isNotBlank() && accessToken.isNotBlank()
}

interface SyncConfigStore {
    val config: Flow<SyncConfig>
    suspend fun get(): SyncConfig
    suspend fun save(baseUrl: String, accessToken: String)
    suspend fun clear()
}

@Singleton
class DataStoreSyncConfigStore @Inject constructor(
    private val appContext: Context,
    private val syncStateStore: SyncStateStore,
) : SyncConfigStore {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { appContext.preferencesDataStoreFile("sync_config.preferences_pb") },
    )

    override val config: Flow<SyncConfig> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            SyncConfig(
                baseUrl = preferences[Keys.BASE_URL].orEmpty(),
                accessToken = preferences[Keys.ACCESS_TOKEN].orEmpty(),
            )
        }

    override suspend fun get(): SyncConfig {
        migrateLegacyIfNeeded()
        return config.first()
    }

    override suspend fun save(baseUrl: String, accessToken: String) {
        dataStore.edit { preferences ->
            preferences[Keys.BASE_URL] = baseUrl.trim()
            preferences[Keys.ACCESS_TOKEN] = accessToken.trim()
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.BASE_URL)
            preferences.remove(Keys.ACCESS_TOKEN)
        }
    }

    private suspend fun migrateLegacyIfNeeded() {
        val current = config.first()
        if (current.isConfigured) return

        val legacy = syncStateStore.get()
        val baseUrl = legacy.configuredBaseUrl?.trim().orEmpty()
        val accessToken = legacy.configuredAccessToken?.trim().orEmpty()
        if (baseUrl.isBlank() || accessToken.isBlank()) return

        save(baseUrl, accessToken)
    }

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
    }
}
