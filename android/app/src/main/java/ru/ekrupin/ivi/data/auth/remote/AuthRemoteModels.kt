package ru.ekrupin.ivi.data.auth.remote

data class RemoteAuthUser(
    val id: String,
    val email: String,
    val displayName: String,
)

data class RemoteAuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

data class RemoteAuthResult(
    val user: RemoteAuthUser,
    val tokens: RemoteAuthTokens,
)

data class RemoteMeResult(
    val user: RemoteAuthUser,
)
