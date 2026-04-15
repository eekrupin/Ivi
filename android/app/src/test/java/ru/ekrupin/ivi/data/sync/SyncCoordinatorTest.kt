package ru.ekrupin.ivi.data.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import ru.ekrupin.ivi.data.local.dao.SyncOutboxDao
import ru.ekrupin.ivi.data.local.entity.SyncOutboxEntity
import ru.ekrupin.ivi.data.sync.model.SyncOutboxStatus
import ru.ekrupin.ivi.data.sync.remote.RemoteBootstrapResponse
import ru.ekrupin.ivi.data.sync.remote.RemoteBootstrapSnapshot
import ru.ekrupin.ivi.data.sync.remote.RemoteChangesPayload
import ru.ekrupin.ivi.data.sync.remote.RemoteChangesResponse
import ru.ekrupin.ivi.data.sync.remote.SyncRemoteDataSource
import java.time.LocalDateTime

class SyncCoordinatorTest {
    @Test
    fun bootstrapImport_fetchesSnapshot_appliesIt_andStoresCursor() = runBlocking {
        val remote = FakeSyncRemoteDataSource()
        val snapshotStore = FakeSyncSnapshotStore()
        val stateStore = FakeSyncStateStore()
        val coordinator = SyncCoordinator(
            syncRemoteDataSource = remote,
            syncSnapshotStore = snapshotStore,
            syncStateStore = stateStore,
            syncOutboxDao = FakeSyncOutboxDao(countAll = 0),
        )

        coordinator.bootstrapImport(baseUrl = "http://localhost:8080", accessToken = "token")

        assertNotNull(snapshotStore.lastBootstrap)
        assertEquals("bootstrap:1000", stateStore.state.cursor)
        assertEquals("http://localhost:8080", remote.lastBaseUrl)
        assertEquals("token", remote.lastAccessToken)
    }

    @Test
    fun pullChanges_usesStoredCursor_appliesChanges_andUpdatesCursor() = runBlocking {
        val remote = FakeSyncRemoteDataSource()
        val snapshotStore = FakeSyncSnapshotStore()
        val stateStore = FakeSyncStateStore(cursor = "bootstrap:1000")
        val coordinator = SyncCoordinator(
            syncRemoteDataSource = remote,
            syncSnapshotStore = snapshotStore,
            syncStateStore = stateStore,
            syncOutboxDao = FakeSyncOutboxDao(countAll = 0),
        )

        coordinator.pullChanges(baseUrl = "http://localhost:8080", accessToken = "token")

        assertNotNull(snapshotStore.lastChanges)
        assertEquals("bootstrap:1000", remote.lastCursor)
        assertEquals("changes:2000", stateStore.state.cursor)
    }

    @Test
    fun bootstrapImport_failsWhenOutboxIsNotEmpty() = runBlocking {
        val coordinator = SyncCoordinator(
            syncRemoteDataSource = FakeSyncRemoteDataSource(),
            syncSnapshotStore = FakeSyncSnapshotStore(),
            syncStateStore = FakeSyncStateStore(),
            syncOutboxDao = FakeSyncOutboxDao(countAll = 1),
        )

        try {
            coordinator.bootstrapImport(baseUrl = "http://localhost:8080", accessToken = "token")
            fail("Expected bootstrap to fail when outbox is not empty")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Bootstrap import в V1 разрешен только при пустом outbox", expected.message)
        }
    }
}

private class FakeSyncRemoteDataSource : SyncRemoteDataSource {
    var lastBaseUrl: String? = null
    var lastAccessToken: String? = null
    var lastCursor: String? = null

    override suspend fun bootstrap(baseUrl: String, accessToken: String): RemoteBootstrapResponse {
        lastBaseUrl = baseUrl
        lastAccessToken = accessToken
        return RemoteBootstrapResponse(
            cursor = "bootstrap:1000",
            snapshot = RemoteBootstrapSnapshot(
                users = emptyList(),
                pets = emptyList(),
                memberships = emptyList(),
                eventTypes = emptyList(),
                petEvents = emptyList(),
                weightEntries = emptyList(),
            ),
        )
    }

    override suspend fun changes(baseUrl: String, accessToken: String, cursor: String): RemoteChangesResponse {
        lastBaseUrl = baseUrl
        lastAccessToken = accessToken
        lastCursor = cursor
        return RemoteChangesResponse(
            cursor = "changes:2000",
            hasMore = false,
            changes = RemoteChangesPayload(
                users = emptyList(),
                pets = emptyList(),
                memberships = emptyList(),
                eventTypes = emptyList(),
                petEvents = emptyList(),
                weightEntries = emptyList(),
            ),
            tombstones = emptyList(),
        )
    }
}

private class FakeSyncSnapshotStore : SyncSnapshotStore {
    var lastBootstrap: RemoteBootstrapResponse? = null
    var lastChanges: RemoteChangesResponse? = null

    override suspend fun importBootstrapSnapshot(snapshot: RemoteBootstrapResponse) {
        lastBootstrap = snapshot
    }

    override suspend fun applyIncrementalChanges(changes: RemoteChangesResponse) {
        lastChanges = changes
    }
}

private class FakeSyncStateStore(cursor: String? = null) : SyncStateStore {
    var state = SyncReadState(
        cursor = cursor,
        lastBootstrapAt = null,
        lastChangesAt = null,
        lastSuccessfulReadAt = null,
    )

    override suspend fun get(): SyncReadState = state

    override suspend fun saveBootstrapCursor(cursor: String, timestamp: LocalDateTime) {
        state = state.copy(cursor = cursor, lastBootstrapAt = timestamp, lastSuccessfulReadAt = timestamp)
    }

    override suspend fun saveChangesCursor(cursor: String, timestamp: LocalDateTime) {
        state = state.copy(cursor = cursor, lastChangesAt = timestamp, lastSuccessfulReadAt = timestamp)
    }
}

private class FakeSyncOutboxDao(
    private val countAll: Int,
) : SyncOutboxDao {
    override suspend fun insert(item: SyncOutboxEntity): Long = 0
    override suspend fun getByStatus(status: SyncOutboxStatus, limit: Int): List<SyncOutboxEntity> = emptyList()
    override suspend fun countByStatus(status: SyncOutboxStatus): Int = 0
    override suspend fun countAll(): Int = countAll
    override suspend fun updateStatus(ids: List<Long>, status: SyncOutboxStatus, updatedAt: java.time.LocalDateTime) = Unit
    override suspend fun deleteByIds(ids: List<Long>) = Unit
}
