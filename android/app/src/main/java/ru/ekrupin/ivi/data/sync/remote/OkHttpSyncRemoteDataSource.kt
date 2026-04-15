package ru.ekrupin.ivi.data.sync.remote

import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class OkHttpSyncRemoteDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
) : SyncRemoteDataSource {
    override suspend fun bootstrap(baseUrl: String, accessToken: String): RemoteBootstrapResponse =
        executeJsonGet(
            url = "${baseUrl.trimEnd('/')}/v1/sync/bootstrap",
            accessToken = accessToken,
        ).toBootstrapResponse()

    override suspend fun changes(baseUrl: String, accessToken: String, cursor: String): RemoteChangesResponse =
        executeJsonGet(
            url = "${baseUrl.trimEnd('/')}/v1/sync/changes?cursor=${java.net.URLEncoder.encode(cursor, Charsets.UTF_8.name())}",
            accessToken = accessToken,
        ).toChangesResponse()

    override suspend fun push(baseUrl: String, accessToken: String, request: RemotePushRequest): RemotePushResponse =
        executeJsonPost(
            url = "${baseUrl.trimEnd('/')}/v1/sync/push",
            accessToken = accessToken,
            body = request.toJsonBody(),
        ).toPushResponse()

    private suspend fun executeJsonGet(url: String, accessToken: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Sync request failed: HTTP ${response.code} $body")
            }
            JSONObject(body)
        }
    }

    private suspend fun executeJsonPost(url: String, accessToken: String, body: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Sync request failed: HTTP ${response.code} $responseBody")
            }
            JSONObject(responseBody)
        }
    }

    private fun JSONObject.toBootstrapResponse(): RemoteBootstrapResponse = RemoteBootstrapResponse(
        cursor = getString("cursor"),
        snapshot = getJSONObject("snapshot").toBootstrapSnapshot(),
    )

    private fun JSONObject.toChangesResponse(): RemoteChangesResponse = RemoteChangesResponse(
        cursor = getString("cursor"),
        hasMore = optBoolean("hasMore", false),
        changes = getJSONObject("changes").toChangesPayload(),
        tombstones = optJSONArray("tombstones").toTombstones(),
    )

    private fun JSONObject.toPushResponse(): RemotePushResponse = RemotePushResponse(
        accepted = optJSONArray("accepted").toAcceptedMutations(),
        conflicts = optJSONArray("conflicts").toConflicts(),
        cursor = getString("cursor"),
        requiresBootstrap = getBoolean("requiresBootstrap"),
    )

    private fun JSONObject.toBootstrapSnapshot(): RemoteBootstrapSnapshot = RemoteBootstrapSnapshot(
        users = optJSONArray("users").toUsers(),
        pets = optJSONArray("pets").toPets(),
        memberships = optJSONArray("memberships").toMemberships(),
        eventTypes = optJSONArray("eventTypes").toEventTypes(),
        petEvents = optJSONArray("petEvents").toPetEvents(),
        weightEntries = optJSONArray("weightEntries").toWeightEntries(),
    )

    private fun JSONObject.toChangesPayload(): RemoteChangesPayload = RemoteChangesPayload(
        users = optJSONArray("users").toUsers(),
        pets = optJSONArray("pets").toPets(),
        memberships = optJSONArray("memberships").toMemberships(),
        eventTypes = optJSONArray("eventTypes").toEventTypes(),
        petEvents = optJSONArray("petEvents").toPetEvents(),
        weightEntries = optJSONArray("weightEntries").toWeightEntries(),
    )

    private fun JSONArray?.toUsers(): List<RemoteSyncUser> = this.toObjects { item ->
        RemoteSyncUser(
            remoteId = item.getString("id"),
            email = item.getString("email"),
            displayName = item.getString("displayName"),
            serverVersion = item.optLong("version", 1),
            serverUpdatedAt = item.getString("updatedAt").toLocalDateTimeUtc(),
            deletedAt = item.optStringOrNull("deletedAt")?.toLocalDateTimeUtc(),
            createdAt = item.getString("createdAt").toLocalDateTimeUtc(),
        )
    }

    private fun JSONArray?.toPets(): List<RemoteSyncPet> = this.toObjects { item ->
        RemoteSyncPet(
            remoteId = item.getString("id"),
            name = item.getString("name"),
            birthDate = item.optStringOrNull("birthDate")?.let(LocalDate::parse),
            photoRevision = item.optStringOrNull("photoRevision"),
            serverVersion = item.getLong("version"),
            serverUpdatedAt = item.getString("updatedAt").toLocalDateTimeUtc(),
            deletedAt = item.optStringOrNull("deletedAt")?.toLocalDateTimeUtc(),
            createdAt = item.getString("createdAt").toLocalDateTimeUtc(),
        )
    }

    private fun JSONArray?.toMemberships(): List<RemoteSyncMembership> = this.toObjects { item ->
        RemoteSyncMembership(
            remoteId = item.getString("id"),
            petRemoteId = item.getString("petId"),
            userRemoteId = item.getString("userId"),
            role = item.getString("role"),
            status = item.getString("status"),
            serverVersion = item.optLong("version", 1),
            serverUpdatedAt = item.getString("updatedAt").toLocalDateTimeUtc(),
            deletedAt = item.optStringOrNull("deletedAt")?.toLocalDateTimeUtc(),
            createdAt = item.getString("createdAt").toLocalDateTimeUtc(),
        )
    }

    private fun JSONArray?.toEventTypes(): List<RemoteSyncEventType> = this.toObjects { item ->
        RemoteSyncEventType(
            remoteId = item.getString("id"),
            petRemoteId = item.getString("petId"),
            name = item.getString("name"),
            category = item.getString("category"),
            defaultDurationDays = item.optNullableInt("defaultDurationDays"),
            isActive = item.getBoolean("isActive"),
            colorArgb = item.optNullableLong("colorArgb"),
            iconKey = item.optStringOrNull("iconKey"),
            serverVersion = item.getLong("version"),
            serverUpdatedAt = item.getString("updatedAt").toLocalDateTimeUtc(),
            deletedAt = item.optStringOrNull("deletedAt")?.toLocalDateTimeUtc(),
            createdAt = item.getString("createdAt").toLocalDateTimeUtc(),
        )
    }

    private fun JSONArray?.toPetEvents(): List<RemoteSyncPetEvent> = this.toObjects { item ->
        RemoteSyncPetEvent(
            remoteId = item.getString("id"),
            petRemoteId = item.getString("petId"),
            eventTypeRemoteId = item.getString("eventTypeId"),
            eventDate = LocalDate.parse(item.getString("eventDate")),
            dueDate = item.optStringOrNull("dueDate")?.let(LocalDate::parse),
            comment = item.optStringOrNull("comment"),
            notificationsEnabled = item.getBoolean("notificationsEnabled"),
            status = item.getString("status"),
            serverVersion = item.getLong("version"),
            serverUpdatedAt = item.getString("updatedAt").toLocalDateTimeUtc(),
            deletedAt = item.optStringOrNull("deletedAt")?.toLocalDateTimeUtc(),
            createdAt = item.getString("createdAt").toLocalDateTimeUtc(),
        )
    }

    private fun JSONArray?.toWeightEntries(): List<RemoteSyncWeightEntry> = this.toObjects { item ->
        RemoteSyncWeightEntry(
            remoteId = item.getString("id"),
            petRemoteId = item.getString("petId"),
            date = LocalDate.parse(item.getString("date")),
            weightGrams = item.getInt("weightGrams"),
            comment = item.optStringOrNull("comment"),
            serverVersion = item.getLong("version"),
            serverUpdatedAt = item.getString("updatedAt").toLocalDateTimeUtc(),
            deletedAt = item.optStringOrNull("deletedAt")?.toLocalDateTimeUtc(),
            createdAt = item.getString("createdAt").toLocalDateTimeUtc(),
        )
    }

    private fun JSONArray?.toTombstones(): List<RemoteTombstone> = this.toObjects { item ->
        RemoteTombstone(
            entityType = item.getString("entityType"),
            remoteId = item.getString("id"),
            deletedAt = item.getString("deletedAt").toLocalDateTimeUtc(),
            version = item.getLong("version"),
        )
    }

    private fun JSONArray?.toAcceptedMutations(): List<RemoteAcceptedMutation> = this.toObjects { item ->
        RemoteAcceptedMutation(
            clientMutationId = item.optStringOrNull("clientMutationId"),
            entityType = item.getString("entityType"),
            entityId = item.getString("entityId"),
            version = item.getLong("version"),
        )
    }

    private fun JSONArray?.toConflicts(): List<RemoteConflict> = this.toObjects { item ->
        RemoteConflict(
            entityType = item.getString("entityType"),
            entityId = item.getString("entityId"),
            clientMutationId = item.optStringOrNull("clientMutationId"),
            baseVersion = item.optNullableLong("baseVersion"),
            serverVersion = item.getLong("serverVersion"),
            reason = item.getString("reason"),
            serverRecordJson = if (item.has("serverRecord") && !item.isNull("serverRecord")) item.get("serverRecord").toString() else null,
        )
    }

    private fun <T> JSONArray?.toObjects(mapper: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) add(mapper(getJSONObject(index)))
        }
    }

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (isNull(name) || !has(name)) null else getString(name).takeIf { it != "null" }
    private fun JSONObject.optNullableInt(name: String): Int? = if (isNull(name)) null else getInt(name)
    private fun JSONObject.optNullableLong(name: String): Long? = if (isNull(name)) null else getLong(name)
    private fun String.toLocalDateTimeUtc(): LocalDateTime = OffsetDateTime.parse(this).toLocalDateTime()

    private fun RemotePushRequest.toJsonBody(): String = JSONObject().apply {
        put("deviceId", deviceId)
        if (lastKnownCursor != null) put("lastKnownCursor", lastKnownCursor)
        put(
            "mutations",
            JSONArray().apply {
                mutations.forEach { mutation ->
                    put(
                        JSONObject().apply {
                            put("clientMutationId", mutation.clientMutationId)
                            put("entityId", mutation.entityId)
                            if (mutation.baseVersion != null) put("baseVersion", mutation.baseVersion)
                            put("entityType", mutation.entityType)
                            put("operation", mutation.operation)
                            if (mutation.payloadJson != null) put("payload", JSONObject(mutation.payloadJson))
                        },
                    )
                }
            },
        )
    }.toString()
}
