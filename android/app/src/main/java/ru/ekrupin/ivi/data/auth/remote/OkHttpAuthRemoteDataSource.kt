package ru.ekrupin.ivi.data.auth.remote

import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import ru.ekrupin.ivi.data.sync.remote.SyncHttpException

class OkHttpAuthRemoteDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
) : AuthRemoteDataSource {
    override suspend fun register(baseUrl: String, email: String, password: String, displayName: String): RemoteAuthResult =
        executeJsonPost(
            url = "${baseUrl.trimEnd('/')}/v1/auth/register",
            body = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
                put("displayName", displayName.trim())
            }.toString(),
        ).toAuthResult()

    override suspend fun login(baseUrl: String, email: String, password: String): RemoteAuthResult =
        executeJsonPost(
            url = "${baseUrl.trimEnd('/')}/v1/auth/login",
            body = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }.toString(),
        ).toAuthResult()

    override suspend fun refresh(baseUrl: String, refreshToken: String): RemoteAuthResult =
        executeJsonPost(
            url = "${baseUrl.trimEnd('/')}/v1/auth/refresh",
            body = JSONObject().apply {
                put("refreshToken", refreshToken)
            }.toString(),
        ).toAuthResult()

    override suspend fun me(baseUrl: String, accessToken: String): RemoteMeResult =
        executeJsonGet(
            url = "${baseUrl.trimEnd('/')}/v1/me",
            accessToken = accessToken,
        ).toMeResult()

    private suspend fun executeJsonPost(url: String, body: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw SyncHttpException(response.code, "Auth request failed: HTTP ${response.code} $responseBody")
            }
            JSONObject(responseBody)
        }
    }

    private suspend fun executeJsonGet(url: String, accessToken: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw SyncHttpException(response.code, "Auth request failed: HTTP ${response.code} $responseBody")
            }
            JSONObject(responseBody)
        }
    }

    private fun JSONObject.toAuthResult(): RemoteAuthResult {
        val userJson = getJSONObject("user")
        val tokensJson = getJSONObject("tokens")
        return RemoteAuthResult(
            user = RemoteAuthUser(
                id = userJson.getString("id"),
                email = userJson.getString("email"),
                displayName = userJson.getString("displayName"),
            ),
            tokens = RemoteAuthTokens(
                accessToken = tokensJson.getString("accessToken"),
                refreshToken = tokensJson.getString("refreshToken"),
            ),
        )
    }

    private fun JSONObject.toMeResult(): RemoteMeResult {
        val userJson = getJSONObject("user")
        return RemoteMeResult(
            user = RemoteAuthUser(
                id = userJson.getString("id"),
                email = userJson.getString("email"),
                displayName = userJson.getString("displayName"),
            ),
        )
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
