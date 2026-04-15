package ru.ekrupin.ivi.data.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import ru.ekrupin.ivi.data.local.entity.SyncOutboxEntity
import ru.ekrupin.ivi.data.sync.model.SyncEntityType
import ru.ekrupin.ivi.data.sync.model.SyncOperation
import ru.ekrupin.ivi.data.sync.model.SyncOutboxStatus
import ru.ekrupin.ivi.data.sync.remote.RemoteAcceptedMutation
import ru.ekrupin.ivi.data.sync.remote.RemoteBootstrapResponse
import ru.ekrupin.ivi.data.sync.remote.RemoteBootstrapSnapshot
import ru.ekrupin.ivi.data.sync.remote.RemoteChangesPayload
import ru.ekrupin.ivi.data.sync.remote.RemoteChangesResponse
import ru.ekrupin.ivi.data.sync.remote.RemoteConflict
import ru.ekrupin.ivi.data.sync.remote.RemotePushRequest
import ru.ekrupin.ivi.data.sync.remote.RemotePushResponse
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
            syncOutboxStore = FakeSyncOutboxStore(),
            syncPushApplier = FakeSyncPushApplier(),
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
            syncOutboxStore = FakeSyncOutboxStore(),
            syncPushApplier = FakeSyncPushApplier(),
        )

        coordinator.pullChanges(baseUrl = "http://localhost:8080", accessToken = "token")

        assertNotNull(snapshotStore.lastChanges)
        assertEquals("bootstrap:1000", remote.lastCursor)
        assertEquals("changes:2000", stateStore.state.cursor)
    }

    @Test
    fun drainOutbox_sendsPendingMutations_andAppliesAccepted() = runBlocking {
        val outboxItem = SyncOutboxEntity(
            id = 10,
            entityType = SyncEntityType.EVENT_TYPE,
            entityLocalId = 1,
            entityRemoteId = "remote-1",
            operation = SyncOperation.UPSERT,
            payloadJson = "{\"name\":\"Type\"}",
            baseVersion = 2,
            clientMutationId = "m1",
            status = SyncOutboxStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val remote = FakeSyncRemoteDataSource().apply {
            pushResponse = RemotePushResponse(
                accepted = listOf(RemoteAcceptedMutation("m1", "EVENT_TYPE", "remote-1", 3)),
                conflicts = emptyList(),
                cursor = "changes:3000",
                requiresBootstrap = false,
            )
        }
        val outboxStore = FakeSyncOutboxStore(mutableListOf(outboxItem))
        val stateStore = FakeSyncStateStore(cursor = "changes:2000")
        val pushApplier = FakeSyncPushApplier()
        val coordinator = SyncCoordinator(remote, FakeSyncSnapshotStore(), stateStore, outboxStore, pushApplier)

        val result = coordinator.drainOutbox("http://localhost:8080", "token", deviceId = "device-a")

        assertTrue(result is PushDrainResult.Applied)
        assertEquals("device-a", remote.lastPushRequest?.deviceId)
        assertEquals(1, pushApplier.acceptedCalls)
        assertEquals(0, pushApplier.conflictCalls)
        assertEquals("changes:3000", stateStore.state.cursor)
        assertEquals(false, stateStore.state.requiresBootstrap)
        assertEquals(listOf(10L), outboxStore.markedInFlight)
    }

    @Test
    fun drainOutbox_marksConflict_andKeepsBootstrapFlagFalse() = runBlocking {
        val outboxItem = SyncOutboxEntity(
            id = 11,
            entityType = SyncEntityType.EVENT_TYPE,
            entityLocalId = 1,
            entityRemoteId = "remote-1",
            operation = SyncOperation.UPSERT,
            payloadJson = "{\"name\":\"Type\"}",
            baseVersion = 2,
            clientMutationId = "m2",
            status = SyncOutboxStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val remote = FakeSyncRemoteDataSource().apply {
            pushResponse = RemotePushResponse(
                accepted = emptyList(),
                conflicts = listOf(RemoteConflict("EVENT_TYPE", "remote-1", "m2", 2, 3, "VERSION_MISMATCH", null)),
                cursor = "changes:4000",
                requiresBootstrap = false,
            )
        }
        val stateStore = FakeSyncStateStore(cursor = "changes:2000")
        val pushApplier = FakeSyncPushApplier()
        val coordinator = SyncCoordinator(remote, FakeSyncSnapshotStore(), stateStore, FakeSyncOutboxStore(mutableListOf(outboxItem)), pushApplier)

        coordinator.drainOutbox("http://localhost:8080", "token", deviceId = "device-a")

        assertEquals(0, pushApplier.acceptedCalls)
        assertEquals(1, pushApplier.conflictCalls)
        assertEquals(false, stateStore.state.requiresBootstrap)
    }

    @Test
    fun drainOutbox_handlesRequiresBootstrap() = runBlocking {
        val outboxItem = SyncOutboxEntity(
            id = 12,
            entityType = SyncEntityType.WEIGHT_ENTRY,
            entityLocalId = 2,
            entityRemoteId = "remote-2",
            operation = SyncOperation.UPSERT,
            payloadJson = "{\"weightGrams\":9700}",
            baseVersion = null,
            clientMutationId = "m3",
            status = SyncOutboxStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val remote = FakeSyncRemoteDataSource().apply {
            pushResponse = RemotePushResponse(
                accepted = emptyList(),
                conflicts = emptyList(),
                cursor = "changes:5000",
                requiresBootstrap = true,
            )
        }
        val stateStore = FakeSyncStateStore(cursor = "changes:2000")
        val pushApplier = FakeSyncPushApplier()
        val coordinator = SyncCoordinator(remote, FakeSyncSnapshotStore(), stateStore, FakeSyncOutboxStore(mutableListOf(outboxItem)), pushApplier)

        val result = coordinator.drainOutbox("http://localhost:8080", "token", deviceId = "device-a")

        assertEquals(PushDrainResult.RequiresBootstrap, result)
        assertEquals(true, stateStore.state.requiresBootstrap)
        assertEquals(1, pushApplier.failedCalls)
    }

    @Test
    fun bootstrapImport_failsWhenOutboxIsNotEmpty() = runBlocking {
        val outboxItem = SyncOutboxEntity(
            id = 1,
            entityType = SyncEntityType.EVENT_TYPE,
            entityLocalId = 1,
            entityRemoteId = "remote",
            operation = SyncOperation.UPSERT,
            payloadJson = null,
            baseVersion = null,
            clientMutationId = "bootstrap-block",
            status = SyncOutboxStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val coordinator = SyncCoordinator(
            syncRemoteDataSource = FakeSyncRemoteDataSource(),
            syncSnapshotStore = FakeSyncSnapshotStore(),
            syncStateStore = FakeSyncStateStore(),
            syncOutboxStore = FakeSyncOutboxStore(mutableListOf(outboxItem)),
            syncPushApplier = FakeSyncPushApplier(),
        )

        try {
            coordinator.bootstrapImport(baseUrl = "http://localhost:8080", accessToken = "token")
            fail("Expected bootstrap to fail when outbox is not empty")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Bootstrap import в V1 разрешен только при пустом outbox", expected.message)
        }
    }
}

internal class FakeSyncRemoteDataSource : SyncRemoteDataSource {
    var lastBaseUrl: String? = null
    var lastAccessToken: String? = null
    var lastCursor: String? = null
    var lastPushRequest: RemotePushRequest? = null
    var pushResponse: RemotePushResponse = RemotePushResponse(emptyList(), emptyList(), "changes:0", false)

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

    override suspend fun push(baseUrl: String, accessToken: String, request: RemotePushRequest): RemotePushResponse {
        lastBaseUrl = baseUrl
        lastAccessToken = accessToken
        lastPushRequest = request
        return pushResponse
    }
}

internal class FakeSyncSnapshotStore : SyncSnapshotStore {
    var lastBootstrap: RemoteBootstrapResponse? = null
    var lastChanges: RemoteChangesResponse? = null

    override suspend fun importBootstrapSnapshot(snapshot: RemoteBootstrapResponse) {
        lastBootstrap = snapshot
    }

    override suspend fun applyIncrementalChanges(changes: RemoteChangesResponse) {
        lastChanges = changes
    }
}

internal class FakeSyncStateStore(cursor: String? = null) : SyncStateStore {
    var state = SyncReadState(
        cursor = cursor,
        lastBootstrapAt = null,
        lastChangesAt = null,
        lastSuccessfulReadAt = null,
        requiresBootstrap = false,
    )

    override suspend fun get(): SyncReadState = state

    override suspend fun saveBootstrapCursor(cursor: String, timestamp: LocalDateTime) {
        state = state.copy(cursor = cursor, lastBootstrapAt = timestamp, lastSuccessfulReadAt = timestamp, requiresBootstrap = false)
    }

    override suspend fun saveChangesCursor(cursor: String, timestamp: LocalDateTime) {
        state = state.copy(cursor = cursor, lastChangesAt = timestamp, lastSuccessfulReadAt = timestamp, requiresBootstrap = false)
    }

    override suspend fun setRequiresBootstrap(value: Boolean) {
        state = state.copy(requiresBootstrap = value)
    }
}

internal class FakeSyncOutboxStore(
    private val items: MutableList<SyncOutboxEntity> = mutableListOf(),
) : SyncOutboxStore {
    val markedInFlight = mutableListOf<Long>()
    val markedPending = mutableListOf<Long>()
    val markedFailed = mutableListOf<Long>()
    val deleted = mutableListOf<Long>()

    override suspend fun pending(limit: Int): List<SyncOutboxEntity> = items.filter { it.status == SyncOutboxStatus.PENDING }.take(limit)
    override suspend fun markInFlight(ids: List<Long>) { markedInFlight += ids }
    override suspend fun markPending(ids: List<Long>) { markedPending += ids }
    override suspend fun markFailed(ids: List<Long>) { markedFailed += ids }
    override suspend fun delete(ids: List<Long>) { deleted += ids }
}

internal class FakeSyncPushApplier : SyncPushApplier {
    var acceptedCalls = 0
    var conflictCalls = 0
    var failedCalls = 0

    override suspend fun applyAccepted(outboxItems: List<SyncOutboxEntity>, accepted: List<RemoteAcceptedMutation>, syncedAt: LocalDateTime) {
        if (accepted.isNotEmpty()) acceptedCalls += 1
    }

    override suspend fun applyConflicts(outboxItems: List<SyncOutboxEntity>, conflicts: List<RemoteConflict>, conflictedAt: LocalDateTime) {
        if (conflicts.isNotEmpty()) conflictCalls += 1
    }

    override suspend fun markFailed(outboxItems: List<SyncOutboxEntity>) {
        failedCalls += 1
    }
}
