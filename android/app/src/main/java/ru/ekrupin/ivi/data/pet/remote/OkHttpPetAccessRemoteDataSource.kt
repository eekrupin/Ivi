package ru.ekrupin.ivi.data.pet.remote

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import ru.ekrupin.ivi.data.sync.remote.SyncHttpException

class OkHttpPetAccessRemoteDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
) : PetAccessRemoteDataSource {
    override suspend fun getCurrentPet(baseUrl: String, accessToken: String): RemotePetAccessPet =
        getCurrentPetAccess(baseUrl, accessToken).pet

    override suspend fun getCurrentPetAccess(baseUrl: String, accessToken: String): RemotePetAccessContext =
        executeJsonGet(
            url = "${baseUrl.trimEnd('/')}/v1/pets/current",
            accessToken = accessToken,
        ).toPetAccessContext()

    override suspend fun getCurrentPetLeaveOptions(baseUrl: String, accessToken: String): RemoteLeavePetOptions =
        executeJsonGet(
            url = "${baseUrl.trimEnd('/')}/v1/pets/current/leave-options",
            accessToken = accessToken,
        ).toLeavePetOptions()

    override suspend fun createPet(baseUrl: String, accessToken: String, name: String, birthDate: LocalDate?): RemotePetAccessPet =
        executeJsonPost(
            url = "${baseUrl.trimEnd('/')}/v1/pets",
            accessToken = accessToken,
            body = JSONObject().apply {
                put("name", name.trim())
                if (birthDate != null) put("birthDate", birthDate.toString())
            }.toString(),
        ).getJSONObject("pet").toPet()

    override suspend fun createInvite(baseUrl: String, accessToken: String, petId: String): RemotePetInvite =
        executeJsonPost(
            url = "${baseUrl.trimEnd('/')}/v1/pets/$petId/invites",
            accessToken = accessToken,
            body = JSONObject().put("expiresInHours", 72).toString(),
        ).getJSONObject("invite").let { invite ->
            RemotePetInvite(
                code = invite.getString("code"),
                petId = invite.getString("petId"),
            )
        }

    override suspend fun acceptInvite(baseUrl: String, accessToken: String, code: String): RemotePetAccessContext =
        executeJsonPost(
            url = "${baseUrl.trimEnd('/')}/v1/invites/accept",
            accessToken = accessToken,
            body = JSONObject().put("code", code.trim()).toString(),
        ).toPetAccessContext()

    override suspend fun leaveCurrentPet(
        baseUrl: String,
        accessToken: String,
        transferOwnerToUserId: String?,
        deletePet: Boolean,
    ): RemotePetMembership =
        executeJsonPost(
            url = "${baseUrl.trimEnd('/')}/v1/pets/current/leave",
            accessToken = accessToken,
            body = JSONObject().apply {
                if (transferOwnerToUserId != null) put("transferOwnerToUserId", transferOwnerToUserId)
                if (deletePet) put("deletePet", true)
            }.toString(),
        ).getJSONObject("membership").toMembership()

    private suspend fun executeJsonGet(url: String, accessToken: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw SyncHttpException(response.code, "Pet request failed: HTTP ${response.code} $responseBody")
            }
            JSONObject(responseBody)
        }
    }

    private suspend fun executeJsonPost(url: String, accessToken: String, body: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw SyncHttpException(response.code, "Pet request failed: HTTP ${response.code} $responseBody")
            }
            JSONObject(responseBody)
        }
    }

    private fun JSONObject.toPet(): RemotePetAccessPet = RemotePetAccessPet(
        id = getString("id"),
        version = getLong("version"),
        updatedAt = getString("updatedAt").toLocalDateTimeUtc(),
        name = getString("name"),
        birthDate = optString("birthDate").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
        createdAt = getString("createdAt").toLocalDateTimeUtc(),
    )

    private fun JSONObject.toPetAccessContext(): RemotePetAccessContext = RemotePetAccessContext(
        pet = getJSONObject("pet").toPet(),
        membership = getJSONObject("membership").toMembership(),
    )

    private fun JSONObject.toLeavePetOptions(): RemoteLeavePetOptions = RemoteLeavePetOptions(
        pet = getJSONObject("pet").toPet(),
        membership = getJSONObject("membership").toMembership(),
        transferCandidates = getJSONArray("transferCandidates").toUserProfiles(),
        canDeletePet = getBoolean("canDeletePet"),
    )

    private fun JSONArray.toUserProfiles(): List<RemotePetUserProfile> = buildList {
        for (index in 0 until length()) {
            add(getJSONObject(index).toUserProfile())
        }
    }

    private fun JSONObject.toUserProfile(): RemotePetUserProfile = RemotePetUserProfile(
        id = getString("id"),
        email = getString("email"),
        displayName = optString("displayName").takeIf { it.isNotBlank() },
    )

    private fun JSONObject.toMembership(): RemotePetMembership = RemotePetMembership(
        petId = optString("petId"),
        role = getString("role"),
    )

    private fun String.toLocalDateTimeUtc(): LocalDateTime = OffsetDateTime.parse(this).toLocalDateTime()

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
