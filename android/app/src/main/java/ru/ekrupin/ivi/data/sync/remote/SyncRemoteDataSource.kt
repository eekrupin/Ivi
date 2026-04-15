package ru.ekrupin.ivi.data.sync.remote

interface SyncRemoteDataSource {
    suspend fun bootstrap(baseUrl: String, accessToken: String): RemoteBootstrapResponse
    suspend fun changes(baseUrl: String, accessToken: String, cursor: String): RemoteChangesResponse
    suspend fun push(baseUrl: String, accessToken: String, request: RemotePushRequest): RemotePushResponse
}
