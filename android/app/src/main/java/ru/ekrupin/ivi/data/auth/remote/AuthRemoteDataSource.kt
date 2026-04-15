package ru.ekrupin.ivi.data.auth.remote

interface AuthRemoteDataSource {
    suspend fun register(baseUrl: String, email: String, password: String, displayName: String): RemoteAuthResult
    suspend fun login(baseUrl: String, email: String, password: String): RemoteAuthResult
    suspend fun refresh(baseUrl: String, refreshToken: String): RemoteAuthResult
    suspend fun me(baseUrl: String, accessToken: String): RemoteMeResult
}
