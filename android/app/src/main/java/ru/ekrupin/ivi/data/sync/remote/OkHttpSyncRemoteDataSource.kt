package ru.ekrupin.ivi.data.sync.remote

import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
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
}
