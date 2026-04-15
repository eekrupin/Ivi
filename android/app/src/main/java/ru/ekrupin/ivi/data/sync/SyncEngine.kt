package ru.ekrupin.ivi.data.sync

interface SyncEngine {
    suspend fun bootstrapImport(baseUrl: String, accessToken: String)
    suspend fun pullChanges(baseUrl: String, accessToken: String)
    suspend fun drainOutbox(baseUrl: String, accessToken: String, deviceId: String, limit: Int = 50): PushDrainResult
}
